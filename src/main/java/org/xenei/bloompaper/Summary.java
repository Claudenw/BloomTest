package org.xenei.bloompaper;

import java.util.ArrayList;
import java.util.List;

import org.xenei.bloompaper.Stats.Type;

/**
 * A summary of statistics.
 *
 */
public class Summary {

    /**
     * A summary element
     */
    public class Element {
        int n;
        private final String indexName;
        int population;
        double load;
        double [][] totals = new double[Stats.Phase.values().length][Stats.Type.values().length];

        public Element(Stats stat) {
            this.indexName = stat.getName();
            this.population = stat.getPopulation();
            this.n = 0;
        }

        public double total( Stats.Phase phase, Stats.Type type )
        {
            return totals[phase.ordinal()][type.ordinal()];
        }

        public String getReport( Stats.Phase phase ) {
            return String.format("'%s','%s', %s,%s,%s,%s,%s", indexName, phase, population, load / n,
                    total(phase, Stats.Type.COMPLETE) / n,
                    total(phase, Stats.Type.HIGHCARD) / n,
                    total(phase, Stats.Type.LOWCARD) / n );
        }

        public boolean add(Stats stat) {
            if (stat.getName().equals(indexName) && stat.getPopulation() == population) {
                load += stat.load;
                for (Stats.Phase phase : Stats.Phase.values() )
                {
                    for (Stats.Type type : Stats.Type.values())
                    {
                        totals[phase.ordinal()][type.ordinal()] += stat.getCount( phase, type);
                    }
                }
                n++;
                return true;
            }
            return false;
        }
    }

    public static String getHeader() {
        StringBuilder sb = new StringBuilder( "'Index Name', 'Phase', 'Population', 'Avg Load Elapsed'" );
        for (Type type : Type.values())
        {
            sb.append( String.format( ", 'Avg %1$s Elapsed'", type));
        }
        return sb.toString();
    }

    private List<Element> table = new ArrayList<Element>();

    public Summary(List<Stats> table) {
        Element el = new Element(table.get(0));
        this.table.add(el);

        for (Stats stat : table) {
            if (!el.add(stat)) {
                el = new Element(stat);
                this.table.add(el);
                el.add(stat);
            }
        }
    }

    public List<Element> getTable() {
        return table;
    }
}
