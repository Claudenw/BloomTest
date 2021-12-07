package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
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
            add(stat);
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
                load += stat.getLoad();
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

    public Summary(Table aTable) throws IOException {

        Consumer<Stats> loader = new Consumer<Stats>() {
            Element el = null;

            @Override
            public void accept(Stats s) {
                if (el == null) {
                    el = new Element(s);
                    table.add(el);
                } else {
                    if (!el.add(s)) {
                        el = new Element(s);
                        table.add(el);
                    }
                }
            }

        };

        aTable.forEachSimpleStat(loader);
    }

    public List<Element> getTable() {
        return table;
    }

    public static void writeData(PrintStream ps, Table table) throws IOException {
        ps.println(Stats.getHeader());
        table.forEachPhase((s, p) -> ps.println(s.reportStats(p)));
    }

    public void writeSummary(PrintStream ps) {
        ps.println(Summary.getHeader());
        for (final Summary.Element e : getTable()) {
            for (Stats.Phase phase : Stats.Phase.values()) {
                ps.println(e.getReport(phase));
            }
        }

    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addOption("c", "csv", true, "The name of a csv file to read");
        options.addOption("d", "data", true, "The name of data directory to read");
        options.addOption("f", "full", false, "Include full statistics report");
        options.addOption("o", "output", true, "Output file.  If not specified results will not be preserved");
        return options;
    }

    /**
     * Read a datafile and print it out.
     * @param args the arguments.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Summary", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", "", getOptions(), "Options 'c' and 'd' may occure more than once");
        }

        if (cmd.hasOption("c")) {
            for (String fn : cmd.getOptionValues("c")) {
                File f = new File(fn);
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    Table table = Stats.parse(br);
                    doOutput(table, cmd.getOptionValue("o"), cmd.hasOption("f"));
                } catch (IOException e) {
                    System.err.println(String.format("Error reading %s: %s", fn, e.getMessage()));
                }
            }

        }
        if (cmd.hasOption("d")) {
            for (String dirs : cmd.getOptionValues("d")) {
                File d = new File(dirs);
                Table table = new Table(d);
                table.scanForFiles();
                doOutput(table, cmd.getOptionValue("o"), cmd.hasOption("f"));
            }

        }
    }

    public static void doOutput(Table table, String fileName, boolean full) throws IOException {

        if (full) {
            System.out.println("===  data ===");
            Summary.writeData(System.out, table);
        }
        Summary summary = new Summary(table);

        System.out.println("=== summary data ===");
        summary.writeSummary(System.out);

        if (fileName != null) {
            File outFile = new File(fileName);
            summary.writeSummary(new PrintStream(new FileOutputStream(outFile)));
        }

    }
}
