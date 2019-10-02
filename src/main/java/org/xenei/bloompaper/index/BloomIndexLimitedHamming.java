package org.xenei.bloompaper.index;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.hamming.HammingUtils;
import org.xenei.bloompaper.index.BloomIndexHamming.HammingList;
import org.xenei.bloompaper.index.btree.InnerNode;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexLimitedHamming extends BloomIndex {
	private Map<Integer,HammingList> index;
	private int count;
	private BloomFilterConfiguration bloomFilterConfig;

	public BloomIndexLimitedHamming( int limit,BloomFilterConfiguration bloomFilterConfig )
	{
		super(limit);
		this.bloomFilterConfig = bloomFilterConfig;
		this.index = new HashMap<Integer,HammingList>();
		this.count = 0;
	}

	@Override
	public void add(BloomFilter filter)
	{
		Integer idx = filter.getHammingWeight();
		HammingList hList = index.get(idx);
		if (hList == null)
		{
			hList = new HammingList();
			index.put(idx, hList);
		}
		hList.add( filter );
		count++;
	}

	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		List<BloomFilter> retval = new ArrayList<BloomFilter>();
		// do direct check;
		Integer hFilter = filter.getHammingWeight();
		Iterator<BloomFilter> iter = null;

		int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), count, InnerNode.BUCKETS);
		if (filter.getHammingWeight() >= minHam)
		{
			HammingList hList = null;
			for (Integer idx : index.keySet())
			{
				if (idx == hFilter)
				{
					hList = index.get(idx);
					if (hList != null)
					{
						iter = hList.get(filter);
						while (iter.hasNext())
						{
							retval.add( iter.next() );
						}
					}
				}
				if (idx > hFilter)
				{
					hList = index.get(idx);
					if (hList != null)
					{
						iter = hList.find(filter);
						while (iter.hasNext())
						{
							BloomFilter found = iter.next();
							if (filter.match( found ))
							{
								retval.add( found );
							}
						}
					}
				}
			}
		}
		else
		{
			for (HammingList hList : index.values())
			{
				iter = hList.find(filter);
				while (iter.hasNext())
				{
					BloomFilter found = iter.next();
					if (filter.match( found ))
					{
						retval.add( found );
					}
				}
			}
		}
		return retval;
	}

	@Override
	public int count(BloomFilter filter)
	{
		int retval = 0;
		// do direct check;
		Integer hFilter = filter.getHammingWeight();
		Iterator<BloomFilter> iter = null;

		int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), count, InnerNode.BUCKETS);
		if (filter.getHammingWeight() >= minHam)
		{
			HammingList hList = null;
			for (Integer idx : index.keySet())
			{
				if (idx == hFilter)
				{
					hList = index.get(idx);
					if (hList != null)
					{
						iter = hList.get(filter);
						while (iter.hasNext())
						{
							retval++;
							iter.next();
						}
					}
				}
				if (idx > hFilter)
				{
					hList = index.get(idx);
					if (hList != null)
					{
						iter = hList.find(filter);
						while (iter.hasNext())
						{
							BloomFilter found = iter.next();
							if (filter.match( found ))
							{
								retval++;
							}
						}
					}
				}
			}
		}
		else
		{
			for (HammingList hList : index.values())
			{
				iter = hList.find(filter);
				while (iter.hasNext())
				{
					BloomFilter found = iter.next();
					if (filter.match( found ))
					{
						retval++;
					}
				}
			}
		}
		return retval;
	}

	@Override
	public String getName() {
		return "LimitedHamming";
	}

}
