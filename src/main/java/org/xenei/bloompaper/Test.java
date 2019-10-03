package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.apache.commons.collections4.bloomfilter.StandardBloomFilter;
import org.xenei.bloompaper.geoname.GeoName;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.BloomIndexBTree;
import org.xenei.bloompaper.index.BloomIndexBTreeNoStack;
import org.xenei.bloompaper.index.BloomIndexBloofi;
import org.xenei.bloompaper.index.BloomIndexBloofiR;
import org.xenei.bloompaper.index.BloomIndexHamming;
import org.xenei.bloompaper.index.BloomIndexLimitedBTree;
import org.xenei.bloompaper.index.BloomIndexLimitedHamming;
import org.xenei.bloompaper.index.BloomIndexLinear;
import org.xenei.bloompaper.index.BloomIndexLinearList;
import org.xenei.bloompaper.index.BloomIndexPartialBTree;

public class Test {

    private static Map<String, Constructor<? extends BloomIndex>> constructors = new HashMap<String, Constructor<? extends BloomIndex>>();

    // 9,772,346 max lines
    private static int RUN_COUNT = 5;

    private static int[] RUNSIZE = { 100, 1000, 10000, 100000, 1000000 };

    private static void init() throws NoSuchMethodException, SecurityException {
        constructors.put("Hamming", BloomIndexHamming.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("LimitedHamming",
                BloomIndexLimitedHamming.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("Bloofi", BloomIndexBloofi.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("BloofiR", BloomIndexBloofiR.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("Btree", BloomIndexBTree.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("BtreeNoStack",
                BloomIndexBTreeNoStack.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("PartialBtree",
                BloomIndexPartialBTree.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("LimitedBtree",
                BloomIndexLimitedBTree.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("Linear", BloomIndexLinear.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put("LinearList",
                BloomIndexLinearList.class.getConstructor(int.class, BloomFilterConfiguration.class));
    }

    public static Options getOptions() {
        StringBuffer sb = new StringBuffer("List of tests to run.  Valid test names are: ALL, ");
        List<String> tests = new ArrayList<String>( constructors.keySet() );
        Collections.sort(tests);
        sb.append(String.join(", ", tests));

        Options options = new Options();
        options.addRequiredOption("r", "run", true, sb.toString());
        options.addOption("d", "dense", false, "Use compact filters");
        options.addOption("h", "help", false, "This help");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        options.addOption("i", "iterations", true, "The number of iterations defualt=" + RUN_COUNT);
        return options;
    }

    public static void main(final String[] args) throws Exception {
        init();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            StringBuffer sb = new StringBuffer("Valid test names are: ALL, ");
            sb.append(String.join(", ", constructors.keySet()));
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", "", getOptions(), sb.toString());
        }

        if (cmd.hasOption("i")) {
            try {
                RUN_COUNT = Integer.parseInt(cmd.getOptionValue("i"));
            } catch (NumberFormatException e) {
                System.err.println(cmd.getOptionValue("i") + " is not a valid number, using " + RUN_COUNT);
            }
            if (RUN_COUNT < 1) {
                RUN_COUNT = 5;
                System.err.println(cmd.getOptionValue("i") + " is not a valid number, using " + RUN_COUNT);
            }
        }

        BloomFilterConfiguration bloomFilterConfig;
        if (cmd.hasOption("d")) {
            bloomFilterConfig = new BloomFilterConfiguration(1, 11, 8);
        } else {
            // 3 items 1/100,000
            bloomFilterConfig = new BloomFilterConfiguration(3, 1.0 / 100000);
        }

        File dir = null;
        if (cmd.hasOption("o")) {
            dir = new File(cmd.getOptionValue("o"));
            if (!dir.exists()) {
                dir.mkdirs();
            } else if (!dir.isDirectory()) {
                throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
            }
        }

        final List<String> tests = new ArrayList<String>();
        final List<Stats> table = new ArrayList<Stats>();
        final BloomFilter[] filters = new BloomFilter[1000000]; // (1e6)
        final URL inputFile = Test.class.getResource("/allCountries.txt");
        final List<GeoName> sample = new ArrayList<GeoName>(1000); // (1e3)

        boolean hasError = false;
        for (String s : cmd.getOptionValues("r")) {
            if ("ALL".equals(s)) {
                tests.clear();
                tests.addAll(constructors.keySet());
                break;
            }
            if (constructors.containsKey(s)) {
                tests.add(s);
            } else {
                System.err.println(s + " is not a valid test name");
                hasError = true;
            }
        }
        if (hasError) {
            System.exit(1);
            ;
        }
        Collections.sort(tests);

        System.out.println("Reading test data");
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
        for (int i = 0; i < 1000000; i++) {
            final GeoName gn = GeoName.parse(br.readLine());
            if ((i % 1000) == 0) {
                sample.add(gn);
            }
            filters[i] = new StandardBloomFilter(GeoNameFilterFactory.create(gn), bloomFilterConfig);
        }

        // run the tests
        for (final String testName : tests) {
            System.out.println("Running " + testName);
            Constructor<? extends BloomIndex> constructor = constructors.get(testName);
            for (final int population : RUNSIZE) {
                final Stats[] stats = new Stats[RUN_COUNT];
                for (int run = 0; run < RUN_COUNT; run++) {
                    stats[run] = new Stats(population);
                }
                table.addAll(runTest(bloomFilterConfig, constructor, sample, filters, stats));
            }
        }

        PrintStream o = null;

        System.out.println("===  data ===");
        if (dir != null) {
            o = new PrintStream(new File(dir, "data.csv"));
        }
        System.out.println(Stats.getHeader());
        if (o != null) {
            o.println(Stats.getHeader());
        }
        for (final Stats s : table) {
            System.out.println(s.toString());
            if (o != null) {
                o.println(s.toString());
            }
        }

        System.out.println("=== summary data ===");
        if (dir != null) {
            o = new PrintStream(new File(dir, "summary.csv"));
        } else {
            o = null;
        }
        final Summary summary = new Summary(table);

        System.out.println(Summary.getHeader());
        if (o != null) {
            o.println(Summary.getHeader());
        }
        for (final Summary.Element e : summary.getTable()) {
            System.out.println(e.toString());
            if (o != null) {
                o.println(e.toString());
            }
        }

        System.out.println("=== run complete ===");
    }

    private static List<Stats> runTest(final BloomFilterConfiguration bloomFilterConfig,
            final Constructor<? extends BloomIndex> constructor, final List<GeoName> sample,
            final BloomFilter[] filters, final Stats[] stats) throws IOException, InstantiationException,
    IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final BloomIndex bi = doLoad(constructor, filters, bloomFilterConfig, stats);
        final int sampleSize = sample.size();
        BloomFilter[] bfSample = new BloomFilter[sampleSize];
        for (int i = 0; i < sample.size(); i++) {
            bfSample[i] = new StandardBloomFilter(GeoNameFilterFactory.create(sample.get(i)), bloomFilterConfig);
        }
        doCount(1, bi, bfSample, stats);

        bfSample = new BloomFilter[sampleSize];
        for (int i = 0; i < sample.size(); i++) {

            bfSample[i] = new StandardBloomFilter(GeoNameFilterFactory.create(sample.get(i).name), bloomFilterConfig);
        }
        doCount(2, bi, bfSample, stats);

        bfSample = new BloomFilter[sampleSize];
        for (int i = 0; i < sample.size(); i++) {
            bfSample[i] = new StandardBloomFilter(GeoNameFilterFactory.create(sample.get(i).feature_code),
                    bloomFilterConfig);
        }
        doCount(3, bi, bfSample, stats);
        return Arrays.asList(stats);
    }

    private static BloomIndex doLoad(final Constructor<? extends BloomIndex> constructor, final BloomFilter[] filters,
            final BloomFilterConfiguration bloomFilterConfig, final Stats[] stats)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi = null;

        for (int run = 0; run < RUN_COUNT; run++) {
            bi = constructor.newInstance(stats[run].population, bloomFilterConfig);
            stats[run].type = bi.getName();
            final long start = System.currentTimeMillis();
            for (int i = 0; i < stats[run].population; i++) {
                bi.add(filters[i]);
            }
            final long end = System.currentTimeMillis();
            stats[run].load = end - start;
            System.out.println(String.format("%s 0 population %s run %s  load time %s", bi.getName(),
                    stats[run].population, run, end - start));
        }
        return bi;
    }

    private static void doSearch(final int pos, final BloomIndex bi, final BloomFilter[] bfSample,
            final Stats[] stats) {
        final int sampleSize = bfSample.length;
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            long found = 0;
            List<BloomFilter> result;
            for (int i = 0; i < sampleSize; i++) {
                start = System.currentTimeMillis();
                result = bi.get(bfSample[i]);
                elapsed += (System.currentTimeMillis() - start);
                found += result.size();
            }
            registerResult(stats[run], pos, elapsed, found);

            System.out.println(String.format("%s %s population %s run %s  search time %s (%s)", bi.getName(), pos,
                    stats[run].population, run, elapsed, found));
        }

    }

    private static void doCount(final int pos, final BloomIndex bi, final BloomFilter[] bfSample, final Stats[] stats) {
        final int sampleSize = bfSample.length;
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            long found = 0;

            for (int i = 0; i < sampleSize; i++) {
                start = System.currentTimeMillis();
                found += bi.count(bfSample[i]);
                elapsed += (System.currentTimeMillis() - start);
            }
            registerResult(stats[run], pos, elapsed, found);

            System.out.println(String.format("%s %s population %s run %s  count time %s (%s)", bi.getName(), pos,
                    stats[run].population, run, elapsed, found));
        }

    }

    private static void registerResult(final Stats stat, final int pos, final long total, final long found) {
        switch (pos) {
        case 1:
            stat.complete = total;
            stat.completeFound = found;
            break;
        case 2:
            stat.name = total;
            stat.nameFound = found;
            break;
        case 3:
            stat.feature = total;
            stat.featureFound = found;
            break;
        default:
            throw new IllegalArgumentException(String.format("%s is not a valid position", pos));
        }

    }
}
