package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.apache.commons.collections4.bloomfilter.StandardBloomFilter;
import org.xenei.bloompaper.geoname.GeoName;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.BloomIndexBTree;
import org.xenei.bloompaper.index.BloomIndexBTreeNoStack;
import org.xenei.bloompaper.index.BloomIndexBloofi;
import org.xenei.bloompaper.index.BloomIndexBloofiR;
import org.xenei.bloompaper.index.BloomIndexHamming;
import org.xenei.bloompaper.index.BloomIndexLimitedBTree;
import org.xenei.bloompaper.index.BloomIndexLimitedHamming;
import org.xenei.bloompaper.index.BloomIndexLinear;
import org.xenei.bloompaper.index.BloomIndexLinearList;
import org.xenei.bloompaper.index.BloomIndexPartialBTree;

public class Density {

    // 9,772,346 max lines
    private static final int MAX_DENSITY=9;
    private static final int SAMPLE_SIZE=1000000;

    public static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "This help");
        options.addOption("o", "output", true, "Output directory.  If not specified results will not be preserved");
        return options;
    }

    public static void main(final String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Test", getOptions() );
        }




        File dir = null;
        if (cmd.hasOption("o")) {
            dir = new File(cmd.getOptionValue("o"));
            if (!dir.exists()) {
                dir.mkdirs();
            } else if (!dir.isDirectory()) {
                throw new IllegalArgumentException(dir.getAbsolutePath() + " is not a directory");
            }
        }
        BloomFilterConfiguration bloomFilterConfig = bloomFilterConfig = new BloomFilterConfiguration(3, 1.0 / 100000);
        BloomFilter[] filters = new BloomFilter[SAMPLE_SIZE];
        final URL inputFile = Density.class.getResource("/allCountries.txt");
        Status status = new Status(bloomFilterConfig);

        System.out.println("Reading test data");
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
        for (int density = 0;density<MAX_DENSITY;density++)
        {
            System.out.println( "Density "+(density+1));
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                final GeoName gn = GeoName.parse(br.readLine());
                if (density > 0)
                {
                    filters[i] = filters[i].merge(new StandardBloomFilter(GeoNameFilterFactory.create(gn), bloomFilterConfig ));
                }
                else
                {
                    filters[i] = new StandardBloomFilter(GeoNameFilterFactory.create(gn), bloomFilterConfig);
                }
            }
            status.record( density, filters );
        }

        br.close();


        PrintStream o = null;

        System.out.println("=== results ===");
        if (dir != null) {
            o = new PrintStream(new File(dir, "density.csv"));
        }
        System.out.println(status.getHeader());
        if (o != null) {
            o.println(status.getHeader());
        }
        for (int density = 0;density<MAX_DENSITY;density++)
        {

            System.out.println(status.getData(density));
            if (o != null) {
                o.println(status.getData(density));
            }
        }

        System.out.println("=== run complete ===");

        if ( o != null)
        {
            o.close();
        }
    }

    private static class Status {
        Map<Integer,int[]> counts;
        int dim;

        public Status( BloomFilterConfiguration config )
        {
            counts = new HashMap<Integer,int[]>();
            dim = config.getNumberOfBits();
        }

        public void record( int density, BloomFilter[] filters )
        {
            int[] c = new int[dim];
            for (BloomFilter f : filters)
            {
                c[f.getHammingWeight()-1]++;
            }
            counts.put( density,  c );
        }

        public String getHeader() {
            StringBuilder sb = new StringBuilder( "'Density'");
            for (int i=0;i<dim;i++)
            {
                sb.append( ",").append(i+1);
            }
            return sb.toString();
        }

        public String getData( int density )
        {
            StringBuilder sb = new StringBuilder( String.format( "'Density %s'", density+1));
            int[] c = counts.get(density);
            for (int i=0;i<dim;i++)
            {
                sb.append( ",").append( c[i]==0?" ":c[i]);
            }
            return sb.toString();
        }

    }
}
