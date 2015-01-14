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

public class Test2 {
	private static int RUNSIZE =  100000;
	// 9,772,346 max lines
	private static int MAX_FACTOR = 9772346/RUNSIZE;

	public static void main(final String[] args) throws Exception {
				final List<Stats> table = new ArrayList<Stats>();
		
		final URL inputFile = Test2.class.getResource("allCountries.txt");
		BloomIndexHamming idx;
		int maxDensity = 9;
		int [][] reportData = new int[maxDensity][NormalBloomFilterFactory.WIDTH+1];
		for (int i=0;i<maxDensity;i++) {
			idx = createFilters( i+1, inputFile );
			int[][] offsets = idx.getOffsets();
			for (int j=0;j<NormalBloomFilterFactory.WIDTH+1;j++)
			{
				reportData[i][j] = offsets[j][BloomIndexHamming.LENGTH];
			}
		}
		
		doReport( maxDensity, reportData );
	}
	
	private static void doReport( int maxDensity, int[][] reportData )
	{
		StringBuilder sb = new StringBuilder().append( "'density'" ).append(",");
		for (int i=0;i<NormalBloomFilterFactory.WIDTH+1;i++)
		{
			sb.append( i ).append( ",");
		}
		sb.deleteCharAt( sb.length()-1 );
		System.out.println( sb.toString());
		
		for (int i=0;i<maxDensity;i++)
		{
			int[] data = reportData[i];
			sb = new StringBuilder().append( i+1 ).append(",");
			for (int j=0;j<data.length;j++)
			{
				sb.append( data[j] ).append( "," );
			}
			sb.deleteCharAt( sb.length()-1 );
			System.out.println( sb.toString());
		}
		
	}
	
	private static BloomIndexHamming createFilters( int density, URL inputFile) throws IOException
	{
		if (density <1) {
			throw new IllegalArgumentException( "Density must be greater than 0");
		}
		if ( density > MAX_FACTOR) {
			throw new IllegalArgumentException( "Density must be less than "+MAX_FACTOR);
		}
		
		BloomIndexHamming idx = new BloomIndexHamming( RUNSIZE, NormalBloomFilterFactory.WIDTH);
		NormalBloomFilter.Builder builder = new NormalBloomFilterFactory().getBuilder();
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				inputFile.openStream()));
		for (int i = 0; i < RUNSIZE; i++) {

			for (int j=0;j<density;j++)
			{
				final GeoName gn = GeoName.parse(br.readLine());
				builder.add(gn);
			}
			idx.add( builder.build());;
		}
		return idx;
	}

}
