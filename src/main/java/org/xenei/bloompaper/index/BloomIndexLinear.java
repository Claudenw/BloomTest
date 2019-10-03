package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;

/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexLinear extends BloomIndex {
	BloomFilter[] index;
	int idx;
	BloomFilterConfiguration config;

	public BloomIndexLinear(int population,BloomFilterConfiguration bloomFilterConfig)
	{
		super(population, bloomFilterConfig);
		this.index = new BloomFilter[population];
		this.idx = 0;
		this.config = bloomFilterConfig;
	}

	@Override
	public void add(BloomFilter filter)
	{
		index[idx++] = filter;
	}

	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		List<BloomFilter> result = new ArrayList<BloomFilter>();
				// searching entire list
				for (BloomFilter candidate : index)
				{
					if (filter.match(candidate))
					{
						result.add(candidate);
					}
				}
			return result;

	}

	@Override
	public int count(BloomFilter filter)
	{
		int result = 0;
				// searching entire list
				for (BloomFilter candidate : index)
				{
					if (filter.match(candidate))
					{
						result++;
					}
				}
			return result;

	}

	@Override
	public String getName() {
		return "Linear";
	}

}
