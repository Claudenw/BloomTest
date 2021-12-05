package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
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

    private static final double TIME_SCALE = 0.000000001;
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
        return foundFilters[type.ordinal()];
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

    public static List<Stats> parse(BufferedReader reader) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withQuote('\'');

        String line = reader.readLine();
        if (!Stats.getHeader().equals(line)) {
            String s = String.format("Wrong header.  Wrong version? Expected:\n%s\nRead:\n%s", Stats.getHeader(), line);
            throw new IOException(s);
        }

        List<Stats> table = new ArrayList<Stats>();
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
            table.add(stat);
        }
        return table;
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

}
