package org.xenei.bloompaper.index;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;



public abstract class BloomIndex {
	/** Constructor to force limit argument in constructor **/
	protected BloomIndex( int limit )
	{
		// do nothing
	}
	abstract public void add(BloomFilter filter);
	abstract public List<BloomFilter> get(BloomFilter filter);
	abstract public int count(BloomFilter filter);
	abstract public String getName();
}
