package org.xenei.bloompaper.index.bloofi;

import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.bloomfilter.BloomFilter;

/**
 * Bloofi Node definition.
 *
 */
public interface Node {

    /**
     * Class that holds the count for the node during creation.
     */
    public static class Counter {
        /**
         * The node count.
         */
        static int totalCount = 0;
        /**
         * Gets the next id.
         * @return the next id.
         */
        public static int nextId() {
            return totalCount++;
        }
        /**
         * Resets the counter to zero.
         */
        public static void reset() {
            totalCount = 0;
        }
    }

    /**
     * Gets the id for this node.
     * @return the id.
     */
    public int getId();

    /**
     * Sets the parent of this node.
     * @param parent the parrent node.
     */
    public void setParent( InnerNode parent );

    /**
     * Gets the parent of this node.
     * Will return null for the root node.
     * @return the parent of this node.
     */
    public InnerNode getParent();

    /**
     * Get the filter for this node.
     * @return
     */
    public BloomFilter getFilter();

    /**
     * Add the Bloom filter to this node.
     * @param filter the Bloom filter to add.
     */
    public void add(BloomFilter filter);

    /**
     * Removes a Filter from this node.
     * @param filter the filter to remove.
     * @return true if the node was removed.
     */
    public boolean remove(BloomFilter filter);

    /**
     * Searches the node for the Bloom filters that match the filter.
     * @param results The List to add the results to.
     * @param filter the filter to look for.
     */
    public void search(List<BloomFilter> results, BloomFilter filter);

    /**
     * Counts the number of stored Bloom filters that match the filter.
     * @param filter  the Bloom filter to match.
     * @return the number of matching bloom filters.
     */
    public int count(BloomFilter filter);

    /**
     * Returns true if the node is empty.
     * @return true if the node is empty.
     */
    public boolean isEmpty();

    /**
     * Sets the filter capture for the count.
     * Every bloom filter returned by the count is added to this collection.
     * Used in debugging. In general the collection is a Null collection that does nothing.
     * @param collection the Collection to add the filters to.
     */
    public void setFilterCapture(Collection<BloomFilter> collection);

}
