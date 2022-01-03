package org.xenei.bloompaper;

import java.io.File;
import java.io.IOException;
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
import org.apache.commons.collections4.bloomfilter.SparseBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.lang3.time.StopWatch;

import org.xenei.bloompaper.geoname.GeoName;
import org.xenei.bloompaper.geoname.GeoNameGatekeeperHasher;
import org.xenei.bloompaper.geoname.GeoNameReferenceHasher;
import org.xenei.bloompaper.geoname.GeoNameIterator;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.BloomIndexBFTrie4;
import org.xenei.bloompaper.index.BloomIndexBFTrie8;
import org.xenei.bloompaper.index.BloomIndexBloofi;
import org.xenei.bloompaper.index.BloomIndexFlatBloofi;
import org.xenei.bloompaper.index.BloomIndexHamming;
import org.xenei.bloompaper.index.BloomIndexList;
import org.xenei.bloompaper.index.BloomIndexArray;

public class Test {

    public static Map<String, Constructor<? extends BloomIndex>> constructors = new HashMap<String, Constructor<? extends BloomIndex>>();

    // 9,772,346 max lines
    private static int RUN_COUNT = 5;

    static int[] POPULATIONS = { 100, 1000, 10000, 100000, 1000000 };

    public static Object lastCreated;

    public static void init() throws NoSuchMethodException, SecurityException {
        constructors.put("Hamming", BloomIndexHamming.class.getConstructor(int.class, Shape.class));
        constructors.put("Bloofi", BloomIndexBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("FlatBloofi", BloomIndexFlatBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("BF-Trie4", BloomIndexBFTrie4.class.getConstructor(int.class, Shape.class));
        constructors.put("BF-Trie8", BloomIndexBFTrie8.class.getConstructor(int.class, Shape.class));
        constructors.put("Array", BloomIndexArray.class.getConstructor(int.class, Shape.class));
        constructors.put("List", BloomIndexList.class.getConstructor(int.class, Shape.class));
    }

    public static Options getOptions() {
        StringBuffer sb = new StringBuffer("List of tests to run.  Valid test names are: ALL, ");
        List<String> tests = new ArrayList<String>(constructors.keySet());
        Collections.sort(tests);
        sb.append(String.join(", ", tests));

        Options options = new Options();
        options.addRequiredOption("r", "run", true, sb.toString());
        options.addOption("h", "help", false, "This help");
        options.addOption("g", "gatekeeper", false,
                "Assume Gatekeeper usage pattern.  If not specified Reference pattern will be assuemd.");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        options.addOption("i", "iterations", true, "The number of iterations defualt=" + RUN_COUNT);
        options.addOption("s", "size", true,
                "The population size.  May occure more than once.  defualt=100, 1000, 10000, 100000, and 1000000.  Default = all");
        options.addOption("v", "short-verify", false,
                "Skip verification of collected bloom filters. Does not preserve bloom filters in .dat file");
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

        if (cmd.hasOption("s")) {
            String[] values = cmd.getOptionValues("s");
            POPULATIONS = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    POPULATIONS[i] = Integer.parseInt(values[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            String.format("Populsation size (s) %s is not a valid number.", values[i]));
                }
            }
        }

        UsagePattern usagePattern = cmd.hasOption("g") ? new GateKeeper() : new Reference();

        File dir = null;
        if (cmd.hasOption("o")) {
            dir = new File(cmd.getOptionValue("o"));
            if (!dir.exists()) {
                dir.mkdirs();
            } else if (!dir.isDirectory()) {
                throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
            }
        }

        boolean collectFilters = !cmd.hasOption("v");

        final List<String> tests = new ArrayList<String>();
        final Table table = new Table(dir);
        table.reset();
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

        System.out.println("Loading sample data");
        try (GeoNameIterator geoIter = new GeoNameIterator(inputFile)) {
            for (int i = 0; i < 1000000; i++) {
                final GeoName gn = geoIter.next();
                if ((i % 1000) == 0) {
                    sample.add(gn);
                }
            }
        }
        // run the tests
        for (final String testName : tests) {
            System.out.println("Running " + testName);

            Constructor<? extends BloomIndex> constructor = constructors.get(testName);
            for (final int population : POPULATIONS) {
                final List<Stats> stats = new ArrayList<Stats>();
                for (int run = 0; run < RUN_COUNT; run++) {
                    stats.add(new Stats(usagePattern.getName(), testName, population, run));
                }
                BloomFilter[] filters = usagePattern.configure(population, new GeoNameIterator(inputFile));
                table.add(testName, runTest(usagePattern.getShape(population), constructor, sample, filters, stats,
                        collectFilters, usagePattern));
            }
        }

        PrintStream o = null;

        System.out.println("=== verification ===");
        try {
            if (dir != null) {
                o = new PrintStream(new File(dir, "verification.txt"));
            }

            Verifier verifier = new Verifier(o);
            verifier.verify(table);
        } catch (IOException e) {
            System.out.println(String.format("Error during verifier: %s", e.getMessage()));
        } finally {
            if (o != null) {
                o.close();
            }
        }

        final Summary summary = new Summary(table);
        Summary.doOutput(table, null, true);

        if (dir != null) {
            try (PrintStream ps = new PrintStream(new File(dir, "data.csv"))) {
                Summary.writeData(ps, table);
            }
            try (PrintStream ps = new PrintStream(new File(dir, "summary.csv"))) {
                summary.writeSummary(ps);
            }
        }

        System.out.println("=== run complete ===");
    }

    private static void doDelete(Stats.Type type, final Constructor<? extends BloomIndex> constructor,
            final BloomFilter[] filters, final BloomFilter[] bfSample, final List<Stats> stats, Shape shape)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi;
        StopWatch stopwatch = new StopWatch();
        for (int run = 0; run < RUN_COUNT; run++) {
            /* setup */
            Stats stat = stats.get(run);
            bi = constructor.newInstance(stat.getPopulation(), shape);
            for (int i = 0; i < stat.getPopulation(); i++) {
                bi.add(filters[i]);
            }

            /* run */
            stopwatch.reset();
            stopwatch.start();
            for (BloomFilter bf : bfSample) {
                bi.delete(bf);
            }
            stopwatch.stop();

            stat.registerResult(Stats.Phase.Delete, type, stopwatch.getNanoTime(), stat.getPopulation() - bi.count());

            System.out.println(stat.displayString(Stats.Phase.Delete, type));
        }
    }

    private static List<Stats> runTest(final Shape shape, final Constructor<? extends BloomIndex> constructor,
            final List<GeoName> sample, final BloomFilter[] filters, List<Stats> stats, boolean collectFilters, UsagePattern pattern)
                    throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {

        BloomIndex bi = doLoad(constructor, filters, shape, stats);

        for (Stats.Type type : Stats.Type.values()) {
            BloomFilter[] bfSample = pattern.createSample(shape, type, sample);
            doCount(type, bi, bfSample, stats, collectFilters);
        }

        // release the memory
        bi = null;

        for (Stats.Type type : Stats.Type.values()) {
            BloomFilter[] bfSample = pattern.createSample(shape, type, sample);
            doDelete(type, constructor, filters, bfSample, stats, shape);
        }

        return stats;
    }

    private static BloomIndex doLoad(final Constructor<? extends BloomIndex> constructor, final BloomFilter[] filters,
            final Shape shape, final List<Stats> stats)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi = null;
        StopWatch stopwatch = new StopWatch();
        for (int run = 0; run < RUN_COUNT; run++) {
            Stats stat = stats.get(run);
            bi = constructor.newInstance(stat.getPopulation(), shape);
            long elapsed = 0;

            for (int i = 0; i < stat.getPopulation(); i++) {
                stopwatch.reset();
                stopwatch.start();
                bi.add(filters[i]);
                stopwatch.stop();
                elapsed += stopwatch.getNanoTime();
            }
            stat.setLoad(elapsed);
            System.out.println(stat.loadDisplayString());
        }
        return bi;
    }

    private static void doCount(final Stats.Type type, final BloomIndex bi, final BloomFilter[] bfSample,
            final List<Stats> stats, boolean collectFilters) {
        final int sampleSize = bfSample.length;
        StopWatch stopwatch = new StopWatch();
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long found = 0;
            Stats stat = stats.get(run);
            for (int i = 0; i < sampleSize; i++) {
                Collection<BloomFilter> filterCapture = new ArrayList<BloomFilter>();
                stopwatch.reset();
                stopwatch.start();
                found += bi.count(filterCapture::add, bfSample[i]);
                stopwatch.stop();
                elapsed += stopwatch.getNanoTime();
                if (collectFilters) {
                    stat.addFoundFilters(type, bfSample[i], filterCapture);
                }
            }
            stat.registerResult(Stats.Phase.Query, type, elapsed, found);
            System.out.println(stat.displayString(Stats.Phase.Query, type));
        }

    }

    interface UsagePattern {
        public static BloomFilter makeFilter( Shape shape, Hasher hasher) {
            int bits = shape.getNumberOfHashFunctions() * hasher.size();
            double d = bits * 1.0 / shape.getNumberOfBits();
            return (d>2.0) ? new SimpleBloomFilter(shape, hasher) : new SparseBloomFilter( shape, hasher );
        }

        public String getName();

        public BloomFilter[] configure(int population, GeoNameIterator iter);

        Shape getShape(int population);

        BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample);
    }

    static class Reference implements UsagePattern {

        final BloomFilter[] filters = new BloomFilter[1000000]; // (1e6)
        final Shape shape = Shape.Factory.fromNP(3, 1.0 / 100000);

        @Override
        public String getName() {
            return "Reference";
        }
        @Override
        public BloomFilter[] configure(int population, GeoNameIterator iter) {
            if (filters[0] == null) {
                readFilters(iter);
            }
            return filters;
        }

        private void readFilters(GeoNameIterator iter) {
            System.out.print("Creating filters...");
            for (int i = 0; i < 1000000; i++) {
                filters[i] = UsagePattern.makeFilter(shape, GeoNameReferenceHasher.createHasher(iter.next()));
                if ((i % 1000) == 0) {
                    System.out.print(".");
                }
            }
            System.out.println(" done ");

        }

        @Override
        public Shape getShape(int population) {
            return shape;
        }

        @Override
        public BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample) {
            final int sampleSize = sample.size();
            BloomFilter[] bfSample = new BloomFilter[sampleSize];
            for (int i = 0; i < sample.size(); i++) {
                switch (type) {
                case COMPLETE:
                    bfSample[i] = UsagePattern.makeFilter(shape, GeoNameReferenceHasher.createHasher(sample.get(i)));
                    break;
                case HIGHCARD:
                    bfSample[i] = UsagePattern.makeFilter(shape, GeoNameReferenceHasher.hasherFor(sample.get(i).name));
                    break;
                case LOWCARD:
                    bfSample[i] = UsagePattern.makeFilter(shape,
                            GeoNameReferenceHasher.hasherFor(sample.get(i).feature_code));
                    break;
                }
            }
            return bfSample;
        }
    }

    static class GateKeeper implements UsagePattern {

        @Override
        public String getName() {
            return "GateKeeper";
        }

        @Override
        public BloomFilter[] configure(int population, GeoNameIterator iter) {
            BloomFilter[] filters = new BloomFilter[population];
            Shape shape = getShape(population);
            System.out.println( "Shape "+shape );
            for (int i = 0; i < population; i++) {
                filters[i] = UsagePattern.makeFilter(shape, GeoNameGatekeeperHasher.createHasher(iter.next()));
            }
            return filters;
        }

        @Override
        public Shape getShape(int population) {
            return Shape.Factory.fromNP(population, 1.0 / 100000);
        }

        @Override
        public BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample) {
            if (type == Stats.Type.COMPLETE) {
                final int sampleSize = sample.size();
                BloomFilter[] bfSample = new BloomFilter[sampleSize];
                for (int i = 0; i < sample.size(); i++) {
                    bfSample[i] = UsagePattern.makeFilter(shape, GeoNameGatekeeperHasher.createHasher(sample.get(i)));
                }
                return bfSample;
            }
            return new BloomFilter[0];
        }
    }
}
