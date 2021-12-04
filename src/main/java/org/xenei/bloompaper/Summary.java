package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
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
        double[][] totals = new double[Stats.Phase.values().length][Stats.Type.values().length];

        public Element(Stats stat) {
            this.indexName = stat.getName();
            this.population = stat.getPopulation();
            this.n = 0;
        }

        public double total(Stats.Phase phase, Stats.Type type) {
            return totals[phase.ordinal()][type.ordinal()];
        }

        public String getReport(Stats.Phase phase) {
            return String.format("'%s','%s', %s,%s,%s,%s,%s", indexName, phase, population, load / n,
                    total(phase, Stats.Type.COMPLETE) / n, total(phase, Stats.Type.HIGHCARD) / n,
                    total(phase, Stats.Type.LOWCARD) / n);
        }

        public boolean add(Stats stat) {
            if (stat.getName().equals(indexName) && stat.getPopulation() == population) {
                load += stat.load;
                for (Stats.Phase phase : Stats.Phase.values()) {
                    for (Stats.Type type : Stats.Type.values()) {
                        totals[phase.ordinal()][type.ordinal()] += stat.getElapsed(phase, type);
                    }
                }
                n++;
                return true;
            }
            return false;
        }
    }

    public static String getHeader() {
        StringBuilder sb = new StringBuilder("'Index Name', 'Phase', 'Population', 'Avg Load Elapsed'");
        for (Type type : Type.values()) {
            sb.append(String.format(", 'Avg %1$s Elapsed'", type));
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

    public static void writeData(PrintStream ps, List<Stats> table) {
        ps.println(Stats.getHeader());
        for (final Stats s : table) {
            for (Stats.Phase phase : Stats.Phase.values()) {
                ps.println(s.reportStats(phase));
            }
        }
    }

    public void writeSummary(PrintStream ps) {
        ps.println(Summary.getHeader());
        for (final Summary.Element e : getTable()) {
            for (Stats.Phase phase : Stats.Phase.values()) {
                ps.println(e.getReport(phase));
            }
        }

    }

    /**
     * Read a datafile and print it out.
     * @param args the arguments.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args.length > 2) {
            System.out.println("Summary <inputFile> [<outputFile>]");
            System.exit(1);
            ;
        }

        File f = new File(args[0]);
        BufferedReader br = new BufferedReader(new FileReader(f));

        List<Stats> table = Stats.parse(br);

        Summary summary = new Summary(table);
        summary.writeSummary(System.out);
        if (args.length == 2) {
            File outFile = new File(args[1]);
            summary.writeSummary(new PrintStream(new FileOutputStream(outFile)));
        }
    }
}
