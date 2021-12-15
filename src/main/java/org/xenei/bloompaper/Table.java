package org.xenei.bloompaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Table {

    private File dir;
    private Set<String> tableNames;

    public Table() throws IOException {
        this(null);
    }

    public Table(File dir) throws IOException {
        if (dir == null) {
            this.dir = Files.createTempDirectory(String.format("BloomTest-%s", System.currentTimeMillis())).toFile();
        } else {
            this.dir = dir;
        }
        tableNames = new TreeSet<String>();
    }

    public File getDir() {
        return dir;
    }

    public void scanForFiles() {
        for (String f : dir.list((d, n) -> n.endsWith(".dat"))) {
            tableNames.add(f.substring(0, f.length() - 4));
        }
    }

    public void reset() {
        scanForFiles();
        tableNames.forEach( (n) -> {new File( dir, n+".dat" ).delete();new File( dir, n+".fmf" ).delete(); }  );
        tableNames.clear();
    }

    public void add(String tableName, List<Stats> tbl) throws FileNotFoundException, IOException {
        tableNames.add(tableName);
        Stats.Serde serde = new Stats.Serde();
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(statsFile(tableName), true))) {
            for (Stats stat : tbl) {
                serde.writeStats(out, stat);
                serde.writeFilterMaps( dir, tableName, stat);
            }
        }
    }

    private File statsFile(String tableName) {
        return new File(dir, String.format("%s.dat", tableName));
    }

    public void forEachStat(Consumer<Stats> consumer) throws IOException {
        Stats.Serde serde = new Stats.Serde();
        for (String tableName : tableNames) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(statsFile(tableName)))) {
                while(in.available() > 0) {
                    consumer.accept(serde.readStats(in));
                }
            }
        }
    }

    public void forEachPhase(BiConsumer<Stats, Stats.Phase> consumer) throws IOException {
        Consumer<Stats> c = s -> {
            for (Stats.Phase phase : Stats.Phase.values()) {
                consumer.accept(s, phase);
            }
        };
        forEachStat(c);
    }

}
