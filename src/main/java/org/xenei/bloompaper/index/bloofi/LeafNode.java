package org.xenei.bloompaper.index.bloofi;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.LongConsumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.exceptions.NoMatchException;
import org.xenei.bloompaper.Test;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.BloomIndex;
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

    private Collection<BloomFilter> filterCapture;

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
        Test.lastCreated = this;
        updateFilters( candidate );
    }

    /**
     * Updates all the parent counting Bloom filters with this Bloom filter.
     * @param candidate
     */
    private void updateFilters( BloomFilter candidate ) {
        InnerNode node = getParent();
        while (node != null)
        {
            node.getFilter().mergeInPlace( candidate );
            node = node.getParent();
        }
    }

    @Override
    public void setParent(InnerNode parent)
    {
        this.parent = parent;
    }

    @Override
    public InnerNode getParent() {
        return parent;
    }

    @Override
    public void add(BloomFilter filter)
    {
        ++count;
        updateFilters( this.filter );
    }



    @Override
    public boolean remove(BloomFilter filter) {
        BufferCompare comp = new BufferCompare( this.filter, (x,y) -> x.equals(y) );
        if (comp.matches(filter))
        {
            count--;
            InnerNode node = getParent();
            if (count <= 0)
            {
                node.remove( this );
            } else {
                /* update the filters */
                while (node != null)
                {
                    node.getFilter().remove( filter );
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
            filterCapture.add(this.filter);
            return count;
        }
        return 0;
    }

    public int count() {
        return count;
    }

    @Override
    public String toString()
    {
        long[] bits = BloomFilter.asBitMapArray(filter);
        return String.format( String.format( "LeafNode %s: %s (%s)", id, BitUtils.formatHex( bits ), BitUtils.format( bits ) ) );
    }

    @Override
    public BloomFilter getFilter() {
        return filter;
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        this.filterCapture = collection;
    }

    @Override
    public int getId() {
        return id;
    }


}
