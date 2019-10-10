package org.xenei.bloompaper.index;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;



public abstract class BloomIndex {
    protected final BloomFilterConfiguration bloomFilterConfig;
    protected final int population;
	/** Constructor to force limit argument in constructor **/
	protected BloomIndex( int population, BloomFilterConfiguration bloomFilterConfig )
	{
		this.bloomFilterConfig = bloomFilterConfig;
		this.population = population;
	}
	abstract public void add(BloomFilter filter);

	abstract public int count(BloomFilter filter);
	abstract public String getName();
}
