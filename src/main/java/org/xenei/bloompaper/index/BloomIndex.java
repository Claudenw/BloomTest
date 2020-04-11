package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.Stats;


/**
 * base class for a Bloom Index.
 */
public abstract class BloomIndex {
    /**
     * The shape of the filters being stored
     */
    protected final Shape shape;
    /**
     * The number of Bloom filters expected in the index.
     */
    protected final int population;

    /**
     * Constructor.
     * @param population the number of Bloom filters expected in the index
     * @param shape the Shape of the bloom filters.
     */
    protected BloomIndex( int population, Shape shape )
    {
        this.shape = shape;
        this.population = population;
    }

    /**
     * Adds an Bloom filter to the index.
     * @param filter  The Bloom filter to add
     */
    abstract public void add(BloomFilter filter);

    /**
     * Deletes an Bloom filter from the index.
     * @param filter  The Bloom filter to delete.
     */
    abstract public void delete(BloomFilter filter);

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    abstract public int count(BloomFilter filter);

    /**
     * Gets the name of the index implementation.
     * @return the name of hte implementation.
     */
    abstract public String getName();

    /**
     * Counts the number of Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    abstract public int count();

}
