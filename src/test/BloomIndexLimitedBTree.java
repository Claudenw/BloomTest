package test;

import java.util.List;

import test.btree.BTree;
import test.btree.InnerNode;

/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexLimitedBTree extends BloomIndex {
	BTree btree;
	BloomIndexLinear linear;
	
	public BloomIndexLimitedBTree(int limit, int width)
	{
		super(limit, width);
		this.btree = new BTree(width);
		linear = new BloomIndexLinear(limit,width);
	}

	@Override
	public void add(BloomFilter filter)
	{
		btree.add( filter );
		linear.add( filter );
	}
	
	private int minimumHamming(int width, int nOfEntries)
	{
		// linear or btree search
				// H >= W - log2( N / logC(N) )
				// W = BloomFilter.WIDTH
				// H = filter.getHammingWeight()
				// N = btree.getN()
				// C = InnerNode.BUCKETS
		double logC = Math.log(nOfEntries)/Math.log(InnerNode.BUCKETS);
		double log2 = Math.log(nOfEntries/logC)/Math.log(2);
		return Double.valueOf(Math.ceil( width - log2 )).intValue();
	}
	
	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		int minHam = minimumHamming(filter.getWidth(), btree.getN());
		
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
		
		
		int minHam = minimumHamming(filter.getWidth(), btree.getN());
		
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
