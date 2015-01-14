package test;

import java.util.List;

import test.btree.BTree;

/**
 * BTree implementation that does not use a stack outside of the call stack.
 *
 */
public class BloomIndexBTree extends BloomIndex {
	private BTree btree;
	
	public BloomIndexBTree(int limit, int width)
	{
		super(limit, width);
		this.btree = new BTree(width);
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
