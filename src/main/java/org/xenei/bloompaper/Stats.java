package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.xenei.bloompaper.index.FrozenBloomFilter;

/**
 * A record of the data from a single run.
 */
public class Stats {
    public enum Type {
        COMPLETE, HIGHCARD, LOWCARD
    }

    public enum Phase {
        Query, Delete
    }

    public static final double TIME_SCALE = 0.000000001;
    private final String indexName;
    private final int population;
    private final int run;

    private long load;

    @SuppressWarnings("unchecked")
    private Map<FrozenBloomFilter, Set<FrozenBloomFilter>>[] foundFilters = new HashMap[Type.values().length];

    private long[][] time = new long[Phase.values().length][Type.values().length];
    private long[][] count = new long[Phase.values().length][Type.values().length];

    public static String getHeader() {
        StringBuilder sb = new StringBuilder("'Index Name', 'Run', 'Phase', 'Population', 'Load Elapsed'");
        for (Type type : Type.values()) {
            sb.append(String.format(", '%1$s Elapsed', '%1$s Count'", type));
        }
        return sb.toString();
    }

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

    public void setLoad(long load) {
        this.load = load;
    }

    public Map<FrozenBloomFilter, Set<FrozenBloomFilter>> getFound(Type type) {
        Map<FrozenBloomFilter, Set<FrozenBloomFilter>> result = foundFilters[type.ordinal()];
        return result == null ? Collections.emptyMap() : result;
    }

    public Stats(String indexName, int population, int run) {
        this.indexName = indexName;
        this.population = population;
        this.run = run;
    }

    public String reportStats(Phase phase) {
        StringBuilder sb = new StringBuilder(
                String.format("'%s',%s,'%s',%s,%s", indexName, run, phase, population, load));
        for (Type type : Type.values()) {
            sb.append(String.format(",%s,%s", getElapsed(phase, type), getCount(phase, type)));
        }
        return sb.toString();
    }

    public void loadFilterMaps(File dir) throws IOException {
        // check if populated
        for (int i=0;i<Type.values().length;i++) {
            if (foundFilters[i] != null) {
                return;
            }
        }
        Serde serde = new Serde();
        serde.readFilterMaps( dir, getName(), this);
    }

    public static Table parse(BufferedReader reader) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withQuote('\'');

        String line = reader.readLine();
        if (!Stats.getHeader().equals(line)) {
            String s = String.format("Wrong header.  Wrong version? Expected:\n%s\nRead:\n%s", Stats.getHeader(), line);
            throw new IOException(s);
        }

        Map<String, List<Stats>> table = new HashMap<String, List<Stats>>();
        while ((line = reader.readLine()) != null) {
            CSVParser parser = CSVParser.parse(line, format);
            List<CSVRecord> lst = parser.getRecords();
            CSVRecord rec = lst.get(0);

            Stats stat = new Stats(rec.get(0), Integer.parseInt(rec.get(3)), Integer.parseInt(rec.get(1)));
            stat.load = Integer.parseInt(rec.get(4));
            for (Phase phase : Phase.values()) {
                if (phase != Phase.valueOf(rec.get(2))) {
                    throw new IOException(
                            String.format("Wrong phase for %s.  Expected: %s Found: %s", stat, phase, rec.get(2)));
                }
                for (Type type : Type.values()) {
                    int idx = 5 + (type.ordinal() * 2);

                    stat.registerResult(phase, type, Long.parseLong(rec.get(idx)), Long.parseLong(rec.get(idx + 1)));
                }
                if (phase.ordinal() + 1 < Phase.values().length) {
                    parser = CSVParser.parse(reader.readLine(), format);
                    lst = parser.getRecords();
                    rec = lst.get(0);
                }
            }
            List<Stats> resultList = table.get(stat.getName());
            if (resultList == null) {
                resultList = new ArrayList<Stats>();
                table.put(stat.getName(), resultList);
            }
            resultList.add(stat);
        }
        Table result = new Table();
        for (Map.Entry<String, List<Stats>> e : table.entrySet()) {
            result.add(e.getKey(), e.getValue());
        }
        return result;
    }

    public String getName() {
        return indexName;
    }

    public int getRun() {
        return run;
    }

    public long getCount(Phase phase, Type type) {
        return count[phase.ordinal()][type.ordinal()];
    }

    public double getElapsed(Phase phase, Type type) {
        return time[phase.ordinal()][type.ordinal()] * TIME_SCALE;
    }

    public double getLoad() {
        return load * TIME_SCALE;
    }

    public int getPopulation() {
        return population;
    }

    public void registerResult(final Phase phase, final Type type, final long elapsed, final long count) {
        this.time[phase.ordinal()][type.ordinal()] = elapsed;
        this.count[phase.ordinal()][type.ordinal()] = count;
    }

    public String displayString(final Phase phase, Type type) {
        return String.format("%s %s %s population %s run %s  exec time %f (%s)", indexName, phase, type, population,
                run, getElapsed(phase, type), getCount(phase, type));

    }

    public String loadDisplayString() {
        return String.format("%s population %s run %s load time %s", indexName, population, run, getLoad());
    }

    public static class Serde {

        public void writeStats(DataOutputStream out, Stats stats) throws IOException {
            out.writeUTF(stats.indexName);
            out.writeInt(stats.population);
            out.writeInt(stats.run);
            out.writeLong(stats.load);
            writeLongMatrix(out, stats.time);
            writeLongMatrix(out, stats.count);

        }

        private void writeLongMatrix(DataOutputStream out, long[][] matrix) throws IOException {
            out.writeInt(Phase.values().length);
            for (int p = 0; p < Phase.values().length; p++) {
                writeLongArray(out, matrix[p]);
            }
        }

        private void writeLongArray(DataOutputStream out, long[] arry) throws IOException {
            out.writeInt(arry.length);
            for (long l : arry) {
                out.writeLong(l);
            }
        }

        private File filterMapFile(File dir, String tableName) {
            return new File(dir, String.format("%s.fmf", tableName));
        }

        public void writeFilterMaps(File dir, String tableName, Stats stats) throws IOException {

            Map<FrozenBloomFilter, Set<FrozenBloomFilter>> map;
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(filterMapFile(dir, tableName), true))) {
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
        }

        private void writeBloomFilter(DataOutputStream out, BloomFilter bf) throws IOException {
            out.writeInt(bf.getShape().getNumberOfHashFunctions());
            out.writeInt(bf.getShape().getNumberOfBits());
            writeLongArray(out, BloomFilter.asBitMapArray(bf));
        }

        public Stats readStats(DataInputStream in) throws IOException {
            Stats result = new Stats(in.readUTF(), in.readInt(), in.readInt());
            result.load = in.readLong();
            result.time = readLongMatrix(in);
            result.count = readLongMatrix(in);
            return result;
        }

        private long[][] readLongMatrix(DataInputStream in) throws IOException {
            int p = in.readInt();
            long[][] matrix = new long[p][];
            for (int i = 0; i < p; i++) {
                matrix[i] = readLongArray(in);
            }
            return matrix;
        }

        private long[] readLongArray(DataInputStream in) throws IOException {
            int len = in.readInt();
            long[] arry = new long[len];
            for (int i = 0; i < len; i++) {
                arry[i] = in.readLong();
            }
            return arry;
        }

        public void readFilterMaps(File dir, String tableName, Stats stats) throws IOException {

            try (DataInputStream in = new DataInputStream(new FileInputStream(filterMapFile(dir, tableName)))) {
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
        }

        private FrozenBloomFilter readBloomFilter(DataInputStream in) throws IOException {
            Shape shape = new Shape(in.readInt(), in.readInt());
            long[] bitMaps = readLongArray(in);
            BitMapProducer producer = BitMapProducer.fromLongArray(bitMaps);
            BloomFilter bf = new SimpleBloomFilter(shape, producer);
            return FrozenBloomFilter.makeInstance(bf);
        }

    }

}
