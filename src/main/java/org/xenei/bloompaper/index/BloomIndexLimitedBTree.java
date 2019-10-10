package org.xenei.bloompaper.index;


import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.hamming.HammingUtils;
import org.xenei.bloompaper.index.btree.BTree;
import org.xenei.bloompaper.index.btree.InnerNode;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexLimitedBTree extends BloomIndex {
	BTree btree;
	BloomIndexLinear linear;

	public BloomIndexLimitedBTree(int population, BloomFilterConfiguration bloomFilterConfig)
	{
		super(population, bloomFilterConfig);
		this.btree = new BTree(bloomFilterConfig);
		linear = new BloomIndexLinear(population, bloomFilterConfig);
	}

	@Override
	public void add(BloomFilter filter)
	{
		btree.add( filter );
		linear.add( filter );
	}

	private int minimumHamming(int width, int nOfEntries, int buckets)
    {
	    // linear or btree search
        // H >= W - log2( N / logC(N) )
        // W = BloomFilter.WIDTH
        // H = filter.getHammingWeight()
        // N = btree.getN()
        // C = InnerNode.BUCKETS
        double logC = Math.log(nOfEntries)/Math.log(buckets);
        double log2 = Math.log(nOfEntries/logC)/Math.log(2);
        return (int) Math.ceil( width - log2 );
    }

	@Override
	public int count(BloomFilter filter)
	{
		int minHam = minimumHamming(bloomFilterConfig.getNumberOfBits(), btree.getN(), InnerNode.BUCKETS);

		if (filter.getHammingWeight() >= minHam)
		{
			return btree.count(filter);
		}
		else
		{
			return linear.count(filter);
		}
	}

	@Override
	public String getName() {
		return "LimitedBtree";
	}

}
