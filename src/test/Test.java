package test;

import geoname.GeoName;
import hamming.ByteInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import normalfilter.NormalBloomFilter;
import normalfilter.NormalBloomFilterFactory;

public class Test {
	// 9,772,346 max lines
	private static int RUN_COUNT = 5;
	private static int[] RUNSIZE = {
		/*100, 1000, 10000,*/ 100000, /*1000000*/
	};
	private static int MAX_RUNSIZE = 1000000;

	public static void main(final String[] args) throws Exception {
		//final Class<?> factoryClass = Class.forName(args[0]);
		final NormalBloomFilterFactory factory = new NormalBloomFilterFactory();
		final List<Constructor<? extends BloomIndex>> constructors = new ArrayList<Constructor<? extends BloomIndex>>();
		final List<Stats> table = new ArrayList<Stats>();
		BloomFilter[] filters;
		final URL inputFile = Test.class.getResource("allCountries.txt");
		final List<GeoName> sample = new ArrayList<GeoName>(1000); // (1e3)

		// create the index constructors
		constructors.add(BloomIndexHamming.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexLimitedHamming.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexBTree.class.getConstructor(int.class,int.class));
		//constructors.add(BloomIndexPartialBTree.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexBloofi.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexLimitedBTree.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexLinear.class.getConstructor(int.class,int.class));

		// read the test data.
		
//		final BufferedReader br = new BufferedReader(new InputStreamReader(
//				inputFile.openStream()));
//		for (int i = 0; i < 1000000; i++) {
//			final GeoName gn = GeoName.parse(br.readLine());
//			if ((i % 1000) == 0) {
//				sample.add(gn);
//			}
//			filters[i] = factory.create(gn);
//		}

		for (int density=1; density < 10; density++)
		{
			filters = createFilterArray( factory, sample, inputFile, density ); 
			for (int runsize : RUNSIZE )
			{
				for (final Constructor<? extends BloomIndex> constructor : constructors) {
					final Stats[] stats = new Stats[RUN_COUNT];
					for (int j = 0; j < RUN_COUNT; j++) {
						stats[j] = new Stats(density, runsize);
					}
					table.addAll(runTest(factory, constructor, sample, filters, runsize,
							stats));
				}
			}
		}

		System.out.println("===  data ===");
		System.out.println( Stats.header() );
		for (final Stats s : table) {
			System.out.println(s.toString());
		}
		System.out.println("=== summary data ===");
		final Summary summary = new Summary(table);
		System.out.println( Summary.header() );
		for (final Summary.Element e : summary.getTable()) {
			System.out.println(e.toString());
		}

	}

	private static List<Stats> runTest(final BloomFilterFactory factory,
			final Constructor<? extends BloomIndex> constructor,
			final List<GeoName> sample, final BloomFilter[] filters,
			final int limit, final Stats[] stats) throws IOException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		final BloomIndex bi = doLoad(constructor, filters, limit, stats);
		final int sampleSize = sample.size();
		
		BloomFilter[] bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {
			bfSample[i] = factory.create(sample.get(i));
		}
		doCount(1, bi, bfSample, limit, stats);

		bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {
			bfSample[i] = factory.create(sample.get(i).name);
		}
		doCount(2, bi, bfSample, limit, stats);

		bfSample = new BloomFilter[sampleSize];
		for (int i = 0; i < sample.size(); i++) {
			bfSample[i] = factory.create(sample.get(i).feature_code);
		}
		doCount(3, bi, bfSample, limit, stats);
		return Arrays.asList(stats);
	}

	private static BloomIndex doLoad(
			final Constructor<? extends BloomIndex> constructor,
			final BloomFilter[] filters, final int limit, final Stats[] stats)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		BloomIndex bi = null;

		for (int j = 0; j < RUN_COUNT; j++) {
			bi = constructor.newInstance(limit, filters[0].getWidth());
			stats[j].type = bi.getName();
			final long start = System.currentTimeMillis();
			for (int i = 0; i < limit; i++) {
				bi.add(filters[i]);
			}
			final long end = System.currentTimeMillis();
			stats[j].load = end - start;
			System.out.println(String.format(
					"%s d:%s 0 limit %s run %s  load time %s", bi.getName(), stats[0].density, limit,
					j, end - start));
		}
		return bi;
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
					"%s D:%s %s limit %s run %s  count time %s (%s)", bi.getName(), stats[0].density,
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
				stat.highCard = total;
				stat.hcFound = found;
				break;
			case 3:
				stat.lowCard = total;
				stat.lcFound = found;
				break;
			default:
				throw new IllegalArgumentException(String.format(
						"%s is not a valid position", pos));
		}

	}
	
	public static BloomFilter[] createFilterArray( NormalBloomFilterFactory factory, List<GeoName> sample, URL inputFile, int density ) throws IOException
	{
		boolean loadSample = sample.isEmpty();
		BloomFilter[] result = new BloomFilter[MAX_RUNSIZE];
		NormalBloomFilter.Builder builder = factory.getBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					inputFile.openStream()));
			int geoCount = 0;
			for (int i = 0; i < MAX_RUNSIZE; i++) {
				for (int j=0;j<density;j++)
				{
					final GeoName gn = GeoName.parse(br.readLine());
					geoCount++;
					builder.add(gn);
					if (loadSample && sample.size()<1000 && (geoCount % 1000)==0)
					{
						sample.add( gn );
					}
				}
				result[i]=builder.build();
			}
			return result;
		} finally {
			try {
				br.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}
}
