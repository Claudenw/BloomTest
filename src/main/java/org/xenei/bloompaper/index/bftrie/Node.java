package org.xenei.bloompaper.index.bftrie;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

/**
 * A BFTrie node
 *
 */
public interface Node {
    public void add(BFTrie4 btree, BloomFilter filter, long[] buffer);

    /**
     * Return true if the node was removed.
     * @param filter
     * @return
     */
    public boolean remove(long[] buffer);

    public boolean find(long[] buffer);

    public boolean isEmpty();

    public void search(List<BloomFilter> results, long[] buffer);

    public int count(Collection<BloomFilter> collection, long[] buffer);
}
