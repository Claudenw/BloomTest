package org.xenei.bloompaper.index.bloofi;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class LeafNode implements Node {
    private final BloomFilter filter;
    private int count;
    private InnerNode parent;

    public LeafNode(InnerNode parent, BloomFilter candidate) {
        this.filter = candidate;
        this.count = 1;
        this.parent = parent;
    }

    @Override
    public void setParent(InnerNode parent)
    {
        this.parent = parent;
    }

    @Override
    public void add(BloomFilter filter)
    {
        ++count;
    }

    @Override
    public boolean remove(BloomFilter filter) {
        if (count>0)
        {
            count--;
            return true;
        }
        return false;
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
