package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;



public abstract class BloomIndex {
    protected final BloomFilter.Shape shape;
    protected final int population;
    /** Constructor to force limit argument in constructor **/
    protected BloomIndex( int population, BloomFilter.Shape shape )
    {
        this.shape = shape;
        this.population = population;
    }
    abstract public void add(InstrumentedBloomFilter filter);

    abstract public int count(InstrumentedBloomFilter filter);
    abstract public String getName();
}
