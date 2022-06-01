package org.xenei.bloompaper.index.bloofi;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.BitUtils.BufferCompare;

/**
 * A leaf node of the Bloofi tree.
 */
public class LeafNode implements Node {
    /**
     * The bloom filter that this node contains.
     */
    private final BloomFilter filter;
    /**
     * The number of times this filter has been inserted.
     */
    private int count;

    private InnerNode parent;

    private final int id;

    /**
     * Constructs a Leaf Node.
     * @param parent the parent of this node.
     * @param candidate the filter that this node holds
     */
    public LeafNode(InnerNode parent, BloomFilter candidate) {
        this.filter = candidate;
        this.id = Node.Counter.nextId();
        this.count = 1;
        this.parent = parent;
        updateFilters(candidate);
    }

    /**
     * Updates all the parent counting Bloom filters with this Bloom filter.
     * @param candidate
     */
    private void updateFilters(BloomFilter candidate) {
        InnerNode node = getParent();
        while (node != null) {
            node.getFilter().mergeInPlace(candidate);
            node = node.getParent();
        }
    }

    @Override
    public void setParent(InnerNode parent) {
        this.parent = parent;
    }

    @Override
    public InnerNode getParent() {
        return parent;
    }

    @Override
    public void add(BloomFilter filter) {
        ++count;
        updateFilters(this.filter);
    }

    @Override
    public boolean remove(BloomFilter filter) {
        BufferCompare comp = new BufferCompare(this.filter, BitUtils.BufferCompare.exact);
        if (comp.matches(filter)) {
            count--;
            InnerNode node = getParent();
            if (count <= 0) {
                node.remove(this);
            } else {
                /* update the filters */
                while (node != null) {
                    node.getFilter().remove(filter);
                    node = node.getParent();
                }
            }
            return true;

        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return count <= 0;
    }

    @Override
    public void search(Consumer<BloomFilter> result, BloomFilter filter) {
        if (this.filter.contains(filter)) {
            for (int i = 0; i < count; i++) {
                result.accept(this.filter);
            }
        }
    }

    public int count() {
        return count;
    }

    @Override
    public String toString() {
        long[] bits = filter.asBitMapArray();
        return String
                .format(String.format("LeafNode %s: %s (%s)", id, BitUtils.formatHex(bits), BitUtils.format(bits)));
    }

    @Override
    public BloomFilter getFilter() {
        return filter;
    }

    @Override
    public int getId() {
        return id;
    }

}
