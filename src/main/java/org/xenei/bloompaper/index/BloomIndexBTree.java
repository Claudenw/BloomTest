package org.xenei.bloompaper.index;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.index.btree.stack.BTree;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexBTree extends BloomIndex {
	private BTree btree;

	public BloomIndexBTree(int limit, BloomFilterConfiguration bloomFilterConfig)
	{
		super(limit);
		this.btree = new BTree(bloomFilterConfig);
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
		return "Btree";
	}
}
