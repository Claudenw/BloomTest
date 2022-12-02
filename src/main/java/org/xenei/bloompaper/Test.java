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
import org.apache.commons.collections4.bloomfilter.BitMap;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.SparseBloomFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.xenei.bloompaper.Stats.Type;
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
import org.xenei.bloompaper.index.naturalbloofi.NaturalBloofi;
import org.xenei.bloompaper.index.shardedlist.ShardedList;
import org.xenei.bloompaper.index.BloomIndexArray;

/**
 * Class that executes the tests.
 *
 */
public class Test {

    /**
     * List map of constructor name to constructor for the test.
     */
    public static Map<String, Constructor<? extends BloomIndex>> constructors = new HashMap<String, Constructor<? extends BloomIndex>>();

    /**
     * The number of times to run each test.
     */
    private static int RUN_COUNT = 5;

    /**
     * The populations to use for the tests.  Will execute the test on each population RUN_COUNT times.
     */
    static int[] POPULATIONS = { 100, 1000, 10000, 100000, 1000000 };

    /**
     * Initializes the constructors map.
     *
     * @throws NoSuchMethodException if the constructor for a specified class can not be found.
     * @throws SecurityException if constructor is not accessible.
     */
    public static void init() throws NoSuchMethodException, SecurityException {
        constructors.put("Hamming", BloomIndexHamming.class.getConstructor(int.class, Shape.class));
        constructors.put("Bloofi", BloomIndexBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("FlatBloofi", BloomIndexFlatBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("BF-Trie4", BloomIndexBFTrie4.class.getConstructor(int.class, Shape.class));
        constructors.put("BF-Trie8", BloomIndexBFTrie8.class.getConstructor(int.class, Shape.class));
        constructors.put("Array", BloomIndexArray.class.getConstructor(int.class, Shape.class));
        constructors.put("List", BloomIndexList.class.getConstructor(int.class, Shape.class));
        constructors.put("NaturalBloofi", NaturalBloofi.class.getConstructor(int.class, Shape.class));
        constructors.put("ShardedList", ShardedList.class.getConstructor(int.class, Shape.class));
    }

    /**
     * Gets the options for the command line.
     * @return the Options object.
     */
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

    /**
     * Main entry.
     * @param args the arguments for the applicaiton.
     * @throws Exception on error
     */
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
        Summary.doOutput(table, null, true, false, usagePattern.getName());

        if (dir != null) {
            try (PrintStream ps = new PrintStream(new File(dir, "data.csv"))) {
                table.forEachPhase(table.new CSV(ps));
            }
            try (PrintStream ps = new PrintStream(new File(dir, "summary.csv"))) {
                summary.new CSV(ps, usagePattern.getName()).print();
            }
        }

        System.out.println("=== run complete ===");
    }

    /**
     * Executes the delete tests.
     * @param bi bloomIndex under test.
     * @param type The type of Bloom filter we are deleting.
     * @param bfSample the list of sample to delete.
     * @param stats the list of statistics for this test.
     * @throws InstantiationException on instantiation error in test constructor.
     * @throws IllegalAccessException on access error in test constructor.
     * @throws InvocationTargetException on invocation error in test constructor.
     */
    private static void doDelete(BloomIndex bi, Stats.Type type, final BloomFilter[] bfSample, final List<Stats> stats)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        boolean[] results = new boolean[bfSample.length];

        StopWatch stopwatch = new StopWatch();
        for (int run = 0; run < RUN_COUNT; run++) {

            Stats stat = stats.get(run);

            /* run */
            stopwatch.reset();
            stopwatch.start();
            for (int i = 0; i < bfSample.length; i++) {
                results[i] = bi.delete(bfSample[i]);
            }
            stopwatch.stop();

            stat.registerResult(Stats.Phase.Delete, type, stopwatch.getNanoTime(), stat.getPopulation() - bi.count());

            System.out.println(stat.displayString(Stats.Phase.Delete, type));
            // add the deleted records back
            for (int i = 0; i < bfSample.length; i++) {
                if (results[i]) {
                    bi.add(bfSample[i]);
                }
            }
        }
    }

    /**
     * Executes the query tests.
     * @param shape The shape of the Bloom filters.
     * @param constructor the constructor to build the test with.
     * @param sample the sample Bloom filters to search for
     * @param filters the List of all bloom filters in the test.
     * @param stats the stats to add the test results to.
     * @param collectFilters if {@code true} then matching filters are preserved for verification.
     * @param pattern the useage pattern we are testing.
     * @return the List of Stats.  Same as @{code stats} param.
     * @throws InstantiationException on instantiation error in test constructor.
     * @throws IllegalAccessException on access error in test constructor.
     * @throws InvocationTargetException on invocation error in test constructor.
     */
    private static List<Stats> runTest(final Shape shape, final Constructor<? extends BloomIndex> constructor,
            final List<GeoName> sample, final BloomFilter[] filters, List<Stats> stats, boolean collectFilters,
            UsagePattern pattern) throws InstantiationException, IllegalAccessException, InvocationTargetException {

        BloomIndex bi = doLoad(constructor, filters, shape, stats);

        System.out.println("Calculating query times");
        for (Stats.Type type : pattern.getSupportedTypes()) {
            BloomFilter[] bfSample = pattern.createSample(shape, type, sample);
            doCount(type, bi, bfSample, stats, collectFilters);
        }

        System.out.println("Calculating delete times");
        for (Stats.Type type : pattern.getSupportedTypes()) {
            BloomFilter[] bfSample = pattern.createSample(shape, type, sample);
            doDelete(bi, type, bfSample, stats);
        }
        System.out.println("test complete");
        return stats;
    }

    /**
     * Executes the load timing tests and produces a populated BloomIndex object.
     * @param constructor The constructor for the test.
     * @param filters the list of filters to load.
     * @param shape the shape of the filters.
     * @param stats the Stats to populate.
     * @return the loaded bloom index.
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private static BloomIndex doLoad(final Constructor<? extends BloomIndex> constructor, final BloomFilter[] filters,
            final Shape shape, final List<Stats> stats)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BloomIndex bi = null;
        System.out.println("Calculating load times");
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

    /**
     * Executes the counting
     * @param type The type of Bloom filter being counted
     * @param bloomIndex the Bloom index to count.
     * @param bfSample the Bloom filter sample to test with.
     * @param stats the stats to update.
     * @param collectFilters if {@code true} the matching bloom filters will be preserved for verification testing.
     */
    private static void doCount(final Stats.Type type, final BloomIndex bloomIndex, final BloomFilter[] bfSample,
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
                found += bloomIndex.count(filterCapture::add, bfSample[i]);
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

    /**
     * The interface that defines the Usage Pattern functions.  These function modify how the tests are
     * populated and run.
     *
     */
    interface UsagePattern {
        /**
         * Returns an array of supported types for this usage pattern.
         * @return the list of supported types.
         */
        public Stats.Type[] getSupportedTypes();

        /**
         * Gets the name of the test.
         * @return the names of the test,
         */
        public String getName();

        /**
         * Generates a list of Bloom filters for the specified population.
         * @param population the population to generate.
         * @param iter the iterator to generated the population from.
         * @return an array of Bloom filters at least {@code population} items long.
         */
        public BloomFilter[] configure(int population, GeoNameIterator iter);

        /**
         * The shape of Bloom filters for the specified population.
         * @param population the population to get the filters for.
         * @return the Shape of the filter.
         */
        Shape getShape(int population);

        /**
         * Creates a set of samples Bloom filters from the sample Geonames.
         * @param shape the Shape of the sample items.
         * @param type the type of filter to generate
         * @param sample the sample of GetName objects to generate the Bloom filters from.
         * @return an array of Bloom filters for the provided GeoNames.
         */
        BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample);
    }

    /**
     * A class that implements the Referance usage data.
     *
     */
    static class Reference implements UsagePattern {
        /**
         * The filters, these do not chagne between runs.
         */
        final BloomFilter[] filters = new BloomFilter[1000000]; // (1e6)
        /**
         * The shape for this type of filter.
         */
        final Shape shape = Shape.fromNP(3, 1.0 / 100000);

        @Override
        public Type[] getSupportedTypes() {
            return Type.values();
        }

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

        /**
         * Reads the filters from the iterator.
         * @param iter the iterator to read from.
         */
        private void readFilters(GeoNameIterator iter) {
            System.out.print("Creating filters...");
            for (int i = 0; i < 1000000; i++) {
                filters[1] = GeoNameReferenceHasher.createHasherCollection(iter.next()).filterFor(shape);
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
                bfSample[i] = new SimpleBloomFilter(shape);
                switch (type) {
                case COMPLETE:
                    GeoNameReferenceHasher.createHasherCollection(sample.get(i)).fill(bfSample[i]);
                    break;
                case HIGHCARD:
                    bfSample[i].merge(GeoNameReferenceHasher.hasherFor(sample.get(i).name));
                    break;
                case LOWCARD:
                    bfSample[i].merge(GeoNameReferenceHasher.hasherFor(sample.get(i).feature_code));
                    break;
                }
            }
            return bfSample;
        }
    }

    /**
     * Class that implements the Gatekeeper usage pattern.
     *
     */
    static class GateKeeper implements UsagePattern {

        @Override
        public String getName() {
            return "GateKeeper";
        }

        /**
         * Makes either Simple or Sparse Bloom filters based on the estimated number of bits in the
         * Bloom filter relative to the number of longs necessry to make a bitmap version.
         * @param shape the shape of the Bloom filter.
         * @param hasher the hasher for the filter.
         * @return A Bloom filter of the proper shape built with the hasher.
         */
        private BloomFilter makeFilter(Shape shape, Hasher hasher) {
            double d = shape.getNumberOfHashFunctions()  / (double)BitMap.numberOfBitMaps(shape.getNumberOfBits());
            BloomFilter bf = (d > 2.0) ? new SimpleBloomFilter(shape) : new SparseBloomFilter(shape);
            bf.merge(hasher);
            return bf;
        }

        @Override
        public Type[] getSupportedTypes() {
            return new Type[] { Type.COMPLETE };
        }

        @Override
        public BloomFilter[] configure(int population, GeoNameIterator iter) {
            BloomFilter[] filters = new BloomFilter[population];
            Shape shape = getShape(population);
            System.out.println("Shape " + shape);
            for (int i = 0; i < population; i++) {
                filters[i] = makeFilter(shape, GeoNameGatekeeperHasher.createHasher(iter.next()));
            }
            return filters;
        }

        @Override
        public Shape getShape(int population) {
            return Shape.fromNP(population, 0.1);
        }

        @Override
        public BloomFilter[] createSample(Shape shape, Stats.Type type, List<GeoName> sample) {
            if (type == Stats.Type.COMPLETE) {
                final int sampleSize = sample.size();
                BloomFilter[] bfSample = new BloomFilter[sampleSize];
                for (int i = 0; i < sample.size(); i++) {
                    bfSample[i] = makeFilter(shape, GeoNameGatekeeperHasher.createHasher(sample.get(i)));
                }
                return bfSample;
            }
            throw new IllegalArgumentException(String.format("Type %s not supported", type));
        }
    }
}
