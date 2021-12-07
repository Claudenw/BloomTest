package org.xenei.bloompaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            ;
        } else {
            this.dir = dir;
        }
        tableNames = new TreeSet<String>();
    }

    public void scanForFiles() {
        for (String f : dir.list((d, n) -> n.endsWith(".dat"))) {
            tableNames.add(f.substring(0, f.length() - 4));
        }
    }

    public void add(String tableName, List<Stats> tbl) throws FileNotFoundException, IOException {
        tableNames.add(tableName);
        Stats.Serde serde = new Stats.Serde();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(statsFile(tableName)))) {
            for (Stats stat : tbl) {
                serde.writeStats(out, stat);
            }
        }
    }

    private File statsFile(String tableName) {
        return new File(dir, String.format("%s.dat", tableName));
    }

    public void forEachStat(Consumer<Stats> consumer) throws IOException {
        Stats.Serde serde = new Stats.Serde();
        for (String tableName : tableNames) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(statsFile(tableName)))) {
                consumer.accept(serde.readStats(in));
            }
        }
    }

    public void forEachSimpleStat(Consumer<Stats> consumer) throws IOException {
        Stats.Serde serde = new Stats.Serde();
        for (String tableName : tableNames) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(statsFile(tableName)))) {
                consumer.accept(serde.readStats(in, true));
            }
        }
    }

    public void forEachPhase(BiConsumer<Stats, Stats.Phase> consumer) throws IOException {
        Consumer<Stats> c = s -> {
            for (Stats.Phase phase : Stats.Phase.values()) {
                consumer.accept(s, phase);
            }
            ;
        };
        forEachStat(c);
    }

}
