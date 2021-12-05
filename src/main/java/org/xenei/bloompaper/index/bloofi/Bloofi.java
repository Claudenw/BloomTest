package org.xenei.bloompaper.index.bloofi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * Traditional Bloofi implementation.
 *
 */
public class Bloofi {

    /**
     * The root node of the Bloofi  tree
     */
    private InnerNode root;
    private int count;

    /**
     * Constructs a bloofi index.
     * @param limit the number of expected filters.
     * @param shape the Shape of the filters.
     */
    public Bloofi(int limit, Shape shape) {
        root = new InnerNode(null, shape);
    }

    /**
     * Add a filter to the index.
     * @param candidate the index to add.
     */
    public void add(BloomFilter candidate) {
        count++;
        root.add(candidate);

        while (root.getParent() != null) {
            root = root.getParent();
        }
    }

    public void delete(BloomFilter candidate) {
        if (root.remove(candidate)) {
            count--;
        }
    }

    /**
     * Gets the number of Bloom filters that have been added to the index.
     * @return the number of Bloom filters that have been added to the index.
     */
    public int count() {
        return count;
    }

    /**
     * Gets a list of BloomFilters that match the specified filter.
     * @param filter the filter to match.
     * @return a list of matching filters.
     */
    public List<BloomFilter> get(BloomFilter filter) {
        List<BloomFilter> retval = new ArrayList<BloomFilter>();
        root.search(retval::add, filter);
        return retval;
    }

    /**
     * Counts the number of filters in the index that match the filter.
     * @param filter the filter to match.
     * @return the number of filters that match.
     */
    public void search(Consumer<BloomFilter> result, BloomFilter filter) {
        root.search( result, filter);
    }

}
