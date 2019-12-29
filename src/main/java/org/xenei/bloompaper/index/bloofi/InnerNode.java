package org.xenei.bloompaper.index.bloofi;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BitSetBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.SetOperations;


public class InnerNode implements Node {

    private BloomFilter filter;
    private Node[] buckets;
    private int used = 0;
    private InnerNode parent;
    private BloomFilter.Shape shape;

    public InnerNode( InnerNode parent, BloomFilter.Shape shape) {
        this.shape = shape;
        filter = new BitSetBloomFilter(shape);
        buckets = new Node[16]; // number of children of the the node.
        used = 0;
        this.parent = parent;
    }

    public InnerNode getParent()
    {
        return parent;
    }

    @Override
    public void setParent(InnerNode parent)
    {
        this.parent = parent;
    }

    @Override
    public void add( BloomFilter candidate )
    {
        filter.merge(candidate);

        int closest = 0;
        if (used > 1)
        {
            int closestDistance = SetOperations.hammingDistance( candidate, buckets[0].getFilter());
            for (int i=1;i<used;i++)
            {
                int dist = SetOperations.hammingDistance( candidate, buckets[i].getFilter());
                if (dist<closestDistance)
                {
                    closestDistance = dist;
                    closest = i;
                }
            }
        }
        insert( closest, candidate );
    }

    private void insert( int position, BloomFilter candidate)
    {
        if (buckets[position] instanceof InnerNode)
        {
            ((InnerNode)buckets[position]).add( candidate );
        }
        else {
            if (used == 0)
            {
                buckets[0] = new LeafNode( this, candidate );
                used++;
                return;
            }
            if (buckets[position].getFilter().equals(candidate))
            {
                ((LeafNode)buckets[position]).add( candidate);
                return;
            }
            if (used < buckets.length)
            {
                System.arraycopy(buckets, position, buckets, position+1, used-position);
                buckets[position]=new LeafNode(this, candidate);
                used++;
                return;
            }
            else
            {
                InnerNode sibling = split(candidate);
                if (position < used)
                {
                    filter.merge(candidate);
                    this.insert(position, candidate);
                }
                else
                {
                    sibling.filter.merge(candidate);
                    sibling.insert( position-used, candidate );
                }
            }
        }
    }

    private InnerNode split( BloomFilter candidate )
    {
        // we are full so so split this node and return the result
        InnerNode sibling = new InnerNode( parent, shape );
        int splitPoint = buckets.length/2;
        sibling.used = buckets.length-splitPoint;
        System.arraycopy(buckets, splitPoint, sibling.buckets, 0, sibling.used);
        used -= sibling.used;
        Arrays.fill( buckets, used, buckets.length, null);

        // reset our filter
        filter = new BitSetBloomFilter( shape );
        for (int i=0;i<used;i++)
        {
            filter.merge( buckets[i].getFilter());
        }

        // populate the sibling filter
        for (int i=0;i<sibling.used;i++)
        {
            sibling.filter.merge( sibling.buckets[i].getFilter());
            sibling.buckets[i].setParent( sibling );
        }

        // if we are the root create a new root.
        if (parent == null)
        {
            parent = new InnerNode( null, shape );
            parent.insert( this );
            parent.filter.merge( candidate );
        }

        // add the sibling to the parent
        parent.insert(sibling);
        return sibling;
    }

    private void insert(InnerNode newNode)
    {
        // this only gets called on a child split.
        filter.merge(newNode.getFilter());

        if (used == 0)
        {
            buckets[0]=newNode;
            newNode.setParent(this);
            used++;
            return;
        }

        int position = 0;
        int closestDistance = SetOperations.hammingDistance( newNode.filter, buckets[0].getFilter());
        for (int i=1;i<used;i++)
        {
            int dist = SetOperations.hammingDistance(newNode.filter, buckets[i].getFilter());
            if (dist<closestDistance)
            {
                closestDistance = dist;
                position = i;
            }
        }

        if (used < buckets.length)
        {
            System.arraycopy(buckets, position, buckets, position+1, used-position);
            buckets[position]=newNode;
            newNode.setParent(this);
            used++;
            for (int i=0;i<used;i++)
            {
                if (buckets[position] == null)
                {
                    throw new IllegalStateException();
                }
            }
        }
        else
        {
            // we are full so so split this node and return the result
            InnerNode sibling = split( newNode.filter );
            if (position < used)
            {
                this.insert(newNode);
            }
            else
            {
                sibling.insert( newNode );
            }
        }
    }

    @Override
    public BloomFilter getFilter() {
        return filter;
    }

    @Override
    public boolean remove(BloomFilter filter) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void search(List<BloomFilter> results, BloomFilter filter) {
        if (this.filter.contains(filter))
        {
            for (int i=0;i<used;i++ )
            {
                buckets[i].search( results, filter);
            }
        }
    }

    @Override
    public int count(BloomFilter filter) {
        int retval = 0;
        if (this.filter.contains(filter))
        {
            for (int i=0;i<used;i++ )
            {
                retval += buckets[i].count( filter);
            }
        }
        return retval;
    }

}
