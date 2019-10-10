package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;

/**
 * Plain ol' linear search of a list
 *
 */
public class BloomIndexLinearList extends BloomIndex {
	List<BloomFilter> index;
	int idx;

	public BloomIndexLinearList(int population,BloomFilterConfiguration bloomFilterConfig)
	{
		super(population, bloomFilterConfig);
		this.index = new ArrayList<BloomFilter>(population);
		this.idx = 0;
	}

	@Override
	public void add(BloomFilter filter)
	{
	    index.add( filter );
	}

	@Override
	public int count(BloomFilter filter)
	{
		int result = 0;
				// searching entire list
				for (BloomFilter candidate : index)
				{
					if (filter.matches(candidate))
					{
						result++;
					}
				}
			return result;

	}

	@Override
	public String getName() {
		return "Linear List";
	}

}
