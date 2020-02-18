package org.xenei.bloom.speedTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.PrimitiveIterator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.bloomfilter.BitSetBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.HasherBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;
import org.apache.commons.collections4.bloomfilter.hasher.function.MD5Cyclic;
import org.xenei.bloom.speedTest.geoname.GeoName;

/*
 * Ok, first I think there is a misunderstanding.  In most cases when
constructing the Bloom filter there will be more than one hash per object
inserted.  Using the bloom filter calculator
https://hur.st/bloomfilter/?n=10000&p=50000&m=&k= for 10K items with a
collision probability of 1 in 50K yields a Bloom filter with 225200 bits of
which 16 are turned on for any one item.

Thus you have to generate 16 hash functions for the object and look for all
16 bits.  The absolute fastest way to do this i to turn  the 225,200 bits
into 3519 longs and do the logical OR between the two Bloom filters.

There are strategies to try to reduce the number of longs needed to do the
comparison using various compression strategies like EWAH. (
https://github.com/lemire/javaewah) but the operational steps are the same.

 */
public class Test {

    // 9,772,346 max lines
    private static int RUN_COUNT = 5;

    private static int[] RUNSIZE = { 100, 1000, 10000, 100000, 1000000 };

    private static final int INT_CHK = 1;
    private static final int LONG_CHK = 2;
    private static final int HASHER_CHK = 3;
    private static final int FILTER_CHK = 4;

    public static Options getOptions() {

        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addOption( "n", "number", true, "The number of items in the filter (defaults to 10K)");
        options.addOption( "p", "probability", true, "The probability of collisions (defaults to 1/50000).  May be specified as x/y or double format");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        options.addOption("i", "iterations", true, "The number of iterations defualt=" + RUN_COUNT);
        return options;
    }

    public static void main(final String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        int n = 10000;
        double p = 1.0/50000;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", getOptions());
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


        if (cmd.hasOption("n")) {
            n = Integer.valueOf(cmd.getOptionValue("n"));
        }

        if (cmd.hasOption("p"))
        {
            String pStr = cmd.getOptionValue("p");
            String[] parts = pStr.split( "/");
            if (parts.length == 1)
            {
                p = Double.parseDouble( parts[0] );
            }
            else
            {
                p = Double.parseDouble( parts[0] ) / Double.parseDouble( parts[1] );
            }
        }
        Shape shape = new Shape( new MD5Cyclic(), n, p );

        File dir = null;
        if (cmd.hasOption("o")) {
            dir = new File(cmd.getOptionValue("o"));
            if (!dir.exists()) {
                dir.mkdirs();
            } else if (!dir.isDirectory()) {
                throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
            }
        }

        final List<Stats> table = new ArrayList<Stats>();
        final StaticHasher[] hashers = new StaticHasher[1000000]; // (1e6)
        final URL inputFile = Test.class.getResource("/allCountries.txt");
        final List<GeoName> sample = new ArrayList<GeoName>(1000); // (1e3)


        System.out.println("Reading test data");
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
        for (int i = 0; i < 1000000; i++) {
            final GeoName gn = GeoName.parse(br.readLine());
            if ((i % 1000) == 0) {
                sample.add(gn);
            }

            hashers[i] = new StaticHasher( GeoNameFilterFactory.create(gn.name), shape );
        }

        // run the tests

        for (final int population : RUNSIZE) {
            final Stats[] stats = new Stats[RUN_COUNT];
            for (int run = 0; run < RUN_COUNT; run++) {
                stats[run] = new Stats(population);
            }
            table.addAll(runTest(shape, sample, hashers, stats));
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

    /**
     * Create an integer array from the hasher values.
     * This is used to remove the overhead of extracting ints from the test timing.
     * @param hasher the hasher to process
     * @param shape the shape to create ints for.
     * @return an array of ints.
     */
    private static int[] extractInts( StaticHasher hasher, Shape shape ) {
        int[] result = new int[hasher.size()];
        PrimitiveIterator.OfInt iter = hasher.getBits(shape);
        int i=0;
        while (iter.hasNext())
        {
            result[i++] = iter.nextInt();
        }
        return result;
    }


    private static void doIntegerMatch(int[] values, BitSet bitset)
    {
        for (int i : values )
        {
            if (!bitset.get(i))
            {
                return;
            }
        }
    }

    private static void doLongMatch(long[] filterLongs, long[] sampleLongs )
    {
        if (sampleLongs.length > filterLongs.length)
        {
            for (int i = 0;i<sampleLongs.length;i++)
            {
                if (sampleLongs[i] != (filterLongs[i] & sampleLongs[i]))
                {
                    return;
                }
            }
        }
    }

    private static List<Stats> runTest(final Shape shape,
            final List<GeoName> sample,
            final StaticHasher[] hashers, final Stats[] stats) throws IOException {

        final BloomFilter bf = doLoad(hashers, shape, stats);
        final int sampleSize = sample.size();
        BitSet bitset = BitSet.valueOf(bf.getBits());

        StaticHasher[] bfSample = new StaticHasher[sampleSize];
        for (int i = 0; i < sample.size(); i++) {
            bfSample[i] = new StaticHasher( GeoNameFilterFactory.create(sample.get(i).name), shape );
        }

        /* do test by bit check */
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            for (int i = 0; i < sampleSize; i++) {
                int[] values = extractInts( bfSample[i], shape );
                start = System.currentTimeMillis();
                doIntegerMatch( values, bitset );
                elapsed += (System.currentTimeMillis() - start);
            }
            registerResult(stats[run], INT_CHK, elapsed);

            System.out.println(String.format("%s %s population %s run %s elapsed time %s", shape, INT_CHK,
                    stats[run].population, run, elapsed));
        }

        /* do test by long check */
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            long[] filterLongs = bf.getBits();
            for (int i = 0; i < sampleSize; i++) {
                BloomFilter sampleFilter = new HasherBloomFilter( bfSample[i], shape );
                long[] sampleLongs = sampleFilter.getBits();
                start = System.currentTimeMillis();
                doLongMatch( filterLongs, sampleLongs );
                elapsed += (System.currentTimeMillis() - start);
            }
            registerResult(stats[run], LONG_CHK, elapsed);

            System.out.println(String.format("%s %s population %s run %s elapsed time %s", shape, LONG_CHK,
                    stats[run].population, run, elapsed));
        }

        /* do test by hasher check */
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            long[] filterLongs = bf.getBits();
            for (int i = 0; i < sampleSize; i++) {
                start = System.currentTimeMillis();
                bf.contains( bfSample[i] );
                elapsed += (System.currentTimeMillis() - start);
            }
            registerResult(stats[run], HASHER_CHK, elapsed);

            System.out.println(String.format("%s %s population %s run %s elapsed time %s", shape, HASHER_CHK,
                    stats[run].population, run, elapsed));
        }

        /* do test by  filter check */
        for (int run = 0; run < RUN_COUNT; run++) {
            long elapsed = 0;
            long start = 0;
            long[] filterLongs = bf.getBits();
            for (int i = 0; i < sampleSize; i++) {
                BloomFilter sampleFilter = new BitSetBloomFilter( bfSample[i], shape );
                start = System.currentTimeMillis();
                bf.contains( sampleFilter);
                elapsed += (System.currentTimeMillis() - start);
            }
            registerResult(stats[run], FILTER_CHK, elapsed);

            System.out.println(String.format("%s %s population %s run %s elapsed time %s", shape, FILTER_CHK,
                    stats[run].population, run, elapsed));
        }

        return Arrays.asList(stats);
    }

    /**
     * Load the bloom filter for testing.
     * @param hashers the list of all the hashers.
     * @param shape the shape for the filter.
     * @param stats the list of Stats.
     * @return the Bloom filter
     */
    private static BloomFilter doLoad(final StaticHasher[] hashers,
            final Shape shape, final Stats[] stats) {

        BloomFilter filter = new BitSetBloomFilter( shape );

        for (int run = 0; run < RUN_COUNT; run++) {
            stats[run].type = shape.toString();
            final long start = System.currentTimeMillis();
            for (int i = 0; i < stats[run].population; i++) {
                filter.merge(hashers[i]);
            }
            final long end = System.currentTimeMillis();
            stats[run].load = end - start;
            System.out.println(String.format("%s 0 population %s run %s  load time %s", shape.toString(),
                    stats[run].population, run, end - start));
        }
        return filter;
    }


    private static void registerResult(final Stats stat, final int chkId, final long elapsed) {
        switch (chkId) {
        case INT_CHK:
            stat.intChk = elapsed;
            break;
        case LONG_CHK:
            stat.longChk = elapsed;
            break;
        case HASHER_CHK:
            stat.hasherChk = elapsed;
            break;
        case FILTER_CHK:
            stat.filterChk = elapsed;
            break;
        default:
            throw new IllegalArgumentException(String.format("%s is not a valid check ID", chkId));
        }

    }
}
