package org.xenei.bloompaper;

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
    private long[][] time = new long[ Phase.values().length][ Type.values().length];
    private long[][] count = new long[ Phase.values().length][ Type.values().length];


    public static String getHeader() {
        return "'Type', 'Population', 'Load Elapsed', 'Complete Elapsed', 'Complete Found', 'Name Elapsed', 'Name Found', 'Feature Elapsed', 'Feature Found', 'Delete Elapsed',  'Delete Count'";
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
