package org.xenei.bloompaper.index.bloofi;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

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

    /**
     * Constructs a Leaf Node.
     * @param parent the parent of this node.
     * @param candidate the filter that this node holds
     */
    public LeafNode(InnerNode parent, BloomFilter candidate) {
        this.filter = candidate;
        this.count = 1;
    }

    @Override
    public void setParent(InnerNode parent)
    {
        // does nothing
    }

    @Override
    public void add(BloomFilter filter)
    {
        ++count;
    }


    @Override
    public boolean remove(BloomFilter filter) {
        if (Arrays.compare( this.filter.getBits(), filter.getBits())==0 && count>0)
        {
            count--;
            return true;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return count <= 0;
    }

    @Override
    public void search(List<BloomFilter> result, BloomFilter filter) {
        if (this.filter.contains(filter))
        {
            for (int i=0;i<count;i++)
            {
                result.add( this.filter );
            }
        }
    }

    @Override
    public int count(BloomFilter filter) {
        if (this.filter.contains(filter))
        {
            return count;
        }
        return 0;
    }

    @Override
    public String toString()
    {
        return String.format( "LeafNode %s", filter );
    }

    @Override
    public BloomFilter getFilter() {
        return filter;
    }

}
