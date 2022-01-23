package org.xenei.bloompaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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

    /**
     * Parse a table from a CSV file.
     * @param reader the reader on the CSV file.
     * @return the Table.
     * @throws IOException on IO error
     */
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

            Stats stat = new Stats(rec.get(1), rec.get(0), Integer.parseInt(rec.get(4)), Integer.parseInt(rec.get(2)));
            stat.setLoad(Integer.parseInt(rec.get(5)));
            for (Stats.Phase phase : Stats.Phase.values()) {
                if (phase != Stats.Phase.valueOf(rec.get(3))) {
                    throw new IOException(
                            String.format("Wrong phase for %s.  Expected: %s Found: %s", stat, phase, rec.get(3)));
                }
                for (Stats.Type type : Stats.Type.values()) {
                    int idx = 6 + (type.ordinal() * 2);

                    stat.registerResult(phase, type, Long.parseLong(rec.get(idx)), Long.parseLong(rec.get(idx + 1)));
                }
                if (phase.ordinal() + 1 < Stats.Phase.values().length) {
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

}
