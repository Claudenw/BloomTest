package org.xenei.bloompaper.index;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.index.bloofi2.Bloofi;
import org.xenei.bloompaper.index.flatbloofi.FlatBloofi;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexFlatBloofi extends BloomIndex {
	private FlatBloofi bloofi;

	public BloomIndexFlatBloofi(int population, BloomFilterConfiguration bloomFilterConfig)
	{
		super(population, bloomFilterConfig);
		this.bloofi = new FlatBloofi(population, bloomFilterConfig);
	}

	@Override
	public void add(BloomFilter filter)
	{
	    bloofi.add( filter );;
	}



	@Override
	public int count(BloomFilter filter)
	{
		return bloofi.count(filter);
	}

	@Override
	public String getName() {
		return "Flat Bloofi";
	}
}
