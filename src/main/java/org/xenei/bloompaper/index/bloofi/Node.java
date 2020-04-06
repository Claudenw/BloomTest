package org.xenei.bloompaper.index.bloofi;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

/**
 * Bloofi Node definition.
 *
 */
public interface Node {
    /**
     * Set the parent of this node.
     * @param parent the parrent node.
     */
    public void setParent( InnerNode parent );

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
}
