package org.xenei.bloompaper.index;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * base class for a Bloom Index.  All BloomIndexes run by Test must implement this class.
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
     * @return true if the filter was deleted.
     */
    abstract public boolean delete(BloomFilter filter);

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final int count(BloomFilter filter) {
        BloomIndex.Incrementer incr = new BloomIndex.Incrementer();
        doSearch(incr, filter);
        return incr.count;
    }

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final int count(Consumer<BloomFilter> consumer, BloomFilter filter) {
        BloomIndex.Incrementer incr = new BloomIndex.Incrementer();
        doSearch(consumer.andThen(incr), filter);
        return incr.count;
    }

    /**
     * Counts the number of matching Bloom filters in the index.
     * @param filter  The Bloom filter to count.
     */
    public final void search(Consumer<BloomFilter> consumer, BloomFilter filter) {
        doSearch(consumer, filter);
    }

    /**
     * Executes the search for the filter an places all matching filters in the consumer.
     * @param consumer the Consumer to accept the matching filters.
     * @param filter the filter to search for.
     */
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

    /**
     * Counts the number of filters returned by a search or count operation.
     *
     */
    public static class Incrementer implements Consumer<BloomFilter> {
        public int count = 0;

        @Override
        public void accept(BloomFilter t) {
            count++;
        }
    }
}
