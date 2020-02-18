package org.xenei.bloom.speedTest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class SplitSummary {

    public static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addRequiredOption("o", "output", true,
                "Output directory.  If not specified results will not be preserved");
        return options;
    }

    public static void main(String[] args) throws IOException {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SplitSummary", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SplitSummary", getOptions());
        }

        File dir = new File(cmd.getOptionValue("o"));
        if (!dir.exists()) {
            dir.mkdirs();
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
        }

        PrintStream load = new PrintStream(new File(dir, "load.csv"));
        PrintStream complete = new PrintStream(new File(dir, "complete.csv"));
        PrintStream name = new PrintStream(new File(dir, "name.csv"));
        PrintStream feature = new PrintStream(new File(dir, "feature.csv"));

        load.print("'type',100,1000,10000,100000,1000000");
        complete.print("'type',100,1000,10000,100000,1000000");
        name.print("'type',100,1000,10000,100000,1000000");
        feature.print("'type',100,1000,10000,100000,1000000");
        Reader in = new FileReader(new File(dir, "summary.csv"));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        String lastType = null;
        for (CSVRecord record : records) {
            String type = record.get(0);
            String elapsed = record.get(2);
            String completeC = record.get(3);
            String nameC = record.get(4);
            String featureC = record.get(5);
            if (!type.equals(lastType)) {
                load.println();
                load.print(String.format("'%s'", type));
                complete.println();
                complete.print(String.format("'%s'", type));
                name.println();
                name.print(String.format("'%s'", type));
                feature.println();
                feature.print(String.format("'%s'", type));
                lastType = type;
            }
            load.print(String.format(",%s", elapsed));
            complete.print(String.format(",%s", completeC));
            name.print(String.format(",%s", nameC));
            feature.print(String.format(",%s", featureC));
        }
        load.println();
        complete.println();
        name.println();
        feature.println();
        load.close();
        complete.close();
        name.close();
        feature.close();
    }

}
