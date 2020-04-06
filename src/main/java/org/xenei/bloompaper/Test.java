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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.bloomfilter.BitSetBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.function.Murmur128x86Cyclic;
import org.xenei.bloompaper.geoname.GeoName;
import org.xenei.bloompaper.index.BloomFilterIndexer;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.BloomIndexBFTrie;
import org.xenei.bloompaper.index.BloomIndexBloofi;
import org.xenei.bloompaper.index.BloomIndexFlatBloofi;
import org.xenei.bloompaper.index.BloomIndexHamming;
import org.xenei.bloompaper.index.BloomIndexLinear;
import org.xenei.bloompaper.index.bftrie.BFTrie4;

public class Test {

    private static Map<String, Constructor<? extends BloomIndex>> constructors = new HashMap<String, Constructor<? extends BloomIndex>>();

    // 9,772,346 max lines
    private static int RUN_COUNT = 5;

    static int[] POPULATIONS = { 100, 1000, 10000, 100000, 1000000 };

    private static void init() throws NoSuchMethodException, SecurityException {
        constructors.put("Hamming", BloomIndexHamming.class.getConstructor(int.class, Shape.class));
        constructors.put("Bloofi", BloomIndexBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("FlatBloofi", BloomIndexFlatBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("BF-Trie", BloomIndexBFTrie.class.getConstructor(int.class, Shape.class));
        constructors.put("Linear", BloomIndexLinear.class.getConstructor(int.class, Shape.class));
    }

    public static Options getOptions() {
        StringBuffer sb = new StringBuffer("List of tests to run.  Valid test names are: ALL, ");
        List<String> tests = new ArrayList<String>(constructors.keySet());
        Collections.sort(tests);
        sb.append(String.join(", ", tests));

        Options options = new Options();
        options.addRequiredOption("r", "run", true, sb.toString());
        options.addOption("h", "help", false, "This help");
        options.addOption("n", "number", true, "The number of items in the filter (defaults to 3)");
        options.addOption("p", "probability", true,
                "The probability of collisions (defaults to 1/100000).  May be specified as x/y or double format");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        options.addOption("i", "iterations", true, "The number of iterations defualt=" + RUN_COUNT);
        return options;
    }

    public static void main(final String[] args) throws Exception {
        init();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        int n = 3;
        double p = 1.0 / 100000;
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

        Shape shape;
        if (cmd.hasOption("n")) {
            n = Integer.valueOf(cmd.getOptionValue("n"));
        }

        if (cmd.hasOption("p")) {
            String pStr = cmd.getOptionValue("p");
            String[] parts = pStr.split("/");
            if (parts.length == 1) {
                p = Double.parseDouble(parts[0]);
            } else {
                p = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
        }
        shape = new Shape(new Murmur128x86Cyclic(), n, p);

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
        }
        Collections.sort(tests);

        System.out.println("Reading test data");
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
        for (int i = 0; i < 1000000; i++) {
            final GeoName gn = GeoName.parse(br.readLine());
            if ((i % 1000) == 0) {
                sample.add(gn);
            }
            filters[i] = new BitSetBloomFilter(GeoNameFilterFactory.create(gn), shape);
        }

        // run the tests
        for (final String testName : tests) {
            System.out.println("Running " + testName);
            Constructor<? extends BloomIndex> constructor = constructors.get(testName);
            for (final int population : POPULATIONS) {
                final List<Stats> stats = new ArrayList<Stats>();
                for (int run = 0; run < RUN_COUNT; run++) {
                    stats.add(new Stats(testName, population, run));
                }
                table.addAll(runTest(shape, constructor, sample, filters, stats));
            }
        }

        PrintStream o = null;

        System.out.println( "=== verification ===" );
        if (dir != null) {
            o = new PrintStream(new File(dir, "verification.txt"));
        }

        Verifier verifier = new Verifier( o );
        verifier.verify( table );

        System.out.println("===  data ===");
        if (dir != null) {
            o = new PrintStream(new File(dir, "data.csv"));
        }
        System.out.println(Stats.getHeader());
        if (o != null) {
            o.println(Stats.getHeader());
        }
        for (final Stats s : table) {
            for (Stats.Phase phase : Stats.Phase.values())
            {
                System.out.println(s.reportStats(phase));
                if (o != null) {
                    o.println(s.reportStats(phase));
                }
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
            for (Stats.Phase phase : Stats.Phase.values()) {
                System.out.println(e.getReport( phase ));
                if (o != null) {
                    o.println(e.getReport(phase));
                }
            }
        }

        System.out.println("=== run complete ===");
    }

    private static void doDelete(Stats.Type type, final Constructor<? extends BloomIndex> constructor,
            final BloomFilter[] filters, final BloomFilter[] bfSample, final List<Stats> stats, Shape shape)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi;
        for (int run = 0; run < RUN_COUNT; run++) {
            /* setup */
            Stats stat = stats.get(run);
            bi = constructor.newInstance(stat.getPopulation(), shape);
            for (int i = 0; i < stat.getPopulation(); i++) {
                bi.add(filters[i]);
            }

            /* run */
            final long start = System.currentTimeMillis();
            for (BloomFilter bf : bfSample) {
                bi.delete(bf);
            }
            final long end = System.currentTimeMillis();

            stat.registerResult(Stats.Phase.Delete, type , end - start, stat.getPopulation() - bi.count());

            System.out.println(
                    stat.displayString(Stats.Phase.Delete, type) );
        }
    }

    private static BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample) {
        final int sampleSize = sample.size();
        BloomFilter[] bfSample = new BloomFilter[sampleSize];
        for (int i = 0; i < sample.size(); i++) {
            switch (type) {
            case COMPLETE:
                bfSample[i] = new BitSetBloomFilter(GeoNameFilterFactory.create(sample.get(i)), shape);
                break;
            case HIGHCARD:
                bfSample[i] = new BitSetBloomFilter(GeoNameFilterFactory.create(sample.get(i).name), shape);
                break;
            case LOWCARD:
                bfSample[i] = new BitSetBloomFilter(GeoNameFilterFactory.create(sample.get(i).feature_code), shape);
                break;
            }
        }
        return bfSample;
    }

    private static List<Stats> runTest(final Shape shape, final Constructor<? extends BloomIndex> constructor,
            final List<GeoName> sample, final BloomFilter[] filters, List<Stats> stats) throws IOException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        BloomIndex bi = doLoad(constructor, filters, shape, stats);

        for (Stats.Type type : Stats.Type.values()) {
            BloomFilter[] bfSample = createSample(shape, type, sample);
            doCount(type, bi, bfSample, stats);
        }

        for (Stats.Type type : Stats.Type.values()) {
            BloomFilter[] bfSample = createSample(shape, type, sample);
            doDelete(type, constructor, filters, bfSample, stats, shape);
        }

        return stats;
    }

    private static BloomIndex doLoad(final Constructor<? extends BloomIndex> constructor, final BloomFilter[] filters,
            final Shape shape, final List<Stats> stats)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi = null;

        for (int run = 0; run < RUN_COUNT; run++) {
            Stats stat = stats.get(run);
            bi = constructor.newInstance(stat.getPopulation(), shape);
            long elapsed = 0;
            long start = 0;
            for (int i = 0; i < stat.getPopulation(); i++) {
                start = System.currentTimeMillis();
                bi.add(filters[i]);
                elapsed += (System.currentTimeMillis() - start);
            }
            stat.load = elapsed;
            System.out.println( stat.loadDisplayString());
        }
        return bi;
    }

    private static void doCount(final Stats.Type type, final BloomIndex bi, final BloomFilter[] bfSample,
            final List<Stats> stats) {
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
            stats.get(run).registerResult(Stats.Phase.Query, type, elapsed, found);
            System.out.println(stats.get(run).displayString(Stats.Phase.Query, type));
        }

    }


}
