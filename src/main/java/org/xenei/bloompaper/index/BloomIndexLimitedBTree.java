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
	BloomFilterConfiguration bloomFilterConfig;

	public BloomIndexLimitedBTree(int limit, BloomFilterConfiguration bloomFilterConfig)
	{
		super(limit);
		this.bloomFilterConfig = bloomFilterConfig;
		this.btree = new BTree(bloomFilterConfig);
		linear = new BloomIndexLinear(limit);
	}

	@Override
	public void add(BloomFilter filter)
	{
		btree.add( filter );
		linear.add( filter );
	}

	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		// linear or btree search
		// H >= W - log2( N / logC(N) )
		// W = BloomFilter.WIDTH
		// H = filter.getHammingWeight()
		// N = btree.getN()
		// C = InnerNode.BUCKETS
		int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), btree.getN(), InnerNode.BUCKETS);

		if (filter.getHammingWeight() >= minHam)
		{
			return btree.search(filter);
		}
		else
		{
			return linear.get( filter );
		}

	}

	@Override
	public int count(BloomFilter filter)
	{
		// linear or btree search
		// H >= W - log2( N / logC(N) )
		// W = BloomFilter.WIDTH
		// H = filter.getHammingWeight()
		// N = btree.getN()
		// C = InnerNode.BUCKETS

		int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), btree.getN(), InnerNode.BUCKETS);

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
