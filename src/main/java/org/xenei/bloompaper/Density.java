package org.xenei.bloompaper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.geoname.GeoNameHasher;
import org.xenei.bloompaper.geoname.GeoNameIterator;

public class Density {

    // 9,772,346 max lines
    private static final int MAX_DENSITY = 9;
    private static final int SAMPLE_SIZE = 1000000;

    private final static int MAX_LOG_DEPTH = 25;

    public static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        return options;
    }

    public static void main(final String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Saturation", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Saturation", getOptions());
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
        int numberOfItems = 3;
        double probability = 1.0 / 100000;

        Shape shape = Shape.Factory.fromNP(numberOfItems, probability);
        Status status = new Status(shape);
        BloomFilter[] filters = new BloomFilter[SAMPLE_SIZE];
        try (GeoNameIterator geoIter = new GeoNameIterator(Density.class.getResource("/allCountries.txt"),shape)) {

            System.out.println("Reading test data");
            for (int density = 0; density < MAX_DENSITY; density++) {
                System.out.println("Saturation " + (density + 1));
                for (int i = 0; i < SAMPLE_SIZE; i++) {
                    final BloomFilter bf = new SimpleBloomFilter(shape, GeoNameHasher.createHasher(geoIter.next()));
                    if (density > 0) {
                        filters[i].mergeInPlace(bf);
                    } else {
                        filters[i] = bf;
                    }
                }
                double effectiveP = shape.getProbability(numberOfItems * (density + 1));
                Map<Integer, Map<Double, Integer>> points = new HashMap<Integer, Map<Double, Integer>>();
                status.record(points, density, filters, effectiveP);
                PrintStream ps = null;
                if (dir != null) {
                    ps = new PrintStream(new File(dir, "points" + (density + 1) + ".csv"));
                } else {
                    ps = System.out;
                }
                for (Map.Entry<Integer, Map<Double, Integer>> entry : points.entrySet()) {
                    for (Map.Entry<Double, Integer> inner : entry.getValue().entrySet()) {
                        ps.println(String.format("%s,%s,%s", entry.getKey(), inner.getKey(), inner.getValue()));
                    }
                }
                if (dir != null) {
                    ps.close();
                }
            }
        }

        PrintStream o = null;
        System.out.println("=== results ===");
        if (dir != null) {
            o = new PrintStream(new File(dir, "density.csv"));
        }

        System.out.println(status.getHeader());
        if (o != null) {
            o.println(status.getHeader());
        }
        for (int density = 0; density < MAX_DENSITY; density++) {

            System.out.println(status.getData(density));
            if (o != null) {
                o.println(status.getData(density));
            }
        }

        System.out.println("=== run complete ===");

        if (o != null) {
            o.close();
        }
    }

    private static class SaturationStats {
        double sd;
        long mean;
        int median;
        int mode;
        double p;

        @Override
        public String toString() {
            return String.format("%s,%s,%s,%s,%s", mean, median, mode, sd, p);
        }
    }

    private static class LogStats {
        double sd;
        double mean;
        double min = Double.MAX_VALUE;
        double max = 0.0;

        @Override
        public String toString() {
            return String.format("%s,%s,%s,%s", min, mean, max, sd);
        }
    }

    private static class Status {
        Map<Integer, int[]> counts;
        int dim;
        Map<Integer, SaturationStats> satStats;
        Map<Integer, LogStats> logStats;

        public Status(Shape config) {
            counts = new HashMap<Integer, int[]>();
            dim = config.getNumberOfBits();
            satStats = new HashMap<Integer, SaturationStats>();
            logStats = new HashMap<Integer, LogStats>();
        }

        public void record(Map<Integer, Map<Double, Integer>> points, int density, BloomFilter[] filters,
                double probability) throws FileNotFoundException {

            SaturationStats sat = new SaturationStats();
            LogStats log = new LogStats();

            int[] c = new int[dim];
            long count = 0;
            for (BloomFilter f : filters) {
                log.mean += logValue(f);
                sat.mean += f.cardinality();
                c[f.cardinality() - 1]++;
                count++;
            }
            counts.put(density, c);

            // calculate standard defiation
            sat.mean /= filters.length;
            log.mean /= filters.length;
            long satSum = 0;
            long satCount = 0;
            int modeCount = 0;
            for (int i = 0; i < dim; i++) {
                if (c[i] > 0) {
                    satSum += c[i] * Math.pow(i + 1 - sat.mean, 2);
                    satCount += c[i];
                    if (c[i] > modeCount) {
                        sat.mode = i + 1;
                        modeCount = c[i];
                    }
                }
                if (satCount >= count / 2 && sat.median == 0) {
                    sat.median = i + 1;
                }
            }

            sat.sd = Math.sqrt(satSum / (filters.length - 1));
            sat.p = probability;
            satStats.put(density, sat);

            double logSum = 0.0;
            for (BloomFilter f : filters) {
                double logV = logValue(f);
                log.min = Double.min(log.min, logV);
                log.max = Double.max(log.max, logV);
                logSum += Math.pow(logValue(f) - log.mean, 2);
                Map<Double, Integer> m = points.get(f.cardinality());
                if (m == null) {
                    m = new HashMap<Double, Integer>();
                    points.put(f.cardinality(), m);
                }
                Integer cnt = m.get(logV);
                if (cnt == null) {
                    m.put(logV, 1);
                } else {
                    m.put(logV, cnt + 1);
                }
            }
            log.sd = Math.sqrt(logSum / (filters.length - 1));
            logStats.put(density, log);
        }

        public String getHeader() {
            StringBuilder sb = new StringBuilder("'Saturation'");
            for (int i = 0; i < dim; i++) {
                sb.append(",").append(i + 1);
            }
            sb.append(",,Mean,Median,Mode,Standard Deviation,p");
            sb.append(",,Min,Mean,Max,Standard Deviation");
            return sb.toString();
        }

        public String getData(int density) {
            StringBuilder sb = new StringBuilder(String.format("'Sat. %s'", density + 1));
            int[] c = counts.get(density);
            for (int i = 0; i < dim; i++) {
                sb.append(",").append(c[i] == 0 ? " " : c[i]);
            }
            sb.append(",,").append(satStats.get(density));
            sb.append(",,").append(logStats.get(density));
            return sb.toString();
        }

        public double logValue(BloomFilter filter) {
            List<Integer> lst = new ArrayList<Integer>();
            filter.forEachIndex(lst::add);
            Collections.sort(lst, Collections.reverseOrder());
            return getApproximateLog(lst, MAX_LOG_DEPTH);
        }

    }

    /**
     * Gets the approximate log for this filter. If the Bloom filter is considered
     * as an unsigned number what is the approximate base 2 log of that value. The
     * depth argument indicates how many extra bits are to be considered in the log
     * calculation. At least one bit must be considered. If there are no bits on
     * then the log value is 0.
     *
     * @param depth the number of bits to consider.
     * @return the approximate log.
     */
    static public double getApproximateLog(List<Integer> lst, int depth) {
        if (depth == 0) {
            return 0;
        }
        int[] exp = getApproximateLogExponents(lst, depth);
        /*
         * this approximation is calculated using a derivation of
         * http://en.wikipedia.org/wiki/Binary_logarithm#Algorithm
         */
        // the mantissa is the highest bit that is turned on.
        if (exp[0] < 0) {
            // there are no bits so return 0
            return 0;
        }
        double result = exp[0];
        /*
         * now we move backwards from the highest bit until the requested depth is
         * achieved.
         */
        double exp2;
        for (int i = 1; i < exp.length; i++) {
            if (exp[i] == -1) {
                return result;
            }
            exp2 = exp[i] - exp[0]; // should be negative
            result += Math.pow(2.0, exp2);
        }
        return result;
    }

    /**
     * Gets the mantissa and characteristic powers of the log. The mantissa is in
     * position position 0. The remainder are characteristic powers.
     *
     * The depth is the depth to probe for characteristics. The effective limit is
     * 25 as beyond that the value of the calculated double does not change.
     *
     * @param depth the depth to probe.
     * @return An array of depth integers that are the exponents.
     */
    static public int[] getApproximateLogExponents(List<Integer> lst, int depth) {
        if (depth < 1) {
            return new int[] { -1 };
        }
        int[] exp = new int[depth];
        Iterator<Integer> iter = lst.iterator();
        exp[0] = iter.next();
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            if (iter.hasNext()) {
                exp[i] = iter.next();
                if (exp[i] - exp[0] < -25) {
                    exp[i] = -1;
                }
            } else {
                exp[i] = -1;
            }
            /*
             * 25 bits from the start make no difference in the double calculation so we can
             * short circuit the method here.
             */

            if (exp[i] == -1) {
                return exp;
            }
        }
        return exp;
    }
}
