package org.xenei.bloompaper.index;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.index.btree.BTree;


/**
 * BTree implementation that does not use a stack outside of the call stack.
 *
 */
public class BloomIndexBTreeNoStack extends BloomIndex {
	private BTree btree;

	public BloomIndexBTreeNoStack(int population, BloomFilterConfiguration bloomFilterConfig)
	{
		super(population, bloomFilterConfig);
		this.btree = new BTree(bloomFilterConfig);
	}

	@Override
	public void add(BloomFilter filter)
	{
		btree.add( filter );;
	}

	@Override
	public int count(BloomFilter filter)
	{
		return btree.count(filter);
	}

	@Override
	public String getName() {
		return "BtreeNoStack";
	}

}
