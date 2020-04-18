package org.xenei.bloompaper.index.hamming;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.index.FrozenBloomFilter;
import org.xenei.bloompaper.index.hamming.Node.NodeComparator;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BFHamming  {

    private TreeSet<Node> index = new TreeSet<Node>( NodeComparator.COMPLETE );
    public List<FrozenBloomFilter> found;
    private Collection<BloomFilter> filterCapture;



    public BFHamming(Shape shape) {
        Node.setEmpty( shape );
    }

    private boolean equals(Node n1, Node n2) {
        return NodeComparator.COMPLETE.compare(n1, n2) == 0;
    }

    public void add(BloomFilter filter) {
        Node node = new Node( filter );
        SortedSet<Node> tailSet = index.tailSet( node );
        if (tailSet.isEmpty() || ! equals( node, tailSet.first()))
        {
            tailSet.add( node );
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
            if (tailSet.first().decrement())
            {
                tailSet.remove(tailSet.first());
            }
            return true;
        }
        return false;
    }

    public int count(BloomFilter filter) {
        int retval = 0;

        Node node = new Node(filter);

        SortedSet<Node> tailSet = index.tailSet( node );
        if (tailSet.isEmpty()) {
            return 0;
        }
        if (equals( node, tailSet.first()))
        {
            filterCapture.add( tailSet.first().getFilter() );
            retval += tailSet.first().getCount();
        }

        Node lowerLimit = node.lowerLimitNode();
        Node upperLimit;

        while (NodeComparator.COMPLETE.compare( lowerLimit, index.last() ) <= 0)
        {
            upperLimit = lowerLimit.upperLimitNode();
            retval += tailSet.tailSet( lowerLimit ).headSet( upperLimit ).stream()
                    .filter( n -> n.getFilter().contains( filter ))
                    .filter( n -> filterCapture.add( n.getFilter() ))
                    .mapToInt( n -> n.getCount() )
                    .sum();
            lowerLimit = upperLimit.lowerLimitNode();
        }
        return retval;
    }

    public int scan( BloomFilter bf )
    {
        int count = 0;
        for (Node test : index )
        {
            if ( test.getFilter().contains( bf ))
            {
                count += test.getCount();
            }
        }
        return count;

    }

    public void setFilterCapture(Collection<BloomFilter> collection) {
        filterCapture = collection;
    }
}
