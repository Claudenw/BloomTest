package test;

import hamming.DoubleLong;

import java.util.List;

import test.btree.BTree;
import test.btree.InnerNode;

/**
 * BTree with stack implementation that only goes to 1/2 depth before
 * switching to a linear search of results.
 */
public class BloomIndexPartialBTree extends BloomIndex {
	BTree btree;
	
	public BloomIndexPartialBTree(int limit, int width)
	{
		super(limit, width);
		int depth = (width/InnerNode.WIDTH) /2;
		this.btree = new BTree( depth );
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
