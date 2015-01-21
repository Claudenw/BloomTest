package test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import normalfilter.NormalBloomFilter;
import test.BloomIndexHamming.Block;
import test.BloomIndexHamming.HammingStats;
import test.btree.InnerNode;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexLimitedHamming extends BloomIndexHamming {
	//private int count;
	private int MIN_LIST = 100; // below this the linear scan is faster.
	
	public BloomIndexLimitedHamming( int limit, int width )
	{
		super(limit, width);
		setStats( new LimitedHammingStats(width));
	//	this.count = 0;
	}

	public LimitedHammingStats getStats()
	{
		return (LimitedHammingStats) super.getStats();
	}
	
	@Override
	public void add(BloomFilter filter)
	{
		super.add(filter);
	//	count++;
	}
	
	// linear or btree search
				// H >= W - log2( N / logC(N) )
				// W = BloomFilter.WIDTH
				// H = filter.getHammingWeight()
				// N = btree.getN()
				// C = InnerNode.BUCKETS = 2
//	public int minimumHamming()
//	{
//		double logC = Math.log(count)/Math.log(2);
//		double log2 = Math.log(count/logC)/Math.log(2);
//		return Double.valueOf(Math.ceil( width - log2 )).intValue();
//	}
//	
//	@Override
//	public List<BloomFilter> get(BloomFilter filter)
//	{
//		int hStart = filter.getHammingWeight();
//		if (filter.getHammingWeight() >= minimumHamming())
//		{
//			return super.get(filter);
//		}
//		
//		List<BloomFilter> retval = new ArrayList<BloomFilter>();
//		for (BloomFilter candidate : lst)
//		{
//			if (filter.match( candidate ))
//			{
//				retval.add( candidate );
//			}
//		}
//		return retval;
//	}

//	@Override
//	public int count(BloomFilter filter)
//	{
//		int hStart = filter.getHammingWeight();
//		if (filter.getHammingWeight() >= minimumHamming())
//		{
//			return super.count(filter);
//		}
//		int retval = 0;
//		
//		for (BloomFilter candidate : lst)
//		{
//			if (filter.match( candidate ))
//			{
//				retval++;
//			}
//		}
//		return retval;
//	}
	
	@Override
	public String getName() {
		return "Limited Hamming";
	}

	public class LimitedHammingStats extends HammingStats {
		
		private double[] factorials;
		
		public LimitedHammingStats(int width) {
			super( width );
			factorials = new double[width+1];
			factorials[0] = 1.0;
			for (int i=1;i<width+1;i++)
			{
				factorials[i] = factorials[i-1]*i;
			}
		}
		
		public double factorial(int i)
		{
			if (i<0 || i>width)
			{
				throw new IllegalArgumentException( String.format("Factorial must be between 0 and %s inclusive", width));
			}
			return factorials[i];
		}
		
		
		@Override
		protected Block newBlock(int[] blocks)
		{
			return new LimitedBlock(blocks);
		}
		
		
	}
	
	public class LimitedBlock extends Block {
		
		
		public LimitedBlock(int[] data) {
			super(data);
		}
		
		// 0 length blocks are filtered out before here
		// exact match
		public List<BloomFilter> get(BloomFilter filter)
		{
			List<BloomFilter> subList = getSubList();
			List<BloomFilter> retval = new ArrayList<BloomFilter>();
			if (getLength() < MIN_LIST)
			{
				doScanLinear( filter, retval, subList, 0, getLength() );
				return retval;
			}
			
			int pos = binarySearch(subList, filter, true, false);
			if (pos >= 0) {
				doScanLinear( filter, retval, subList, pos, findUpperLimit( filter,subList) );
				return retval;
			}
			return Collections.emptyList();
		}
		

		// exact match
		public int count(BloomFilter filter) 
		{
			List<BloomFilter> subList = getSubList();
			
			if (getLength() < MIN_LIST)
			{
				return doCountMatchLinear( filter, subList, 0, getLength() );
			}
			
			int pos = binarySearch(subList, filter, true, false);
			if (pos >= 0) {
				if (getLength()-pos < MIN_LIST)
				{
					return doCountMatchLinear( filter, subList, pos, getLength() );
				}
				
				int limit = binarySearch(subList, filter, true, true);
				return doCountMatchLinear( filter, subList, pos, limit );
			}
			return 0;
		}
		
				
		// range match
		public void scan(BloomFilter filter, List<BloomFilter> result)
		{
			List<BloomFilter> subList = getSubList();
			
			if (getLength() < MIN_LIST)
			{
				doScanLinear( filter, result, subList, 0, getLength() );
				return;
			}
			
			int pos = binarySearch(subList, filter, true, false);
			if (pos < 0) {
				pos = Math.abs( pos+1);
			}
			doScanLinear( filter, result, subList, pos, findUpperLimit( filter, subList));
		}
	
	private void doScanLinear(BloomFilter filter, List<BloomFilter> retval, List<BloomFilter> subList, int start, int limit)
	{
		for (int pos = start;pos<limit;pos++)
		{
			
			BloomFilter candidate = subList.get(pos);
			if (filter.match(candidate))
			{
				retval.add(candidate);
			}
			pos++;
		}
	}
	
		private int findUpperLimit( BloomFilter filter, List<BloomFilter> subList )
		{
			NormalBloomFilter.Builder builder = new NormalBloomFilter.Builder( width, 0);
			BloomFilter candidate = subList.get(0);
			int s = candidate.getHammingWeight()-filter.getHammingWeight();
			int bit = width;
			for (int i=0;i<s;i++)
			{
				builder.set(bit);
				bit--;
			}
			BitSet target = NormalBloomFilter.Builder.toBitSet( filter );
			target.xor(builder.getBitSet());
			boolean isUpperLimit = true;
			bit = width;
			for (int i=0;i<s;i++)
			{
				isUpperLimit &= target.get(bit);
				bit--;
			}
			int limit = getLength();
			if (isUpperLimit)
			{
				 limit = binarySearch(subList, filter, true, true);
				 if (limit < 0 )
				 {
					 limit = Math.abs( limit+1);
				 }
			}
			return limit;
		}

		
		public int countMatch(BloomFilter filter)
		{
			
			List<BloomFilter> subList = getSubList();
			
			if (getLength() < MIN_LIST)
			{
				return doCountMatchLinear( filter, subList, 0, getLength() );
			}
			int pos = binarySearch(subList, filter, true, false);
			if (pos >= 0) {
				return doCountMatchLinear( filter, subList, pos, findUpperLimit( filter, subList ) );
			}
			return 0;
		}
		
		private int doCountMatchLinear( BloomFilter filter, List<BloomFilter> subList, int start, int limit)
		{
			int retval = 0;
			for (int pos=start;pos<limit;pos++)
			{
					BloomFilter candidate = subList.get(pos);
					if (filter.match(candidate))
					{
						retval++;
					}
			}
			return retval;
		}
		
	}
}
