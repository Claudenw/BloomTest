package org.xenei.bloompaper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
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
            formatter.printHelp("Saturation", "", getOptions(), e.getMessage());
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Saturation", getOptions() );
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
        BloomFilterConfiguration bloomFilterConfig = new BloomFilterConfiguration(3, 1.0 / 100000);
        BloomFilter[] filters = new BloomFilter[SAMPLE_SIZE];
        final URL inputFile = Density.class.getResource("/allCountries.txt");
        Status status = new Status(bloomFilterConfig);

        System.out.println("Reading test data");
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
        for (int density = 0;density<MAX_DENSITY;density++)
        {
            System.out.println( "Saturation "+(density+1));
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
            double effectiveP = new BloomFilterConfiguration( bloomFilterConfig.getNumberOfItems()*(density+1), bloomFilterConfig.getNumberOfBits(), bloomFilterConfig.getNumberOfHashFunctions() ).getProbability();
            status.record( density, filters, effectiveP );
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

    private static class SD {
        double sd;
        long mean;
        double p;

        @Override
        public String toString() {
            return String.format( "%s,%s,%s", mean, sd,p);
        }
    }
    private static class Status {
        Map<Integer,int[]> counts;
        int dim;
        Map<Integer,SD> sds;

        public Status( BloomFilterConfiguration config )
        {
            counts = new HashMap<Integer,int[]>();
            dim = config.getNumberOfBits();
            sds = new HashMap<Integer,SD>();
        }

        public void record( int density, BloomFilter[] filters, double probability )
        {
            SD sd = new SD();

            int[] c = new int[dim];
            for (BloomFilter f : filters)
            {
                sd.mean += f.getHammingWeight();
                c[f.getHammingWeight()-1]++;
            }
            counts.put( density,  c );

            // calculate standard defiation
            sd.mean /= filters.length;
            long sum=0;
            for (int i=0;i<dim;i++) {
                if (c[i]>0)
                {
                    sum += c[i]*Math.pow(i+1-sd.mean,2);
                }
            }
            sd.sd = Math.sqrt( sum/(filters.length-1) );
            sd.p = probability;
            sds.put( density,  sd );
        }

        public String getHeader() {
            StringBuilder sb = new StringBuilder( "'Saturation'");
            for (int i=0;i<dim;i++)
            {
                sb.append( ",").append(i+1);
            }
            sb.append(",,Mean,Standard Deviation,p");
            return sb.toString();
        }

        public String getData( int density )
        {
            StringBuilder sb = new StringBuilder( String.format( "'Sat. %s'", density+1));
            int[] c = counts.get(density);
            for (int i=0;i<dim;i++)
            {
                sb.append( ",").append( c[i]==0?" ":c[i]);
            }
            sb.append(",,").append( sds.get(density));
            return sb.toString();
        }


    }
}
