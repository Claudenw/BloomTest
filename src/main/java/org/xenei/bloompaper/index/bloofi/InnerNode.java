package org.xenei.bloompaper.index.bloofi;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.SetOperations;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;

/**
 * An inner node for the Bloofi tree.
 *
 */
public class InnerNode implements Node {

    /**
     * Number of buckets on the node.
     */
    private static final int NODE_SIZE = 16;

    /**
     * The counting bloom filter for all the filters below.
     */
    private CountingBloomFilter filter;

    /**
     * The buckets of inner nodes.
     */
    private Node[] buckets;

    /**
     * Number of used buckets.
     */
    private int used = 0;

    /**
     * The parent of this node.  May be null.
     */
    private InnerNode parent;

    /**
     * The shape of the filters stored in this node.
     */
    private Shape shape;

    /**
     * Constructs an inner node.
     * @param parent the parent of this node (may be null).
     * @param shape the Shape of the Bloom filters that will be stored.
     */
    public InnerNode( InnerNode parent, Shape shape) {
        this.shape = shape;
        filter = new ArrayCountingBloomFilter(shape);
        buckets = new Node[NODE_SIZE];
        used = 0;
        this.parent = parent;
    }

    /**
     * Gets the parent of this node.
     * @return the parrent node, or null if not set.
     */
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

    /**
     * Insert the candidate in the specified bucket.
     * @param position the bucket to store the candidate in.
     * @param candidate the candidate to store.
     */
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

    /**
     * Split the node.
     * @param candidate the candidate that caused the split.
     * @return the other node created by the split.
     */
    private InnerNode split( BloomFilter candidate )
    {
        /* we are full so split this node and return the result.
         * The split operation does not change the bloom filter of the
         * parent but does change the bloom filter of this node.
         */
        InnerNode sibling = new InnerNode( parent, shape );
        int splitPoint = buckets.length/2;
        sibling.used = buckets.length-splitPoint;
        System.arraycopy(buckets, splitPoint, sibling.buckets, 0, sibling.used);
        used -= sibling.used;
        Arrays.fill( buckets, used, buckets.length, null);

        // reset our filter
        filter = new ArrayCountingBloomFilter( shape );
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

    /**
     * Insert an inner node into this node.
     * This only gets called during a split operation.
     * @param newNode the node to insert.
     */
    private void insert(InnerNode newNode)
    {
        // the filter does not change
        //filter.merge(newNode.getFilter());

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
                if (buckets[i] == null)
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

    /**
     * Remove the specified child node.
     * @param position the node to remove.
     */
    private void remove( int position )
    {
        Node node = buckets[position];
        if (node instanceof InnerNode)
        {
            filter.subtract( (CountingBloomFilter) node.getFilter());
        } else {
            filter.remove( node.getFilter() );
        }
        if (position+1 < used)
        {
            System.arraycopy(buckets, position+1, buckets, position, used-position-1);
        }
        buckets[--used] = null;
    }

    @Override
    public boolean isEmpty() {
        return used == 0;
    }

    @Override
    public boolean remove(BloomFilter filter) {
        // locate the child node that contains the filter.
        if (this.filter.contains(filter))
        {
            for (int i=0;i<used;i++ )
            {
                if (buckets[i].remove(filter))
                {
                    if (buckets[i].isEmpty())
                    {
                        remove( i );
                    }
                    return true;
                }
            }
        }
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
