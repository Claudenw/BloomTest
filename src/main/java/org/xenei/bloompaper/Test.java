package org.xenei.bloompaper;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
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
import org.xenei.bloompaper.index.BloomIndexLinear;
import org.xenei.bloompaper.index.BloomIndexPartialBTree;

public class Test {

    public static Map<String,Constructor<? extends BloomIndex>> constructors = new HashMap<String,Constructor<? extends BloomIndex>>();


    private static void init() throws NoSuchMethodException, SecurityException {
        constructors.put( "Hamming", BloomIndexHamming.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "Bloofi", BloomIndexBloofi.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "BloofiR", BloomIndexBloofiR.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "BtreeNoStack", BloomIndexBTreeNoStack.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "Btree", BloomIndexBTree.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "PartialBtree", BloomIndexPartialBTree.class.getConstructor(int.class,BloomFilterConfiguration.class));
        constructors.put( "LimitedBTree", BloomIndexLimitedBTree.class.getConstructor(int.class, BloomFilterConfiguration.class));
        constructors.put( "Linear", BloomIndexLinear.class.getConstructor(int.class,BloomFilterConfiguration.class));
    }

    public static Options getOptions() {
        StringBuffer sb = new StringBuffer("List of tests to run.  Valid test names are: ");
        sb.append( String.join(", ", constructors.keySet()) );

        Options options = new Options();
        options.addRequiredOption( "r", "run", true, sb.toString());
        options.addOption( "d", "dense", false, "Use compact filters");
        options.addOption( "h", "help", false, "This help");
        return options;
    }

	// 9,772,346 max lines
	private static int RUN_COUNT = 5;

	private static int[] RUNSIZE = {
		100, 1000, 10000, 100000, 1000000
	};

	public static void main(final String[] args) throws Exception {
	    init();
	    CommandLineParser parser = new DefaultParser();
	    CommandLine cmd = null;
	    try {
	     cmd = parser.parse( getOptions(), args);
	    }
	    catch(Exception e)
	    {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Test", "", getOptions(), e.getMessage() );
            System.exit(1);
	    }

	    if (cmd.hasOption("h"))
	    {
	        StringBuffer sb = new StringBuffer("Valid test names are: ");
	        sb.append( String.join(", ", constructors.keySet()) );
	        HelpFormatter formatter = new HelpFormatter();
	        formatter.printHelp( "Test", "", getOptions(), sb.toString() );
	    }

	    BloomFilterConfiguration bloomFilterConfig;
		if (cmd.hasOption("d"))
		{
		    bloomFilterConfig = new BloomFilterConfiguration( 1, 11, 8 );
		} else {
		   // 3 items 1/100,000
		    bloomFilterConfig = new BloomFilterConfiguration( 3, 1.0/100000 );
		}

		final List<Constructor<? extends BloomIndex>> tests = new ArrayList<Constructor<? extends BloomIndex>>();
		final List<Stats> table = new ArrayList<Stats>();
		final BloomFilter[] filters = new BloomFilter[1000000]; // (1e6)
		final URL inputFile = Test.class.getResource("/allCountries.txt");
		final List<GeoName> sample = new ArrayList<GeoName>(1000); // (1e3)

		for (String s : cmd.getOptionValues("r"))
		{
		    tests.add( constructors.get(s));
		}

		// read the test data.
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				inputFile.openStream()));
		for (int i = 0; i < 1000000; i++) {
			final GeoName gn = GeoName.parse(br.readLine());
			if ((i % 1000) == 0) {
				sample.add(gn);
			}
			filters[i] = new StandardBloomFilter( GeoNameFilterFactory.create(gn),bloomFilterConfig);
		}

		for (final Constructor<? extends BloomIndex> constructor : tests) {
			for (final int i : RUNSIZE) {
				final Stats[] stats = new Stats[RUN_COUNT];
				for (int j = 0; j < RUN_COUNT; j++) {
					stats[j] = new Stats(i);
				}
				table.addAll(runTest(bloomFilterConfig, constructor, sample, filters, i,
						stats));
			}
		}

		System.out.println("===  data ===");
		for (final Stats s : table) {
			System.out.println(s.toString());
		}
		System.out.println("=== summary data ===");
		final Summary summary = new Summary(table);

		for (final Summary.Element e : summary.getTable()) {
			System.out.println(e.toString());
		}

	}

	private static List<Stats> runTest(final BloomFilterConfiguration bloomFilterConfig,
			final Constructor<? extends BloomIndex> constructor,
			final List<GeoName> sample, final BloomFilter[] filters,
			final int limit, final Stats[] stats) throws IOException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		final BloomIndex bi = doLoad(constructor, filters, bloomFilterConfig,limit, stats);
		final int sampleSize = sample.size();
		BloomFilter[] bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {
		    bfSample[i] = new StandardBloomFilter( GeoNameFilterFactory.create(sample.get(i)),bloomFilterConfig);
		}
		doCount(1, bi, bfSample, limit, stats);

		bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {

		    bfSample[i] = new StandardBloomFilter( GeoNameFilterFactory.create(sample.get(i).name), bloomFilterConfig );
		}
		doCount(2, bi, bfSample, limit, stats);

		bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {
            bfSample[i] = new StandardBloomFilter( GeoNameFilterFactory.create(sample.get(i).feature_code), bloomFilterConfig );
		}
		doCount(3, bi, bfSample, limit, stats);
		return Arrays.asList(stats);
	}

	private static BloomIndex doLoad(
			final Constructor<? extends BloomIndex> constructor,
			final BloomFilter[] filters, final BloomFilterConfiguration bloomFilterConfig,final int limit, final Stats[] stats)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		BloomIndex bi = null;

		for (int j = 0; j < RUN_COUNT; j++) {
			bi = constructor.newInstance(limit, bloomFilterConfig);
			stats[j].type = bi.getName();
			final long start = System.currentTimeMillis();
			for (int i = 0; i < limit; i++) {
				bi.add(filters[i]);
			}
			final long end = System.currentTimeMillis();
			stats[j].load = end - start;
			System.out.println(String.format(
					"%s 0 limit %s run %s  load time %s", bi.getName(), limit,
					j, end - start));
		}
		return bi;
	}

	private static void doSearch(final int pos, final BloomIndex bi,
			final BloomFilter[] bfSample, final int limit, final Stats[] stats) {
		final int sampleSize = bfSample.length;
		for (int j = 0; j < RUN_COUNT; j++) {
			long total = 0;
			long start = 0;
			long found = 0;
			List<BloomFilter> result;
			for (int i = 0; i < sampleSize; i++) {
				start = System.currentTimeMillis();
				result = bi.get(bfSample[i]);
				total += (System.currentTimeMillis() - start);
				found += result.size();
			}
			registerResult(stats[j], pos, total, found);

			System.out.println(String.format(
					"%s %s limit %s run %s  search time %s (%s)", bi.getName(),
					pos, limit, j, total, found));
		}

	}

	private static void doCount(final int pos, final BloomIndex bi,
			final BloomFilter[] bfSample, final int limit, final Stats[] stats) {
		final int sampleSize = bfSample.length;
		for (int j = 0; j < RUN_COUNT; j++) {
			long total = 0;
			long start = 0;
			long found = 0;

			for (int i = 0; i < sampleSize; i++) {
				start = System.currentTimeMillis();
				found += bi.count(bfSample[i]);
				total += (System.currentTimeMillis() - start);
			}
			registerResult(stats[j], pos, total, found);

			System.out.println(String.format(
					"%s %s limit %s run %s  count time %s (%s)", bi.getName(),
					pos, limit, j, total, found));
		}

	}

	private static void registerResult(final Stats stat, final int pos,
			final long total, final long found) {
		switch (pos) {
			case 1:
				stat.complete = total;
				stat.completeFound = found;
				break;
			case 2:
				stat.name = total;
				stat.nameFound = found;
				break;
			case 3:
				stat.feature = total;
				stat.featureFound = found;
				break;
			default:
				throw new IllegalArgumentException(String.format(
						"%s is not a valid position", pos));
		}

	}
}
