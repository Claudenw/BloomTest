package org.xenei.bloompaper.index.bloofi2;

/*
 * Implementation of the Bloom Filter Index - BlooFI
 * The BloomFilterIndex creates a hierarchical index for Bloom filters.
 * All the Bloom filters indexed have the same size and use the same
 * hashing functions for insertion/ to check for membership
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.apache.commons.collections4.bloomfilter.StandardBloomFilter;
import org.xenei.bloompaper.index.BloomIndex;

/**
 *
 * @author adina
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class Bloofi extends BloomIndex {

    private BFINode root;
    private int order;

    // things to help with testing/experiments
    //private Hashtable<Integer, BFINode> idMap;
    private List<BloomFilter> bfList;
    private boolean splitFull;
    private BloomFilterConfiguration config;
    private InsDelUpdateStatistics stat;

//    public Set<Integer> getIDs() {
//        return idMap.keySet();
//    }

    /**
     * Constructs an empty Bloom Filter Index with just the root
     */
    public Bloofi(int limit, BloomFilterConfiguration config) {
        this( limit, 16, true, config);
    }


    /**
     * Constructs an empty Bloom Filter Index with just the root
     */
    public Bloofi(int population, int order,
                            boolean splitFull, BloomFilterConfiguration config) {
        super(population, config);
        root = null;
        this.order = order;
 //       this.idMap = new Hashtable<Integer, BFINode>();
        this.bfList = new ArrayList<BloomFilter>();
        this.splitFull = splitFull;
        this.config = config;
        this.stat = new InsDelUpdateStatistics();

        // initialize the BFI with a root with an all-zero bloom filter
        BloomFilter zeroFilter = StandardBloomFilter.EMPTY;
        this.root = new BFINode(zeroFilter, this.order,
                                   this.splitFull);
    }

    /**
     * Constructs a Bloom Filter Index for the Bloom Filters received as
     * param
     *
     */
    public Bloofi(int population, List<BloomFilter> bfList, int order,
                            boolean splitFull,BloomFilterConfiguration config) {
        super(population, config);
        this.order = order;
        this.splitFull = splitFull;
        this.stat = new InsDelUpdateStatistics();
      //  this.idMap = new Hashtable<Integer, BFINode>();
        this.bfList = bulkLoad(bfList);
        this.config = config;

    }

    /**
     * Get the ID- BFINode map
     *
     * @return
     */
    // public Hashtable<Integer, BFINode> getIDBFINodeMap(){
    // return this.idMap;
    // }

    /**
     * Return the height of the bloom Filter Index - A tree with only root
     * and leaves has height 1
     *
     * @return
     */
    public int getHeight() {
        return root.getLevel();
    }

    /**
     * Return the number of nodes in this Bloom Filter Index
     *
     * @return
     */
    public int getSize() {
        return this.root.getTreeSize();
    }

    /**
     * Return the size - number of bits in a Bloom Filter indexed by this
     * index
     *
     * @return
     */
    public int getBloomFilterSize() {
        return config.getNumberOfBits();
    }

    /**
     * Return the number of children of the root
     */
    public int getNbChildrenRoot() {
        return this.root.children.size();
    }

    /**
     * Return whether all the bits in the root are 1 or not
     *
     * @return
     */
    public boolean getIsRootAllOne() {
        return config.isFull(this.root.value);
    }

    /**
     * Get the list of all BloomFIlters indexed by this index
     *
     * @return
     */
    public List<BloomFilter> getBFList() {
        return this.bfList;
    }

//    /**
//     * Update the Bloom Filter Index due to the new value for BloomFilter
//     * with given ID
//     *
//     * @param newBloomFilter
//     * @return
//     */
//    public int updateIndex(BloomFilter newBloomFilter,
//                           InsDelUpdateStatistics stat) {
////        int id = newBloomFilter.getID();
//        // find the node corresponding to the id
////        BFINode node = this.idMap.get(id);
//        if (node == null) {
//            System.err
//            .println("ERROR: Cound not find node with ID "
//                     + id);
//            return -1;
//        }
//        updateValueToTheRoot(node, newBloomFilter, stat);
//        return 0;
//
//    }

//    /**
//     * Delete the Bloom filter with the given ID from the index
//     */
//    public int deleteFromIndex(int id, InsDelUpdateStatistics stat) {
//        // find the node corresponding to the id
//        BFINode node = this.idMap.get(id);
//        if (node == null) {
//            System.err
//            .println("ERROR delete: Could not find node with ID "
//                     + id);
//            return -1;
//        }
//        else {
//            //System.err
//            //       .println("OK delete: node with ID "
//            //              + id);
//        }
//        deleteNode(node, stat);
//
//        // delete from the bflist and idMap
//        this.idMap.remove(id);
//        this.bfList.remove(node.value);
//
//        return 0;
//
//    }

    /**
     * Delete the given node from the index. The deletion moves bottom up
     *
     * @param node
     */
    private void deleteNode(BFINode childNode) {

        if (this.root.children.size() < 2) {
            System.err.println("ERROR: nb children of root is "
                               + this.root.children.size());
            System.err.println(this.toString());
            assert false;
        }

        // remove node from the list of children in its parent
        BFINode node = childNode.parent;
        boolean ok = node.children.remove(childNode);
        assert ok;
        stat.nbBFNodesAccessed += 2; // get parent, plus parent node
        // accessed

        // check whether the tree height needs to be reduced
        // this is the case if the parent is the root and
        // only one non-leaf child remained
        if (node == this.root && node.children.size() == 1) {
            if (!node.children.get(0).isLeaf()) {
                this.root = node.children.get(0);
                this.root.parent = null;
                stat.nbBFNodesAccessed++; // changed root
                return;
            }
        }

        stat.nbBFNodesAccessed++; // check for merge
        // check if underflow at the parent
        if (!node.needMerge()) {
            // no underflow, update values
            recomputeValueToTheRoot(node);
        } else {
            // try to re-distribute
            // get a sibling of the node
            int index = node.parent.children.indexOf(node);
            stat.nbBFNodesAccessed += 2; // check position to find
            // sibling
            BFINode sibling;
            boolean isRightSibling = false;
            // try the right sibling. if does not exist, try the
            // left one
            if (index + 1 < node.parent.children.size()) {
                isRightSibling = true;
                sibling = node.parent.children
                          .get(index + 1);
            } else {
                if (index - 1 < 0) {
                    System.err.println("Error "
                                       + this.toString() + " node: "
                                       + node.toString()
                                       + "childNode: "
                                       + childNode.toString());
                    assert false;
                }

                isRightSibling = false;
                sibling = node.parent.children
                          .get(index - 1);
            }
            stat.nbBFNodesAccessed++; // get the sibling
            // see if the sibling can redistribute
            stat.nbBFNodesAccessed++; // check if can redistribute
            if (sibling.canRedistribute()) {
                redistribute(node, sibling, isRightSibling);
            } else {
                merge(node, sibling, isRightSibling);
                // delete the node
                deleteNode(node);

            }
        }

        return;
    }

    /**
     * Redistribute the entries between 2 siblings and update values to the
     * root
     *
     * @param node
     * @param sibling
     */
    private void redistribute(BFINode node, BFINode sibling,
                              boolean isRightSibling) {

        stat.nbRedistributes++;

        // get nb entries in both;
        int nbChildren = node.children.size() + sibling.children.size();
        int nbChildren1 = nbChildren / 2;
        int nbChildren2 = nbChildren - nbChildren1;
        int nbChildrenToGive = sibling.children.size() - nbChildren2;

        stat.nbBFNodesAccessed += 2; // accessed siblings to get size

        BFINode childToMove;
        if (isRightSibling) {
            // move first nbChildrenToGive from sibling to node
            for (int i = 0; i < nbChildrenToGive; i++) {
                childToMove = sibling.children.remove(0);
                node.children.add(childToMove);
                childToMove.parent = node;
            }
        } else {
            // move last nbChildrenToGive from sibling to node
            for (int i = 0; i < nbChildrenToGive; i++) {
                childToMove = sibling.children
                              .remove(sibling.children.size() - 1);
                node.children.add(0, childToMove);
                childToMove.parent = node;
            }

        }
        // update stat
        stat.nbBFNodesAccessed += nbChildrenToGive + 2; // accessed
        // node,
        // sibling, and
        // all new
        // children

        // recompute values for all nodes involved, up to the root
        sibling.recomputeValue();
        recomputeValueToTheRoot(node);
    }

    /**
     * Merge the entries between 2 siblings; all the entries from the node
     * are given to the sibling, since the node has fewer children value of
     * sibling will be updated to be the OR of all children
     *
     * @param node
     * @param sibling
     */
    private void merge(final BFINode node, final BFINode sibling,
                               final boolean isRightSibling) {

        stat.nbMerges++;

        // get nb entries to move;
        int nbChildrenToGive = node.children.size();

        stat.nbBFNodesAccessed++; // accessed node to get size

        BFINode childToMove;
        if (isRightSibling) {
            // move last nbChildrenToGive from node to sibling
            for (int i = 0; i < nbChildrenToGive; i++) {
                childToMove = node.children
                              .remove(node.children.size() - 1);
                sibling.children.add(0, childToMove);
                sibling.value = sibling.value.merge(childToMove.value);
                childToMove.parent = sibling;
            }

        } else {
            // move first nbChildrenToGive from node to sibling
            for (int i = 0; i < nbChildrenToGive; i++) {
                childToMove = node.children.remove(0);
                sibling.children.add(childToMove);
                sibling.value = sibling.value.merge(childToMove.value);
                childToMove.parent = sibling;
            }

        }

        // update stat
        stat.nbBFNodesAccessed += nbChildrenToGive + 2; // accessed
        // node,
        // sibling, and
        // all new
        // children
        stat.nbBFAccessed += nbChildrenToGive + 1; // add new children
        // to the value

    }

    /**
     * Bulk load a Bloom Filter Index. It changes the root field.
     */
    private List<BloomFilter> bulkLoad(List<BloomFilter> mbfList) {
        // "sort" the received list of Bloom filters according to some
        // metric
        ArrayList<BloomFilter> copy = new ArrayList<BloomFilter>(mbfList);
        mbfList = sort(copy);

        // keep pointer to right-most leaf
        BFINode rightmost;

        // initialize the BFI with a root with an all-zero bloom filter
        BloomFilter sampleFilter = mbfList.get(0);
        this.root = new BFINode(StandardBloomFilter.EMPTY, this.order,
                                   this.splitFull);
        rightmost = this.root;

        // insert each Bloomfilter in the rightmost leaf
        // if needed, split
        BFINode current;
        for (BloomFilter bf : mbfList) {
            // create a BFINode for this BloomFilter
            current = new BFINode(bf, this.order, this.splitFull);

            // insert the ID - node mapping into the hashtable
            //this.idMap.put(bf.getID(), current);

            // insert it into the rightmost leaf
            rightmost = insertRight(true, rightmost, current,
                                    rightmost);

        }

        return mbfList;
    }

    /**
     * Search for an object in the BFI and return the matching Bloom filters
     *
     * @param o
     * @return
     */
    public ArrayList<BloomFilter> searchBloomFilters(BloomFilter filter) {
        return findMatches(this.root, filter);
    }

//    @Override
//    public List<Integer> search(E o, SearchStatistics stat) {
//        ArrayList<BloomFilter> x = searchBloomFilters(o, stat);
//        ArrayList<Integer> ans = new ArrayList<Integer>(x.size());
//        for (BloomFilter bf : x) {
//            ans.add( bf.getID());
//        }
//        return ans;
//    }

    /**
     * Search for an object in the subtree rooted at given node and return
     * the Bloom filters matching the value
     *
     * @param node
     * @param o
     * @return
     */
    public ArrayList<BloomFilter> findMatches(BFINode node, BloomFilter filter) {
        ArrayList<BloomFilter> result = new ArrayList<BloomFilter>();
//        stat.nbBFChecks++;
        // if node does not matches the object,
        // return empty set, else check the descendants
        if (!filter.match( node.value )) {
            return result;
        }

        // if this node is a leaf, just return the value
        if (node.isLeaf()) {

            result.add(node.value);
            return result;
        }

        // if not leaf, check the descendants
        for (int i = 0; i < node.children.size(); i++) {
            result.addAll(findMatches(node.children.get(i), filter));
        }

        return result;
    }
//    /**
//     * Search for an object in the subtree rooted at given node and return
//     * the Bloom filters matching the value, it does so using a naive
//     * approach where only leaf nodes are checked
//     *
//     * @param node
//     * @param o
//     * @return
//     */
//    public ArrayList<BloomFilter> naivefindMatches(BFINode node, E o,
//            SearchStatistics stat) {
//
//        ArrayList<BloomFilter> result = new ArrayList<BloomFilter>();
//
//        // increase the number of bloom filters checks, since this node
//        // will be checked
//        stat.nbBFChecks++;
//        if(node.isLeaf()) {
//            if(node.value.contains(o)) {
//                result.add(node.value);
//            }
//            return result;
//        }
//        for (int i = 0; i < node.children.size(); i++) {
//            result.addAll(naivefindMatches(node.children.get(i), o, stat));
//        }
//        return result;
//    }
//


//    /**
//     * Create an all-zero Bloom filter with the same size as given filter
//     *
//     * @param filter
//     * @return Empty Bloom filter with the size and expected number of
//     *         elements same as the input filter
//     */
//    private static BloomFilter createZeroBloomFilter(BloomFilter filter) {
//
//        /*
//         * Old implementation: does not work in extreme cases due to
//         * double math where (a/double(b) * b is not always a) For
//         * example, if expected number of elements is 100,000,000 and
//         * falsePosProb is 0.01
//         */
//        /*
//         * int bitSetSize = filter.size(); int expectedNumberOfElements
//         * = filter.getExpectedNumberOfElements();
//         * System.out.println("BitSetSize is: " + bitSetSize);
//         * System.out.println("ExpectedNbOfElements: " +
//         * expectedNumberOfElements);
//         * System.out.println("bitsPerElement: " +
//         * bitSetSize/(double)expectedNumberOfElements);
//         * System.out.println("Calculated raw bitSetSize: " +
//         * bitSetSize/(double)expectedNumberOfElements *
//         * expectedNumberOfElements);
//         * System.out.println("Math.ceil bitSetSize: " +
//         * Math.ceil(bitSetSize/(double)expectedNumberOfElements *
//         * expectedNumberOfElements));
//         * System.out.println("(int)Math.ceil bitSetSize: " +
//         * (int)Math.ceil(bitSetSize/(double)expectedNumberOfElements *
//         * expectedNumberOfElements));
//         *
//         * BitSet zeroBitSet = new BitSet(bitSetSize);
//         *
//         * //create all zeros BloomFilter BloomFilter zeroFilter =
//         * new BloomFilter(bitSetSize, expectedNumberOfElements, 0,
//         * zeroBitSet);
//         *
//         * assert bitSetSize == zeroFilter.size(); assert filter.size()
//         * == zeroFilter.size();
//         */
//
//        // public BloomFilter(double c, int n, int k) {
//        double bitsPerElement = filter.getExpectedBitsPerElement();
//        int expectedNumberOfFilterElements = filter
//                                             .getExpectedNumberOfElements();
//        int k = filter.getK();
//        int metric = filter.getMetric();
//
//        // create all zeros BloomFilter
//        BloomFilter zeroFilter = new BloomFilter(filter.h,
//                bitsPerElement, expectedNumberOfFilterElements, k,
//                metric);
//
//        // assert zeroFilter.getExpectedNumberOfElements() ==
//        // expectedNumberOfFilterElements;
//        // assert zeroFilter.getK() == k;
//        // assert zeroFilter.getExpectedBitsPerElement() ==
//        // bitsPerElement;
//
//        assert filter.size() == zeroFilter.size();
//        if(filter.size() != zeroFilter.size()) throw new RuntimeException("size mismatch?");
//        if(zeroFilter.h != filter.h) throw new RuntimeException("different hasher?");
//
//        return zeroFilter;
//    }

    /**
     * Sort the given list according to some distance We use Hamming
     * distance for now and sort such that first element is closest to zero,
     * the next one is closest to the first, and so on
     *
     * @param bfList
     * @return
     */
    private ArrayList<BloomFilter> sort(List<BloomFilter> bf) {
        return sortIterative(bf);
    }

    private int findClosest(List<BloomFilter> bfList, BloomFilter bf) {

        // return null if no element to compare with
        if (bfList.isEmpty())
            return -1;

        // initialize min distance to be distance to first element
        double minDistance = bf.distance(bfList.get(0));
        int minIndex = 0;
        double currentDistance;

        // loop through all elements to find the closest
        for (int i = 1; i < bfList.size(); i++) {
            currentDistance = bf.distance(bfList.get(i));
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                minIndex = i;
            }
        }

        return minIndex;
    }
    /**
     * Sort the input list based on Hamming distance between objects First
     * object will be the closest to "000...", then the closest to first...
     *
     * @param c
     * @return
     */
    private  ArrayList<BloomFilter> sortIterative(
        final List<BloomFilter> bf) {

        System.out.print("| sortIterative start");

        long startTime = System.currentTimeMillis();

        ArrayList<BloomFilter> sorted = new ArrayList<BloomFilter>();

        // create all zeros BloomFilter
        BloomFilter current = StandardBloomFilter.EMPTY;

        BloomFilter closest;
        int closestIndex;

        // Iterativelly, find the BloomFilter closest to current filter,
        // move it to the sorted list
        // the closest filter becomes the current
        while (!bf.isEmpty()) {
            // find the bloom filter closest to the current on
            closestIndex = findClosest(bf,current);
            closest = bf.get(closestIndex);
            // add it to the sorted list
            sorted.add(closest);
            // remove it from the initial list
            bf.remove(closestIndex);
            // make it the new current
            current = closest;
        }

        long endTime = System.currentTimeMillis();
        long diffTime = endTime - startTime;
        System.out.print("| sortIterative end");
        System.out.print("| Sorting time millis| " + diffTime);

        return sorted;
    }

    /**
     * Insert a new child into "current" node as last (rightmost) child
     * Return the possibly new rightmost index node above the leaf level
     *
     * @param newChild
     * @return new rightmost index node above leaf level
     */
    private BFINode insertRight(boolean isInBFI, BFINode current,
                                   BFINode newChild, BFINode rightmost) {

        // create array of children if none exists
        if (current.children == null) {
            current.children = new ArrayList<BFINode>();
        }

        // add the new child to the right
        current.children.add(newChild);
        newChild.parent = current;
        stat.nbBFNodesAccessed += 2; // current and new child link

        // update the value to be the current value or child value
        if (!isInBFI) {
            current.value = current.value.merge(newChild.value);
            stat.nbBFAccessed += 2;
        }
        // if child inserted is a new leaf, update all parent values to
        // the root
        // if child inserted is not a leaf, no need to update the
        // parents,
        // since they already have the correct value
        if (newChild.isLeaf()) {
            updateValueToTheRoot(current, newChild.value);
        }

        stat.nbBFNodesAccessed++; // check for split
        // check if need to split, if no, just return the old rightmost
        // node
        if (!current.needSplit()) {
            return rightmost;
        }

        // else, split the node
        rightmost = splitRight(current, rightmost);

        return rightmost;
    }

    // check for bugs
    public void validate() {
        aggregateChildren(root);
    }

    // aggregate children and checks consistency, used by validate
    private BloomFilter aggregateChildren(BFINode node) {
        if (node.children == null) {
            return node.value;// nothing to check
        }


        BloomFilter current = StandardBloomFilter.EMPTY;

        for (BFINode c : node.children) {
            BloomFilter r = aggregateChildren(c);
            current = current.merge( r );
        }
        if (!node.value.getBitSet().equals(current.getBitSet()))
            throw new RuntimeException("bug "
                                       + node.children.size() + " "
                                       + current.getBitSet().cardinality() + " "
                                       + node.value.getBitSet().cardinality());
        return node.value;
    }

    @Override
    public void add(BloomFilter bf) {

        // create new BFINode for this BloomFilter
        BFINode newBFINode = new BFINode(bf, this.order,
                                               this.splitFull);

        // insert the ID - node mapping into the hashtable
        //this.idMap.put(bf.getID(), newBFINode);
        this.bfList.add(bf);

        // special case when this is the first child of the root
        if (root.children == null) {
            root.children = new ArrayList<BFINode>();

            // add the new child to the right
            root.children.add(newBFINode);

            newBFINode.parent = root;

            // update the value to be the current value or child
            // value
            root.value = root.value.merge(bf);

            // update stats
            stat.nbBFNodesAccessed++; // accessed the root
            stat.nbBFNodesAccessed++; // accessed the new node
            stat.nbBFAccessed += 2; // accessed root and new bloom
            // filter values

        } else {
            insert(root, newBFINode );

        }
    }

    /**
     * Insert a new child into sub-tree rooted at "current" node Return null
     * of pointer to new child if split occurred
     *
     * @param newChild
     * @return
     */
    // private BFINode insertRight(boolean isInBFI, BFINode current,
    // BFINode newChild, BFINode rightmost){
    private BFINode insert(BFINode current, BFINode newChild) {

        stat.nbBFNodesAccessed++;// current node accessed

        // if node is not leaf, need to direct the search for
        if (!current.isLeaf()) {
            // update the value of the current node, since it will
            // insert into that subtree
            current.value = current.value.merge(newChild.value);
//            stat.nbBFAccessed += 2; // current and new child values

            // find child closest to newChild and insert there
            BFINode closestChild = newChild.findClosest(
                                          current.children);
            // insert into that subtree
            BFINode newSibling = insert(closestChild, newChild);
            // if newSibling is null (no split), return null
            if (newSibling == null) {
                return null;
            }
            // there was a split;
            else {
                // check whether new root is needed
                if (current.parent == null) {
                    assert (current == root);
                    // root was split, create a new root
                    BFINode newRoot = new BFINode(
                        StandardBloomFilter.EMPTY,
                        this.order, this.splitFull);
                    newRoot.value = newRoot.value.merge( current.value )
                            .merge( newSibling.value );
                    newRoot.children = new ArrayList<BFINode>();
                    newRoot.children.add(current);
                    current.parent = newRoot;
                    newRoot.children.add(newSibling);
                    newSibling.parent = newRoot;
                    this.root = newRoot;

                    // update stats
                    stat.nbBFAccessed += 3;
                    stat.nbBFNodesAccessed += 3;
                    return null;
                }
                // if this is not the root
                else {
                    newSibling = insertEntryIntoParent(
                                     newSibling, current);
                    return newSibling;
                }
            } // end split or not
        }
        // if current is leaf, need to insert into its parent
        else {
            BFINode newSibling = insertEntryIntoParent(newChild,
                                    current);
            return newSibling;
        }

    }

    /**
     * Insert a new child in the parent of the provided node
     *
     * @param newChild
     * @param node
     * @return null, if the parent did not split, newNode otherwise
     */
    private BFINode insertEntryIntoParent(BFINode newChild,
            BFINode node) {
        // insert into the node's parent, after this node

        // System.out.println("CHECK: insertEntryIntoParent: " +
        // node.toString());
        // find the position of current node among its siblings
        int index = node.parent.children.indexOf(node);
        stat.nbBFNodesAccessed++; // access parent

        // System.out.println("CHECK: Found current node at position " +
        // index + " in the parent's children list");
        // insert the new child after this one
        node.parent.children.add(index + 1, newChild);
        newChild.parent = node.parent;
        stat.nbBFNodesAccessed += 2; // access parent and new sibling

        // check if split is needed
        stat.nbBFNodesAccessed++;
        if (!node.parent.needSplit()) {
            return null;
        }
        // else, need to split the node

        return split(node.parent);
    }

    private BFINode split(BFINode current) {
        // sanity check: current node should have 2*d +1 children
        assert current.children != null
        && current.children.size() >= 2 * this.order + 1 : "Split not needed in split"
        + current;

        BFINode newNode;
        BFINode newChild;
        // initialize the new BFINode with an all-zero bloom filter
        BloomFilter sampleFilter = current.value;
        BloomFilter zeroFilter = StandardBloomFilter.EMPTY;
        newNode = new BFINode(zeroFilter, this.order, this.splitFull);
        newNode.children = new ArrayList<BFINode>();

        stat.nbSplits++; // increase nb splits

        // insert the last half of the current children list into the
        // new node
        for (int i = this.order + 1; i < current.children.size(); i++) {
            // get the new child
            newChild = current.children.get(i);
            // add the new child to the right
            newNode.children.add(newChild);
            newChild.parent = newNode;

            newNode.value = newNode.value.merge( newChild.value );

        }

        stat.nbBFNodesAccessed += newNode.children.size() + 1; // update
        // parent
        // info
        stat.nbBFAccessed += newNode.children.size() + 1; // or current
        // child value

        // remove the last half of the children for the current node
        current.children.subList(this.order + 1,
                                 current.children.size()).clear();
        stat.nbBFNodesAccessed++; // accessed current
        // update the value of current node to be the or of its reduced
        // set of children
        current.recomputeValue();

        return newNode;
    }

    /**
     * Update the value of the current node and its ancestors to contain the
     * new value
     *
     * @param current
     * @param newValue
     */
    private void updateValueToTheRoot(BFINode current,
                                      BloomFilter newValue) {

        assert current != null;
        // update value of current node
        current.value = current.value.merge( newValue );
        stat.nbBFAccessed += 2;
        // if needed, recursively update the parent
        if (current.parent != null) {
            updateValueToTheRoot(current.parent, newValue);
        }
    }

    /**
     * Recompute all values from the current node to the root
     *
     * @param current
     * @param newValue
     */
    private void recomputeValueToTheRoot(BFINode current) {

        assert current != null;
        // update value of current node
        current.recomputeValue();

        // if needed, recursively update the parent
        if (current.parent != null) {
            recomputeValueToTheRoot(current.parent);
        }
    }

    /**
     * Split the current node and return the possibly new rightmost index
     * node
     *
     * @param root
     * @return
     */
    private BFINode splitRight(BFINode current, BFINode rightmost) {

        // sanity check: current node should have 2*d +1 children
        assert current.children != null
        && current.children.size() >= 2 * this.order + 1 : "Split not needed in splitRight"
        + current;

        stat.nbSplits++; // increase splits

        BFINode newNode;
        // initialize the new BFINode with an all-zero bloom filter
        BloomFilter sampleFilter = current.value;
        BloomFilter zeroFilter = StandardBloomFilter.EMPTY;
        newNode = new BFINode(zeroFilter, this.order, this.splitFull);

        // insert the last half of the current children list into the
        // new node
        BFINode receivedRight;
        for (int i = this.order + 1; i < current.children.size(); i++) {
            receivedRight = insertRight(false, newNode,
                                        current.children.get(i), rightmost);
            assert receivedRight == rightmost;
        }

        // remove the last half of the children for the current node
        current.children.subList(this.order + 1,
                                 current.children.size()).clear();

        stat.nbBFNodesAccessed++; // changed current children

        // update the value of current node to be the or of its reduced
        // set of children
        current.recomputeValue();

        // if current != root, insert the new sibling into the parent
        if (current.parent != null) {
            receivedRight = insertRight(true, current.parent,
                                        newNode, rightmost);
            assert receivedRight == rightmost; // because the
            // rightmost node
            // above the leaf
            // level should not
            // change when
        }
        // otherwise need to create a new root
        else {
            // assert that current is root
            assert current == this.root : "splitRight: Split of current with no parent but not root";

            BFINode newRoot;
            // initialize the new BFINode with an all-zero bloom
            // filter
            BloomFilter sampleFilter1 = current.value;
            BloomFilter zeroFilter1 = StandardBloomFilter.EMPTY;
            newRoot = new BFINode(zeroFilter1, this.order,
                                     this.splitFull);
            rightmost = insertRight(false, newRoot, current,
                                    rightmost);
            rightmost = insertRight(false, newRoot, newNode,
                                    rightmost);
            this.root = newRoot;

        }

        // update rightmost if current node that split was the rightmost
        // node
        if (current == rightmost) {
            rightmost = newNode;
        }

        return rightmost;
    }

    /**
     * TODO Show a representation of the BFI
     *
     * @return
     */
    @Override
    public String toString() {
        if (this.root == null) {
            return "null";
        }
        // return this.root.toString();
        return this.root.printTree();
    }


    @Override
    public List<BloomFilter> get(BloomFilter filter) {
        return findMatches(this.root, filter);
    }

    @Override
    public int count(BloomFilter filter) {
        return get(filter).size();
    }

    @Override
    public String getName() {
        return "Adina Bloofi";
    }


    /**
     * The node in a Bloom Filter Index
     */
    private class BFINode {

        BloomFilter value;
        int order1;
        BFINode parent; // need parent info since updates propagate up
        ArrayList<BFINode> children;

        // if splitFull is true, the condition for split is just the
        // number of children
        // otherwise, we use the "optimization" where we do not split
        // the nodes if
        // they are full, since the children might also be full
        boolean splitFull1 = true;

        BFINode(BloomFilter value, int order, boolean splitFull) {
            this.value = value;
            this.order1 = order;
            this.splitFull1 = splitFull;
            parent = null;
            children = null;
        }

        /**
         * Return the level of this node in the index - leafs have level
         * 0, root has highest level
         *
         * @return
         */
        int getLevel() {
            if (isLeaf()) {
                // if leaf, return 0
                return 0;
            } else {
                // compute the level of children and add 1
                BFINode child = this.children.get(0);
                int childLevel = child.getLevel();
                return 1 + childLevel;
            }
        }

        /**
         * Return the number of nodes in the subtree rooted at this node
         *
         * @return
         */
        int getTreeSize() {
            // if leaf, return 1
            if (isLeaf()) {
                return 1;
            }
            // else, computet the number of nodes in each subtree
            // and add them up
            else {
                int size = 1; // this node
                for (BFINode currentNode : this.children) {
                    size += currentNode.getTreeSize();
                }
                return size;
            }
        }

        /**
         * Return the number of bits in the Bloom filter
         *
         * @return
         */
        int getBloomFilterSize() {
            return config.getNumberOfBits();
        }

        /**
         * Recompute the value of the BloomFilter to be the or of its
         * children
         */
        void recomputeValue() {

            assert this.value != null : "value in recomputeValue is null ";

            this.value = StandardBloomFilter.EMPTY;
            for (BFINode currentNode : this.children) {
                this.value = this.value.merge( currentNode.value);
                stat.nbBFAccessed++; // used current value
            }
            stat.nbBFAccessed++; // computed this value

        }

        /**
         * Return true if this node is a leaf-level node (no children)
         * and false otherwise
         *
         * @return true is no children, false otherwise
         */
        public boolean isLeaf() {
            return this.children == null || this.children.isEmpty();
        }

        /**
         * Return true if a split is needed: the number of children is
         * at least 2*order +2 and value is not all 1s
         *
         * @return
         */
        public boolean needSplit() {

            if (splitFull1) {
                return !(this.children == null || this.children
                         .size() <= 2 * this.order1);
            } else {
                return !(this.children == null
                         || this.children.size() <= 2 * this.order1 ||
                         config.isFull(this.value));
            }
        }

        /**
         * Return true if this is not the root (a root never needs
         * merge) and the number of children is less than order + 1
         *
         * @return
         */
        public boolean needMerge() {
            return this.parent != null
                   && this.children.size() < this.order1;
        }

        public boolean canRedistribute() {
            return this.children.size() > this.order1;
        }

        /**
         * Find the BFINode in the list with the BloomFilter value
         * "closest" to this value. This is used to direct the search
         * during insert. If the distance between this bloom filter and
         * several filters in the list is the same, it should return one
         * of the closest filters, at random
         *
         * @param nodeList
         * @return
         */
        public BFINode findClosest(ArrayList<BFINode> nodeList) {

            int index = findClosestIndex(nodeList);
            if (index >= 0) {
                return nodeList.get(index);
            } else {
                return null;
            }

        }

        /**
         * Find the index of BFINode in the list with the BloomFilter
         * value "closest" to this value. This is used to direct the
         * search during insert. If the distance between this bloom
         * filter and several filters in the list is the same, it should
         * return one of the closest filters, at random
         *
         * @param list
         *                of BFINodes to compare with
         * @return
         */
        private int findClosestIndex(ArrayList<BFINode> nodeList) {

            assert nodeList != null : "Empty list in BFINode findCLosest";

            // return null if no element to compare with
            if (nodeList.isEmpty())
                return -1;

            BFINode currentNode;

            // initialize min distance to be distance to first
            // element
            currentNode = nodeList.get(0);
            double minDistance = this.value.distance(currentNode.value);
            stat.nbBFAccessed += 2; // this value and currentNode
            // value
            int minIndex = 0;
            double currentDistance;

            // loop through all elements to find the closest
            for (int i = 1; i < nodeList.size(); i++) {
                currentNode = nodeList.get(i);
                currentDistance = this.value.distance(currentNode.value);
                stat.nbBFAccessed += 2; // this value and
                // currentNode value

                // replace current min if found a smaller one,
                // or if same, randomly replace the current one
                // TODO: might need to work on that probability:
                // if x nodes are at the same distance to
                // this.value, each node should be returned with
                // prob 1/x
                if (currentDistance < minDistance
                        || (minDistance - currentDistance < 0.00001 && Math
                            .random() < 1.0 / nodeList
                            .size())) {
                    minDistance = currentDistance;
                    minIndex = i;
                }

            }

            return minIndex;

        }

        @Override
        public String toString() {
            return this.value.toString();
        }

        /**
         * Print this node and the sub-tree rooted at this node
         *
         * @return
         */
        public String printTree() {

            // print value
            String output = "\n" + this.value.toString();
            // if no children, return
            if (this.children == null || this.children.isEmpty()) {
                return output;
            }

            output += "\n(";
            BFINode currentNode;
            // else recursively print the children sub-trees
            for (int i = 0; i < this.children.size(); i++) {
                currentNode = this.children.get(i);
                output += currentNode.printTree();
            }
            output += "\n)";

            return output;
        }
    }


}