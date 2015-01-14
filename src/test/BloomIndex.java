package test;

import java.util.List;



public abstract class BloomIndex {
	/** Constructor to force limit argument in constructor **/
	protected BloomIndex( int limit, int width )
	{
		// do nothing
	}
	abstract public void add(BloomFilter filter);
	abstract public List<BloomFilter> get(BloomFilter filter);
	abstract public int count(BloomFilter filter);
	abstract public String getName();
}
