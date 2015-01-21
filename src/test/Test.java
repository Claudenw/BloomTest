package test;

import geoname.GeoName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
		100 , 1000, 10000, 100000, 1000000
	};
	private static int MAX_RUNSIZE = 1000000;
	private static int MAX_DENSITY = 9;
	private static String eol = System.getProperty("line.separator");

	public static void main(final String[] args) throws Exception {
		final NormalBloomFilterFactory factory = new NormalBloomFilterFactory();
		final List<Constructor<? extends BloomIndex>> constructors = new ArrayList<Constructor<? extends BloomIndex>>();
		final List<Stats> table = new ArrayList<Stats>();
		BloomFilter[] filters;
		final URL inputFile = Test.class.getResource("allCountries.txt");
		final List<GeoName> sample = new ArrayList<GeoName>(1000); 

		// create the index constructors
		//constructors.add(BloomIndexHamming.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexLimitedHamming.class.getConstructor(int.class,int.class));
		//constructors.add(BloomIndexBTree.class.getConstructor(int.class,int.class));
		//constructors.add(BloomIndexPartialBTree.class.getConstructor(int.class,int.class));
		//constructors.add(BloomIndexBloofi.class.getConstructor(int.class,int.class));
		//constructors.add(BloomIndexLimitedBTree.class.getConstructor(int.class,int.class));
		constructors.add(BloomIndexLinear.class.getConstructor(int.class,int.class));

		for (int density=1; density <= MAX_DENSITY; density++)
		{
			filters = createFilterArray( factory, sample, inputFile, density ); 
			for (int runsize : RUNSIZE )
			{
				System.out.println( "Running N="+runsize);
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
		printReport( table );
	}
	
	private static void printReport( List<Stats> table ) throws IOException {
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
		
		File f = File.createTempFile( "Bloom", ".csv");
		System.out.println("Writing data to "+f);
		 BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		writer.write(Summary.header());
		writer.write( eol );
		for (final Summary.Element e : summary.getTable()) {
			writer.write( e.toString() );
			writer.write( eol );
		}
	    writer.close();

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
			bfSample[i] = factory.create(sample.get(i).country_code);
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
			stats[j].reportLoad(j);
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
			stats[j].registerResult(pos, total, found);
			stats[j].reportCount(pos, j);
		}

	}

	
	
	public static BloomFilter[] createFilterArray( NormalBloomFilterFactory factory, List<GeoName> sample, URL inputFile, int density ) throws IOException
	{
		System.out.println( "Creating filters for density "+density);
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
					builder.add(gn);
					if (loadSample && sample.size()<1000 && (geoCount % 1000)==0)
					{
						sample.add( gn );
					}
					geoCount++;
				}
				result[i]=builder.build();
			}
			return result;
		} finally {
			if (br != null)
			{
				try {
					br.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}
	}
}
