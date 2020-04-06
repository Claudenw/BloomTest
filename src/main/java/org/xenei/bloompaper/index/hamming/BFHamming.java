package org.xenei.bloompaper.index.hamming;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.hamming.Node.NodeComparator;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BFHamming  {

    TreeSet<Node> index = new TreeSet<Node>( NodeComparator.COMPLETE );

    public BFHamming() {
    }

    private boolean equals(Node n1, Node n2) {
        return NodeComparator.COMPLETE.compare(n1, n1) == 0;
    }

    public void add(BloomFilter filter) {
        Node node = new Node( filter );
        SortedSet<Node> tailSet = index.tailSet( node );
        if (tailSet.isEmpty() || ! equals( node, tailSet.first()))
        {
            // not found
            index.add( node );
        }
        else {
            tailSet.first().merge( node );
        }
    }


    public boolean delete(BloomFilter filter) {
        Node node = new Node( filter );
        SortedSet<Node> tailSet = index.tailSet( node );
        if (!tailSet.isEmpty() && equals( node, tailSet.first()))
        {
            tailSet.first().decrement();
            if (tailSet.first().getCount() == 0)
            {
                tailSet.remove(tailSet.first());
                return true;
            }
        }
        return false;
    }

    public int count(BloomFilter filter) {
        int retval = 0;

        Node node = new Node(filter);
        SortedSet<Node> tailSet = index.tailSet( new Node( filter ));
        if (tailSet.isEmpty()) {
            return 0;
        }
        if (equals( node, tailSet.first()))
        {
            retval += tailSet.first().getCount();
        }

        Node lowerLimit = node.lowerLimitNode();
        Node upperLimit;
        while (NodeComparator.COMPLETE.compare( index.last(), lowerLimit ) <= 0)
        {
            upperLimit = lowerLimit.upperLimitNode();
            retval += tailSet.tailSet( lowerLimit ).headSet( upperLimit ).stream()
                    .filter( n -> n.getFilter().contains( filter ))
                    .mapToInt( n -> n.getCount() )
                    .sum();
            lowerLimit = upperLimit.lowerLimitNode();
        }
        return retval;
    }



}
