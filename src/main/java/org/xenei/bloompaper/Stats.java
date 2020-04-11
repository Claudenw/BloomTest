package org.xenei.bloompaper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.NumericBloomFilter;

/**
 * A record of the data from a single run.
 */
public class Stats {
    public enum Type { COMPLETE, HIGHCARD, LOWCARD }
    public enum Phase {Query, Delete}

    private final String indexName;
    private final int population;
    private final int run;

    long load;

    Phase currentPhase;
    Type currentType;

    Map<NumericBloomFilter,Set<NumericBloomFilter>>[] foundFilters = new HashMap[ Type.values().length ];

    private long[][] time = new long[ Phase.values().length][ Type.values().length];
    private long[][] count = new long[ Phase.values().length][ Type.values().length];

    public static String getHeader() {
        StringBuilder sb = new StringBuilder( "'Index Name', 'Phase', 'Population', 'Load Elapsed'");
        for (Type type : Type.values())
        {
            sb.append( String.format( ", '%1$s Elapsed', '%1$s Count'", type));
        }
        return sb.toString();
    }

    public void addFoundFilters( Type type, BloomFilter filter, Collection<NumericBloomFilter> filters) {
        if (filters != null) {
            if (foundFilters[type.ordinal()] == null)
            {
                foundFilters[type.ordinal()] = new HashMap<NumericBloomFilter, Set<NumericBloomFilter>>();
            }
            Set<NumericBloomFilter> set = new HashSet<NumericBloomFilter>();
            set.addAll( filters );
            foundFilters[type.ordinal()].put(NumericBloomFilter.makeInstance(filter), set );
        }
    }

    public Map<NumericBloomFilter,Set<NumericBloomFilter>> getFound( Type type )
    {
        return foundFilters[type.ordinal()];
    }

    public Stats( String indexName, int population, int run)
    {
        this.indexName = indexName;
        this.population = population;
        this.run = run;
    }

    public String reportStats( Phase phase ) {
        StringBuilder sb = new StringBuilder( String.format( "'%s','%s',%s,%s", indexName, phase, population, load) );
        for (Type type : Type.values())
        {
            sb.append( String.format( ",%s,%s", getElapsed(phase, type ), getCount(phase, type)));
        }
        return sb.toString();
    }

    public String getName() {
        return indexName;
    }

    public int getRun() {
        return run;
    }

    public long getCount( Phase phase, Type type) {
        return count[phase.ordinal()][type.ordinal()];
    }

    public long getElapsed( Phase phase, Type type) {
        return time[phase.ordinal()][type.ordinal()];
    }

    public int getPopulation() {
        return population;
    }

    public void registerResult(final Phase phase, final Type type, final long elapsed, final long count) {
        time[phase.ordinal()][type.ordinal()] = elapsed;
        this.count[phase.ordinal()][type.ordinal()] = count;
    }

    public String displayString( final Phase phase, Type type ) {
        return String.format("%s %s %s population %s run %s  exec time %s (%s)",
                indexName, phase, type, population, run, time[phase.ordinal()][type.ordinal()],
                count[phase.ordinal()][type.ordinal()]);

    }

    public String loadDisplayString() {
        return String.format("%s population %s run %s load time %s",
                indexName, population, run, load );
    }

}
