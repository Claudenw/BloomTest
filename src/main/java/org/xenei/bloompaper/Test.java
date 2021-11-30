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
import java.util.Collection;
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
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.geoname.GeoName;
import org.xenei.bloompaper.geoname.GeoNameHasher;
import org.xenei.bloompaper.geoname.GeoNameIterator;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.BloomIndexBFTrie;
import org.xenei.bloompaper.index.BloomIndexBloofi;
import org.xenei.bloompaper.index.BloomIndexFlatBloofi;
import org.xenei.bloompaper.index.BloomIndexHamming;
import org.xenei.bloompaper.index.BloomIndexLinear;
import org.xenei.bloompaper.index.NullCollection;

public class Test {

    private static Map<String, Constructor<? extends BloomIndex>> constructors = new HashMap<String, Constructor<? extends BloomIndex>>();

    // 9,772,346 max lines
    private static int RUN_COUNT = 5;

    static int[] POPULATIONS = { 100, 1000, 10000, 100000, 1000000 };


    public static Object lastCreated;

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
        options.addOption("s", "size", true, "The population size.  May occure more than once.  defualt=100, 1000, 10000, 100000, and 1000000");
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

        if (cmd.hasOption("s")) {
            String[] values = cmd.getOptionValues("s");
            POPULATIONS = new int[values.length];
            for (int i=0;i<values.length;i++)
            {
                try {
                        POPULATIONS[i] = Integer.parseInt( values[i] );
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format("Populsation size (s) %s is not a valid number.", values[i]));
                }
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
        shape = Shape.Factory.fromNP( n, p);

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
        try (GeoNameIterator geoIter = new GeoNameIterator(inputFile)) {
            for (int i = 0; i < 1000000; i++) {
                final GeoName gn = geoIter.next();
                if ((i % 1000) == 0) {
                    sample.add(gn);
                }
                filters[i] = new SimpleBloomFilter(shape, GeoNameHasher.createHasher(geoIter.next()));
            }
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
        Summary.writeData( System.out, table);
        if (dir != null) {
            o = new PrintStream(new File(dir, "data.csv"));
            Summary.writeData( o, table);
            o.close();
        }

        final Summary summary = new Summary(table);

        System.out.println("=== summary data ===");
        summary.writeSummary( System.out );
        if (dir != null) {
            o = new PrintStream(new File(dir, "summary.csv"));
            summary.writeSummary( o );
            o.close();
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
                bfSample[i] = new SimpleBloomFilter(shape, GeoNameHasher.createHasher(sample.get(i)));
                break;
            case HIGHCARD:
                bfSample[i] = new SimpleBloomFilter(shape, GeoNameHasher.hasherFor(sample.get(i).name));
                break;
            case LOWCARD:
                bfSample[i] = new SimpleBloomFilter(shape, GeoNameHasher.hasherFor(sample.get(i).feature_code));
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
            for (int i = 0; i < sampleSize; i++)
            {
                start = System.currentTimeMillis();
                Stats stat = stats.get(run);
                stat.currentPhase = Stats.Phase.Query;
                stat.currentType = type;
                Collection<BloomFilter> filterCapture = NullCollection.INSTANCE;
                //Collection<BloomFilter> filterCapture = new ArrayList<BloomFilter>();
                bi.setFilterCapture( filterCapture );
                found += bi.count(bfSample[i]);
                elapsed += (System.currentTimeMillis() - start);
                stat.addFoundFilters(type, bfSample[i], filterCapture );
            }
            stats.get(run).registerResult(Stats.Phase.Query, type, elapsed, found);
            System.out.println(stats.get(run).displayString(Stats.Phase.Query, type));
        }

    }
}
