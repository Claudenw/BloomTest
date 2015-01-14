package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import test.btree.InnerNode;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexLimitedHamming extends BloomIndexHamming {
	private int count;
	
	public BloomIndexLimitedHamming( int limit, int width )
	{
		super(limit, width);
		this.count = 0;
	}

	@Override
	public void add(BloomFilter filter)
	{
		super.add(filter);
		count++;
	}
	
	// linear or btree search
				// H >= W - log2( N / logC(N) )
				// W = BloomFilter.WIDTH
				// H = filter.getHammingWeight()
				// N = btree.getN()
				// C = InnerNode.BUCKETS = 2
	public int minimumHamming()
	{
		double logC = Math.log(count)/Math.log(2);
		double log2 = Math.log(count/logC)/Math.log(2);
		return Double.valueOf(Math.ceil( width - log2 )).intValue();
	}
	
	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		int hStart = filter.getHammingWeight();
		if (filter.getHammingWeight() >= minimumHamming())
		{
			return super.get(filter);
		}
		
		List<BloomFilter> retval = new ArrayList<BloomFilter>();
		for (BloomFilter candidate : lst)
		{
			if (filter.match( candidate ))
			{
				retval.add( candidate );
			}
		}
		return retval;
	}

	@Override
	public int count(BloomFilter filter)
	{
		int hStart = filter.getHammingWeight();
		if (filter.getHammingWeight() >= minimumHamming())
		{
			return super.count(filter);
		}
		int retval = 0;
		
		for (BloomFilter candidate : lst)
		{
			if (filter.match( candidate ))
			{
				retval++;
			}
		}
		return retval;
	}
	
	@Override
	public String getName() {
		return "Limited Hamming";
	}

}
