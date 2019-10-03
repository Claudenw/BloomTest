package org.xenei.bloompaper.hamming;

public class HammingUtils {

	// linear or btree search
			// H >= W - log2( N / logC(N) )
			// W = BloomFilter.WIDTH
			// H = filter.getHammingWeight()
			// N = btree.getN()

	public static int minimumHamming(int width, int nOfEntries, int buckets)
	{
		double logC = Math.log(nOfEntries)/Math.log(buckets);
		double log2 = Math.log(nOfEntries/logC)/Math.log(2);
		return (int) Math.ceil( width - log2 );
	}

}
