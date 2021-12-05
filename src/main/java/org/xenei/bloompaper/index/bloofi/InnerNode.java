package org.xenei.bloompaper.index.bloofi;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.SetOperations;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.PseudoCountingBloomFilter;

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
    private int used;

    /**
     * The parent of this node. May be null.
     */
    private InnerNode parent;

    /**
     * The shape of the filters stored in this node.
     */
    private Shape shape;

    /**
     *  The id of this node (used in debugging)
     */
    private final int id;

    /**
     * Constructs an inner node.
     *
     * @param parent the parent of this node (may be null).
     * @param shape  the Shape of the Bloom filters that will be stored.
     */
    public InnerNode(InnerNode parent, Shape shape) {
        this.id = Node.Counter.nextId();
        this.shape = shape;
        filter = new ArrayCountingBloomFilter(shape);
        buckets = new Node[NODE_SIZE];
        used = 0;
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format(
                String.format("InnerNode:%s %s", this.id, BitUtils.formatHex(BloomFilter.asBitMapArray(filter))));
    }

    /**
     * Gets the parent of this node.
     *
     * @return the parrent node, or null if not set.
     */
    @Override
    public InnerNode getParent() {
        return parent;
    }

    @Override
    public void setParent(InnerNode parent) {
        this.parent = parent;
    }

    @Override
    public void add(BloomFilter candidate) {
        insert(candidate);
    }

    /**
     * Determine the closest node to the candidate from among those in the buckets.
     * @param candidate the bloom filter to use to test.
     * @return the closest node.
     */
    private int determineClosest(BloomFilter candidate) {
        int closest = 0;
        int closestDistance = SetOperations.hammingDistance(candidate, buckets[0].getFilter());
        for (int i = 1; i < used; i++) {
            int dist = SetOperations.hammingDistance(candidate, buckets[i].getFilter());
            if (dist < closestDistance) {
                closestDistance = dist;
                closest = i;
            }
        }
        return closest;
    }

    /**
     * Insert the candidate in the specified bucket.
     *
     * @param position  the bucket to store the candidate in.
     * @param candidate the candidate to store.
     */
    private void insert(BloomFilter candidate) {
        if (used == 0) {
            buckets[used++] = new LeafNode(this, candidate);
        } else if (buckets[0] instanceof InnerNode) {
            ((InnerNode) buckets[determineClosest(candidate)]).add(candidate);
        } else {
            buckets[used++] = new LeafNode(this, candidate);
        }

        /*
         * if the buckets are full we split this page
         */
        if (used == NODE_SIZE) {
            split();
        }
    }

    /**
     * Convert the Bloom filter to a counting Bloom filter if necessary.
     * @param filter the filter to
     * @return
     */
    private CountingBloomFilter asCountingFilter(final BloomFilter filter) {
        if (filter instanceof CountingBloomFilter) {
            return (CountingBloomFilter) filter;
        } else {
            return new PseudoCountingBloomFilter(filter);
        }
    }

    /**
     * Split the node. Does not modify the parent filter. Does modify this filter
     * and create sibling filter. Will create a new root with both this and sibling
     * filter counts.
     *
     * @param candidate the candidate that caused the split.
     * @return the other node created by the split.
     */
    private InnerNode split() {

        /*
         * we are full so split this node and return the result. The split operation
         * does not change the bloom filter of the parent but does change the bloom
         * filter of this node.
         */
        InnerNode sibling = new InnerNode(parent, shape);
        int splitPoint = buckets.length / 2;
        sibling.used = buckets.length - splitPoint;
        System.arraycopy(buckets, splitPoint, sibling.buckets, 0, sibling.used);
        used -= sibling.used;
        Arrays.fill(buckets, used, buckets.length, null);

        // reset our filter
        filter = new ArrayCountingBloomFilter(shape);
        for (int i = 0; i < used; i++) {
            filter.add(asCountingFilter(buckets[i].getFilter()));
        }

        // populate the sibling filter
        for (int i = 0; i < sibling.used; i++) {
            sibling.buckets[i].setParent(sibling);
            CountingBloomFilter bucketFilter = asCountingFilter(sibling.buckets[i].getFilter());
            sibling.filter.add(bucketFilter);
        }

        // if we are the root create a new root.
        if (parent == null) {
            parent = new InnerNode(null, shape);
            parent.insert(this);
            parent.getFilter().add(this.getFilter());
            parent.getFilter().add(sibling.getFilter());
        }

        /*
         * Add the sibling to the parent. This may cause the parent to split but when it
         * returns from the insert the parent will be correctly set.
         */
        parent.insert(sibling);

        return sibling;
    }

    /**
     * Insert an inner node into this node. This only gets called during a split
     * operation.
     *
     * @param newNode the node to insert.
     */
    private void insert(InnerNode newNode) {
        // first entry insert and return.
        buckets[used++] = newNode;
        newNode.setParent(this);
        if (used == NODE_SIZE) {
            split();
        }
    }

    @Override
    public CountingBloomFilter getFilter() {
        return filter;
    }

    /**
     * Remove the specified node from the buckets.
     * If the nodes is the last one remove this inner node from its parent.
     *
     * @param node from the list of children.
     */
    public void remove(Node node) {
        for (int position = 0; position < used; position++) {
            if (buckets[position] == node) {
                if (position + 1 < used) {
                    System.arraycopy(buckets, position + 1, buckets, position, used - position - 1);
                }
                buckets[--used] = null;
                if (used == 0) {
                    if (parent != null) {
                        parent.remove(this);
                    }
                } else {
                    CountingBloomFilter nodeFilter = asCountingFilter(node.getFilter());
                    this.filter.subtract(nodeFilter);
                    InnerNode parent = this.parent;
                    while (parent != null) {
                        parent.filter.subtract(nodeFilter);
                        parent = parent.parent;
                    }
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return used == 0;
    }

    @Override
    public boolean remove(BloomFilter filter) {
        // locate the child node that contains the filter.
        if (this.filter.contains(filter)) {
            for (int i = 0; i < used; i++) {
                if (buckets[i].remove(filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void search(Consumer<BloomFilter> results, BloomFilter filter) {
        if (this.filter.contains(filter)) {
            for (int i = 0; i < used; i++) {
                buckets[i].search(results, filter);
            }
        }
    }

    @Override
    public int getId() {
        return id;
    }

}
