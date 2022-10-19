package org.xenei.bloompaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.FrozenBloomFilter;

/**
 * A record of the data from a single run.
 */
public class Stats {
    /**
     * The type of Bloom filter used in the test.
     */
    public enum Type {
        COMPLETE, HIGHCARD, LOWCARD
    }

    /**
     * The phase of the test.
     */
    public enum Phase {
        Query, Delete
    }

    /**
     * used to convert timing to seconds.
     */
    public static final double TIME_SCALE = 0.000000001;
    /**
     * The usage type for this stat.
     */
    private final String usageType;
    /**
     * The index being tested.
     */
    private final String indexName;
    /**
     * The population for this run.
     */
    private final int population;
    /**
     * the run number.
     */
    private final int run;

    /**
     * How long it took to load the data.
     */
    private long load;

    /**
     * A list of found filters.  Used for verification.
     */
    @SuppressWarnings("unchecked")
    private Map<FrozenBloomFilter, Set<FrozenBloomFilter>>[] foundFilters = new HashMap[Type.values().length];

    /**
     * An array of timings for phase and type
     */
    private long[][] time = new long[Phase.values().length][Type.values().length];

    /**
     * An array of counts for phase and type.
     */
    private long[][] count = new long[Phase.values().length][Type.values().length];

    /**
     * Constructor.
     * @param usageType the usage type for this set of statistics.
     * @param indexName the index name that generated the statistics.
     * @param population the population for this set of statistics.
     * @param run the run number for this set of statistics.
     */
    public Stats(String usageType, String indexName, int population, int run) {
        this.usageType = usageType;
        this.indexName = indexName;
        this.population = population;
        this.run = run;
    }

    /**
     * Adds the found filters for the specified filter under the specified type of execution.
     * @param type the type of execution.
     * @param filter the filter used for matching.
     * @param filters the filters found during the matching operation.
     */
    public void addFoundFilters(Type type, BloomFilter filter, Collection<BloomFilter> filters) {
        if (filters != null) {
            if (foundFilters[type.ordinal()] == null) {
                foundFilters[type.ordinal()] = new HashMap<FrozenBloomFilter, Set<FrozenBloomFilter>>();
            }
            Set<FrozenBloomFilter> set = new HashSet<FrozenBloomFilter>();
            filters.stream().map(FrozenBloomFilter::makeInstance).forEach(set::add);
            foundFilters[type.ordinal()].put(FrozenBloomFilter.makeInstance(filter), set);
        }
    }

    /**
     * Sets the load time.
     * @param load the load time.
     */
    public void setLoad(long load) {
        this.load = load;
    }

    /**
     * Gets all the found filters for the specified operation.
     * @param type the operation type.
     * @return the map of found filters by target filter.
     */
    public Map<FrozenBloomFilter, Set<FrozenBloomFilter>> getFound(Type type) {
        Map<FrozenBloomFilter, Set<FrozenBloomFilter>> result = foundFilters[type.ordinal()];
        return result == null ? Collections.emptyMap() : result;
    }

    /**
     * Load the filter maps from a directory.
     * Filter maps are large structures.  This method loads them from disk.
     * @param dir the directory to load from.
     * @throws IOException
     */
    public void loadFilterMaps(File dir) throws IOException {
        // check if populated
        for (int i = 0; i < Type.values().length; i++) {
            if (foundFilters[i] != null) {
                return;
            }
        }
        Serde serde = new Serde();
        try (DataInputStream filters = new DataInputStream(new FileInputStream(Table.filterMapFile(dir, getName())))) {
            serde.readFilterMaps(filters, this);
        }
    }

    /**
     * Gets the name of the index that generated these stats.
     * @return the name of the index that generated these stats.
     */
    public String getName() {
        return indexName;
    }

    /**
     * Gets the number of the run that generated these stats.
     * @return the number of the run that generated these stats.
     */
    public int getRun() {
        return run;
    }

    /**
     * Gets the count for the specific phase and type.
     * @param phase the phase to get the count for.
     * @param type the Bloom filter type.
     * @return the count for the specified phase and type.
     */
    public long getCount(Phase phase, Type type) {
        return count[phase.ordinal()][type.ordinal()];
    }

    /**
     * Gets the elapsed time in seconds for the specific phase and type.
     * @param phase the phase to get the count for.
     * @param type the Bloom filter type.
     * @return the elapsed time in seconds for the specified phase and type.
     */
    public double getElapsed(Phase phase, Type type) {
        return time[phase.ordinal()][type.ordinal()] * TIME_SCALE;
    }

    /**
     * Gets the load time in seconds for stats.
     * @return the load time in seconds for stats.
     */
    public double getLoad() {
        return load * TIME_SCALE;
    }

    /**
     * Gets the populations for the stats
     * @return the population for the stats.
     */
    public int getPopulation() {
        return population;
    }

    /**
     * Register results for the stats
     * @param phase the phase being tested.
     * @param type the type of filter being used.
     * @param elapsed the elapsed time in milliseconds.
     * @param count the count of items detected in the test.
     * @param falsePositives the count of falst positives on this run.
     */
    public void registerResult(final Phase phase, final Type type, final long elapsed, final long count) {
        this.time[phase.ordinal()][type.ordinal()] = elapsed;
        this.count[phase.ordinal()][type.ordinal()] = count;
    }

    /**
     * Gets a Display string of the summary results.
     * @param phase the phase to display
     * @param type the type of filter to display.
     * @return the human readable summary.
     */
    public String displayString(final Phase phase, Type type) {
        return String.format("%s %s %s %s population %s run %s  exec time %f (%s)", indexName, usageType, phase, type,
                population, run, getElapsed(phase, type), getCount(phase, type));
    }

    /**
     * Gets a human readable summary of the load time.
     * @return
     */
    public String loadDisplayString() {
        return String.format("%s population %s run %s load time %s", indexName, population, run, getLoad());
    }

    /**
     * Class to perform binary serialization/deserialization of the Stats object.
     * This is stored in the .dat files as well as the .fmf files.
     */
    public static class Serde {

        /**
         * Write the stats to the output stream.
         * This is the .dat file contents.
         * @param out the output stream to write to.
         * @param stats the statistics to write.
         * @throws IOException on IO Error
         */
        public void writeStats(DataOutputStream out, Stats stats) throws IOException {
            out.writeUTF(stats.usageType);
            out.writeUTF(stats.indexName);
            out.writeInt(stats.population);
            out.writeInt(stats.run);
            out.writeLong(stats.load);
            writeLongMatrix(out, stats.time);
            writeLongMatrix(out, stats.count);
        }

        /**
         * Write a matrix of long values to the output stream.
         * @param out the output stream
         * @param matrix the matrix of values to write.
         * @throws IOException on IO Error.
         */
        private void writeLongMatrix(DataOutputStream out, long[][] matrix) throws IOException {
            out.writeInt(Phase.values().length);
            for (int p = 0; p < Phase.values().length; p++) {
                writeLongArray(out, matrix[p]);
            }
        }

        /**
         * Write a long array to the output stream
         * @param out the outputs stream
         * @param arry the array of longs to write.
         * @throws IOException on IO error.
         */
        private void writeLongArray(DataOutputStream out, long[] arry) throws IOException {
            out.writeInt(arry.length);
            for (long l : arry) {
                out.writeLong(l);
            }
        }

        /**
         * Write the filter maps to the outpust stream.
         * this is the .fmf file format.
         * @param out the output stream
         * @param stats the statis object.
         * @throws IOException on IO error.
         */
        public void writeFilterMaps(DataOutputStream out, Stats stats) throws IOException {
            Map<FrozenBloomFilter, Set<FrozenBloomFilter>> map;
            out.writeInt(Type.values().length);
            for (Type t : Type.values()) {
                map = stats.getFound(t);
                if (map == null || map.isEmpty()) {
                    out.writeInt(0);
                } else {
                    out.writeInt(map.size());
                    for (Map.Entry<FrozenBloomFilter, Set<FrozenBloomFilter>> entry : map.entrySet()) {
                        writeBloomFilter(out, entry.getKey());
                        out.writeInt(entry.getValue().size());
                        for (BloomFilter bf : entry.getValue()) {
                            writeBloomFilter(out, bf);
                        }
                    }
                }
            }
        }

        /**
         * Write a Bloom filter to output stream
         * @param out output stream.
         * @param bf the Bloom filter to write
         * @throws IOException on IO Error.
         */
        private void writeBloomFilter(DataOutputStream out, BloomFilter bf) throws IOException {
            out.writeInt(bf.getShape().getNumberOfHashFunctions());
            out.writeInt(bf.getShape().getNumberOfBits());
            writeLongArray(out, bf.asBitMapArray());
        }

        /**R
         * Read the stats object from an input stream.
         * this is the .dat format.
         * @param in the input stream to read.
         * @return the deserialzied stats object.
         * @throws IOException
         */
        public Stats readStats(DataInputStream in) throws IOException {
            Stats result = new Stats(in.readUTF(), in.readUTF(), in.readInt(), in.readInt());
            result.load = in.readLong();
            result.time = readLongMatrix(in);
            result.count = readLongMatrix(in);
            return result;
        }

        /**
         * Reads a long matrix from the input stream.
         * @param in the Input stream
         * @return the long matrix.
         * @throws IOException on IO error.
         */
        private long[][] readLongMatrix(DataInputStream in) throws IOException {
            int p = in.readInt();
            long[][] matrix = new long[p][];
            for (int i = 0; i < p; i++) {
                matrix[i] = readLongArray(in);
            }
            return matrix;
        }

        /**
         * Reads a long array from the input stream.
         * @param in input stream to read.
         * @return the long array.
         * @throws IOException on IO error.
         */
        private long[] readLongArray(DataInputStream in) throws IOException {
            int len = in.readInt();
            long[] arry = new long[len];
            for (int i = 0; i < len; i++) {
                arry[i] = in.readLong();
            }
            return arry;
        }

        /**
         * Reads filter maps from the input stream.
         * this is the .fmf format.
         * @param in the input stream to read.
         * @param stats the stats for the filter maps.
         * @throws IOException on IO error.
         */
        public void readFilterMaps(DataInputStream in, Stats stats) throws IOException {
            int len = in.readInt();
            @SuppressWarnings("unchecked")
            Map<FrozenBloomFilter, Set<FrozenBloomFilter>>[] result = new HashMap[len];

            for (int t = 0; t < len; t++) {
                int count = in.readInt();
                if (count != 0) {
                    result[t] = new HashMap<FrozenBloomFilter, Set<FrozenBloomFilter>>();
                    for (int f = 0; f < count; f++) {
                        FrozenBloomFilter key = readBloomFilter(in);
                        Set<FrozenBloomFilter> value = new HashSet<FrozenBloomFilter>();
                        int sLen = in.readInt();
                        for (int i = 0; i < sLen; i++) {
                            value.add(readBloomFilter(in));
                        }
                        result[t].put(key, value);
                    }
                }
            }
            stats.foundFilters = result;
        }

        /**
         * Read bloom filters from input stream.
         * @param in the input stream to read.
         * @return a FrozenBloomFilter.
         * @throws IOException on IO error.
         */
        private FrozenBloomFilter readBloomFilter(DataInputStream in) throws IOException {
            Shape shape = Shape.fromKM(in.readInt(), in.readInt());
            long[] bitMaps = readLongArray(in);
            BitMapProducer producer = BitMapProducer.fromBitMapArray(bitMaps);
            return new FrozenBloomFilter(shape, producer);
        }

    }

    /**
     * Class to perform binary serialization/deserialization of the Stats object.
     * This is stored in the .dat files as well as the .fmf files.
     */
    public class CSV {
        private PrintStream out;

        public CSV(PrintStream out) {
            this.out = out;
        }

        /**
         * Generate the CSV header for this stat
         * @return the CSV header for this stat.
         */
        public void printHeader() {
            out.print("'Index Name', 'Usage', 'Run', 'Phase', 'Population', 'Load Elapsed'");
            for (Type type : Type.values()) {
                out.format(", '%1$s Elapsed', '%1$s Count'", type);
            }
            out.println();
        }

        public void printLine(Phase phase) {
            out.format("'%s','%s', %s,'%s',%s,%s", indexName, usageType, run, phase, population, load);
            for (Type type : Type.values()) {
                out.format(",%s,%s", getElapsed(phase, type), getCount(phase, type));
            }
            out.println();
        }

        /**
         * Report the statistics for the specified phase.
         * @param phase the phase to report for.
         * @return the reporting string CSV.
         */

    }

}
