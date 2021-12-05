package org.xenei.bloompaper.index;

import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * base class for a Bloom Index.
 */
public abstract class BloomIndex {

    public static Consumer<BloomFilter> NULL_CONSUMER = (b) -> {};
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
    protected BloomIndex(int population, Shape shape) {
        this.shape = shape;
        this.population = population;
    }

    /**
     * Adds an Bloom filter to the index.
     * @param filter  The Bloom filter to add
     */
    abstract public void add(BloomFilter filter);

    /**
     * Deletes a Bloom filter from the index.
     * @param filter  The Bloom filter to delete.
     */
    abstract public void delete(BloomFilter filter);

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final int count(BloomFilter filter) {
        BloomIndex.Incrementer incr = new BloomIndex.Incrementer();
        doSearch( incr, filter );
        return incr.count;
    }

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final int count(Consumer<BloomFilter> consumer,BloomFilter filter) {
        BloomIndex.Incrementer incr = new BloomIndex.Incrementer();
        doSearch( consumer.andThen(incr), filter );
        return incr.count;
    }

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final void search(Consumer<BloomFilter> consumer, BloomFilter filter) {
        doSearch( consumer, filter );
    }

    abstract protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter);

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

    public static class Incrementer implements Consumer<BloomFilter> {
        public int count=0;

        @Override
        public void accept(BloomFilter t) {
            count++;
        }
    }
}
