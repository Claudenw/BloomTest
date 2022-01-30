package org.xenei.bloompaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.xenei.bloompaper.Stats.Phase;

/**
 * A collection of Stats that represents a complete test run.
 *
 */
public class Table {

    /**
     * The directory that data files are stored in.
     */
    private final File dir;
    /**
     * The table names.
     */
    private Set<String> tableNames;

    /**
     * Constructor for a table with no directory.
     * @throws IOException
     */
    public Table() throws IOException {
        this(null);
    }

    public class CSV implements BiConsumer<Stats,Stats.Phase>{

        private PrintStream stream;
        private boolean headerPrinted = false;

        public CSV( PrintStream stream ) {
            this.stream = stream;
        }


        @Override
        public void accept(Stats stats, Phase phase) {
            Stats.CSV csv = stats.new CSV(stream);
            if (!headerPrinted) {
                csv.printHeader();
                headerPrinted = true;
            }
            try {
                stats.loadFilterMaps(getDir());
            } catch (IOException e) {
                stream.println("Unable to load filter maps: " + e.getMessage());
            }
            csv.printLine(phase);
        }
    }

    /**
     * Constructor.
     * If the dir parameter is null a temporary directory in the system defined tmp space is created and used.
     * @param dir the directory that the data tables exist in. May be null.
     * @throws IOException on IO error.
     */
    public Table(File dir) throws IOException {
        if (dir == null) {
            this.dir = Files.createTempDirectory(String.format("BloomTest-%s", System.currentTimeMillis())).toFile();
        } else {
            this.dir = dir;
        }
        tableNames = new TreeSet<String>();
    }

    /**
     * Gets the directory where the files are stored.
     * @return the directory where the files are stored.
     */
    public File getDir() {
        return dir;
    }

    /**
     * Scans the directory for data files.
     * This method scans the data directory for .dat files and attempts to read them.
     */
    public void scanForFiles() {
        for (String f : dir.list((d, n) -> n.endsWith(".dat"))) {
            tableNames.add(f.substring(0, f.length() - 4));
        }
    }

    /**
     * Scans the directory for the data files and then deletes them.
     * This includes the .dat and the .fmf files.
     */
    public void reset() {
        scanForFiles();
        tableNames.forEach((n) -> {
            new File(dir, n + ".dat").delete();
            new File(dir, n + ".fmf").delete();
        });
        tableNames.clear();
    }

    /**
     * Add a table name to the table.
     * @param tableName the table name.
     * @param tbl the list of stats for the table.
     * @throws IOException on error.
     */
    public void add(String tableName, List<Stats> tbl) throws IOException {
        System.out.println("Saving run data");
        tableNames.add(tableName);
        Stats.Serde serde = new Stats.Serde();
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(statsFile(tableName), true));
                DataOutputStream filters = new DataOutputStream(new FileOutputStream(filterMapFile(tableName), true))) {
            for (Stats stat : tbl) {
                System.out.format("writing %s run %s stats ", stat.getPopulation(), stat.getRun());
                serde.writeStats(out, stat);
                System.out.format("and filter maps%n", stat.getPopulation(), stat.getRun());
                serde.writeFilterMaps(filters, stat);
            }
        }
    }

    /**
     * Gets the stats file for the table name.
     * @param tableName the table name to locate the stats file for.
     * @return the File in the system directory.
     */
    private File statsFile(String tableName) {
        return new File(dir, String.format("%s.dat", tableName));
    }

    /**
     * Gets the filter map file for the table.
     * @param dir the directory for the file.
     * @param tableName the table name.
     * @return the fmf file name in the specified directory.
     */
    public static File filterMapFile(File dir, String tableName) {
        return new File(dir, String.format("%s.fmf", tableName));
    }

    /**
     * Gets the filter map file for the table.
     * @param tableName the table name
     * @return the .fmf file in the system directory.
     */
    private File filterMapFile(String tableName) {
        return filterMapFile(dir, tableName);
    }

    /**
     * Executes a function against each stat file in turn.
     * @param consumer the consumer function to execute.
     * @throws IOException on IO error.
     */
    public void forEachStat(Consumer<Stats> consumer) throws IOException {
        Stats.Serde serde = new Stats.Serde();
        for (String tableName : tableNames) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(statsFile(tableName)))) {
                while (in.available() > 0) {
                    consumer.accept(serde.readStats(in));
                }
            }
        }
    }

    /**
     * Executes a function for each phase in each stat in turn.
     * @param consumer the BiConsumer for the stats and the phase.
     * @throws IOException on IO error.
     */
    public void forEachPhase(BiConsumer<Stats, Stats.Phase> consumer) throws IOException {
        Consumer<Stats> c = s -> {
            for (Stats.Phase phase : Stats.Phase.values()) {
                consumer.accept(s, phase);
            }
        };
        forEachStat(c);
    }

}
