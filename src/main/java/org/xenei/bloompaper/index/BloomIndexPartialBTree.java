package org.xenei.bloompaper.index;


import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.index.btree.stack.BTree;
import org.xenei.bloompaper.index.btree.stack.InnerNode;


/**
 * BTree with stack implementation that only goes to 1/2 depth before
 * switching to a linear search of results.
 */
public class BloomIndexPartialBTree extends BloomIndex {
	BTree btree;
	BloomFilterConfiguration bloomFilterConfig;

	public BloomIndexPartialBTree(int limit,BloomFilterConfiguration bloomFilterConfig)
	{
		super(limit);
		this.bloomFilterConfig = bloomFilterConfig;
		int depth = (bloomFilterConfig.getNumberOfBits()/InnerNode.WIDTH) /2;
		this.btree = new BTree( bloomFilterConfig, depth );
	}

	@Override
	public void add(BloomFilter filter)
	{
		btree.add( filter );;
	}

	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		return btree.search(filter);
	}

	@Override
	public int count(BloomFilter filter)
	{
		return btree.count(filter);
	}

	@Override
	public String getName() {
		return "PartialBtree";
	}

}