package test;

import geoname.GeoName;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import normalfilter.NormalBloomFilter;
import normalfilter.NormalBloomFilterFactory;

public class Test2 {
	private static int RUNSIZE =  100000;
	// 9,772,346 max lines
	private static int MAX_FACTOR = 9772346/RUNSIZE;
	
	private static String eol = System.getProperty("line.separator");
	
	public static void main(final String[] args) throws Exception {
		final URL inputFile = Test2.class.getResource("allCountries.txt");
		BloomIndexHamming idx;
		int maxDensity = 9;
		int [][] reportData = new int[maxDensity][NormalBloomFilterFactory.WIDTH+1];
		for (int i=0;i<maxDensity;i++) {
			idx = createFilters( i+1, inputFile );
			BloomIndexHamming.HammingStats stats = idx.getStats();
			for (int j=stats.getMinimumHamming();j<=stats.getMaximumHamming();j++)
			{
				BloomIndexHamming.Block block = stats.getBlock( j );
				if ( block != null)
				{
					reportData[i][j] = block.getLength();
				}
			}
		}
		
		doReport( maxDensity, reportData );
	}
	
	private static void doReport( int maxDensity, int[][] reportData ) throws IOException
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
		
		for (int i=1;i<NormalBloomFilterFactory.WIDTH;i++)
		{
			sb = new StringBuilder().append( i ).append(",");
			for (int j=i;j<=NormalBloomFilterFactory.WIDTH;j++)
			{
				sb.append( Util.getMissProbability(NormalBloomFilterFactory.WIDTH, i, j))
				.append(",");
			}
			sb.deleteCharAt( sb.length()-1 );
			System.out.println( sb.toString());
		}
		
		File f = File.createTempFile( "Bloom", ".csv");
		System.out.println("Writing data to "+f);
		 BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		 sb = new StringBuilder().append( "'density'" ).append(",");
			for (int i=0;i<NormalBloomFilterFactory.WIDTH+1;i++)
			{
				sb.append( i ).append( ",");
			}
			sb.deleteCharAt( sb.length()-1 );
			writer.write( sb.toString());
			writer.write( eol );
			for (int i=0;i<maxDensity;i++)
			{
				int[] data = reportData[i];
				sb = new StringBuilder().append( i+1 ).append(",");
				for (int j=0;j<data.length;j++)
				{
					sb.append( data[j] ).append( "," );
				}
				sb.deleteCharAt( sb.length()-1 );
				writer.write( sb.toString());
				writer.write( eol );
			}
		
	    writer.close();
		
	}
	
	private static BloomIndexHamming createFilters( int density, URL inputFile) throws IOException
	{
		if (density <1) {
			throw new IllegalArgumentException( "Density must be greater than 0");
		}
		if ( density > MAX_FACTOR) {
			throw new IllegalArgumentException( "Density must be less than "+MAX_FACTOR);
		}
		System.out.println( "Creating Filters of density "+density);
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
			idx.add( builder.build());
		}
		
		return idx;
	}

}
