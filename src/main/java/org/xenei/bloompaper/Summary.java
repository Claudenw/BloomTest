package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.xenei.bloompaper.Stats.Phase;
import org.xenei.bloompaper.Stats.Type;

/**
 * A summary of statistics.
 * This class can read multiple .dat and .fmf files to create complete data.csv and
 * summary.csv files.
 *
 */
public class Summary {

    /**
     * A summary element
     */
    public class Element {
        /**
         *  The number of stats that have been added to this element.
         */
        int n;
        /**
         * The index name that generated the stats
         */
        private final String indexName;
        /**
         * the population for the stats.
         */
        int population;
        /**
         * The load time in seconds.
         */
        double load;
        /**
         * The total for the phase and type combinations.
         */
        double[][] totals = new double[Stats.Phase.values().length][Stats.Type.values().length];

        /**
         * Create an element from a Stats object.
         * @param stat the Stats to start with.
         */
        public Element(Stats stat) {
            this.indexName = stat.getName();
            this.population = stat.getPopulation();
            this.n = 0;
            add(stat);
        }

        /**
         * Gets the total for the specified phase and type.
         * @param phase the phase to get the total for.
         * @param type the Bloom filter type to get the total for.
         * @return the total for the specified phase and type.
         */
        public double total(Stats.Phase phase, Stats.Type type) {
            return totals[phase.ordinal()][type.ordinal()];
        }

        /**
         * Report the totals for the specified phase.
         * @param phase the Phase to report.
         * @param the name of the usage pattern
         * @return a CSV formatted string of the totals.
         */
        public String getReport(Stats.Phase phase, String usage) {
            return String.format("'%s','%s', '%s', %s,%s,%s,%s,%s", indexName, usage, phase, population, load / n,
                    total(phase, Stats.Type.COMPLETE) / n, total(phase, Stats.Type.HIGHCARD) / n,
                    total(phase, Stats.Type.LOWCARD) / n);
        }

        public double getGraphLoadTime() {
            return load / n;
        }

        public double getGraphValue(Stats.Phase phase, Stats.Type type) {
            return total(phase, type) / n;
        }

        /**
         * Add a stats object to this summary.
         * If the stats has the same index name and population as this summary it is added,
         * otherwise it is ignored.
         * @param stat the stats object to add.
         * @return true if the stats was added, false otherwise.
         */
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

    /**
     * Get the header for the CSV report.
     * @return the header string for the CSV report.
     */
    public static String getHeader() {
        StringBuilder sb = new StringBuilder("'Index Name', 'Usage', 'Phase', 'Population', 'Avg Load Elapsed'");
        for (Type type : Type.values()) {
            sb.append(String.format(", 'Avg %1$s Elapsed'", type));
        }
        return sb.toString();
    }

    /**
     * The list of Elements that make up this summary.
     */
    private List<Element> table = new ArrayList<Element>();

    /**
     * The Summary of a table.
     * @param aTable the table to summarize.
     * @throws IOException on IO error.
     */
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

        aTable.forEachStat(loader);
    }

    /**
     * Gets the list of elements for this summary.
     * @return the list of elements for this summary.
     */
    public List<Element> getTable() {
        return table;
    }

    /**
     * Writes produces a summary report for the specified table.
     * @param ps the print stream to write the report to.
     * @param table the table to process.
     * @throws IOException on IO Error.
     */
    public static void writeData(PrintStream ps, Table table) throws IOException {
        ps.println(Stats.getHeader());
        table.forEachPhase((s, p) -> {
            try {
                s.loadFilterMaps(table.getDir());
            } catch (IOException e) {
                ps.println("Unable to load filter maps: " + e.getMessage());
            }
            ps.println(s.reportStats(p));
        });
    }

    /**
     * Write the summary report.
     * @param ps the print stream to write the report to.
     * @param usage the name of the usage pattern.
     */
    public void writeSummary(PrintStream ps, String usage) {
        ps.println(Summary.getHeader());
        for (final Summary.Element e : getTable()) {
            for (Stats.Phase phase : Stats.Phase.values()) {
                ps.println(e.getReport(phase, usage));
            }
        }

    }

    /**
     * Gets the options for the main code.
     * @return the Options object
     */
    public static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addOption("c", "csv", true, "The name of a csv file to read (optional, may be repeated)");
        options.addOption("d", "data", true, "The name of directory containing .dat files (optional, may be repeated)");
        options.addOption("f", "full", false, "Include full statistics report (data.csv)");
        options.addOption("o", "output", true,
                "Output directory in which to write output files.  If not specified results will not be preserved");
        options.addOption("v", "verify", false, "Run data verification");
        options.addOption("g", "graph", false, "Create a graph_x.csv files. Only applies if -o is also specified");
        options.addOption("u", "usage", true, "Specify usage pattern, if not set 'unknown' is used");
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
        String usagePattern = "unknown";
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

        if (cmd.hasOption('u')) {
            usagePattern = cmd.getOptionValue('u');
        }
        if (cmd.hasOption("c")) {
            for (String fn : cmd.getOptionValues("c")) {
                File f = new File(fn);
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    Table table = Table.parse(br);
                    doOutput(table, cmd.getOptionValue("o"), cmd.hasOption("f"), cmd.hasOption("g"), usagePattern);
                    if (cmd.hasOption("v")) {
                        doVerify(table, cmd.getOptionValue("o"));
                    }
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
                doOutput(table, cmd.getOptionValue("o"), cmd.hasOption("f"), cmd.hasOption("g"),usagePattern);
                if (cmd.hasOption("v")) {
                    try {
                        doVerify(table, cmd.getOptionValue("o"));
                    } catch (IOException e) {
                        System.err.println(String.format("Error executing verify: %s", e.getMessage()));
                    }
                }
            }

        }

    }

    /**
     * Verifies the table data.
     * Output goes to System.out
     * @param table the table to verify.
     * @throws IOException on IO Error
     */
    private static void doVerify(Table table, String directory) throws IOException {
        Verifier verifier = new Verifier(new PrintStream(new File(directory, "verification.txt")));
        verifier.verify(table);
    }

    /**
     * Output a table summary in CSV format to the directory.
     * @param table the table to summarize
     * @param directoryName the directory in which to write the summary
     * @param full if true the full data should also be written.
     * @param graph if true the graph tables should be written.
     * @param usage The name of the usage pattern.
     * @throws IOException on IO Error.
     */
    public static void doOutput(Table table, String directoryName, boolean full, boolean graph, String usage) throws IOException {

        File outfile = null;
        if (directoryName != null) {
            outfile = new File(directoryName);
        }

        if (full) {
            System.out.println("=== data ===");
            Summary.writeData(System.out, table);
            if (outfile != null) {
                Summary.writeData(new PrintStream(new FileOutputStream(new File(outfile, "data.csv"))), table);
            }
        }
        Summary summary = new Summary(table);

        System.out.println("=== summary data ===");
        summary.writeSummary(System.out, usage);

        if (outfile != null) {
            summary.writeSummary(new PrintStream(new FileOutputStream(new File(outfile, "summary.csv"))),
                    usage);
        }

        if (graph) {
            System.out.println("=== graph data ===");

            summary.writeGraph(outfile);
        }

    }

    private void writeGraph(File dir) throws FileNotFoundException {
        Set<String> names = new TreeSet<>();
        Set<Integer> populations = new TreeSet<>();
        table.forEach((e) -> {
            names.add(e.indexName);
            populations.add(e.population);
        });
        PrintStream outfile = null;
        for (Phase p : Phase.values()) {
            for (Type t : Type.values()) {
                System.out.format("%nGraph data for %s %s%n", p, t);
                {
                    StringBuilder sb = new StringBuilder("'name'");
                    populations.forEach(pop -> sb.append(",'10^").append(Double.valueOf(Math.log10(pop)).intValue()).append("'"));
                    System.out.println(sb);

                    if (dir != null) {
                        outfile = new PrintStream(
                                new FileOutputStream(new File(dir, String.format("%s_%s.csv", p, t))));
                        outfile.println(sb);
                    }
                }
                for (String name : names) {
                    StringBuilder sb = new StringBuilder(String.format("'%s'", name));
                    for (Integer population : populations) {
                        table.stream().filter(e -> e.population == population && e.indexName.equals(name))
                        .forEach(e -> {
                            sb.append(",").append(e.getGraphValue(p, t));
                        });
                    }
                    System.out.println(sb);
                    if (outfile != null) {
                        outfile.println(sb);
                    }
                }
            }
        }
    }
}
