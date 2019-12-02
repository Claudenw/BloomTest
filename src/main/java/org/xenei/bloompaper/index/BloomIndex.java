package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter.Shape;
import org.xenei.bloompaper.InstrumentedBloomFilter;



public abstract class BloomIndex {
    protected final Shape shape;
    protected final int population;
    /** Constructor to force limit argument in constructor **/
    protected BloomIndex( int population, Shape shape )
    {
        this.shape = shape;
        this.population = population;
    }
    abstract public void add(InstrumentedBloomFilter filter);

    abstract public int count(InstrumentedBloomFilter filter);
    abstract public String getName();
}
