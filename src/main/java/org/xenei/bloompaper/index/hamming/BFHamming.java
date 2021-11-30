package org.xenei.bloompaper.index.hamming;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.FrozenBloomFilter;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BFHamming  {

    private TreeSet<Node> index = new TreeSet<Node>();
    public List<FrozenBloomFilter> found;
    private Collection<BloomFilter> filterCapture;

    public BFHamming(Shape shape) {
        Node.setEmpty( shape );
    }

    public void add(BloomFilter filter) {
        Node node = new Node( filter );
        SortedSet<Node> tailSet = index.tailSet( node );
        if (tailSet.isEmpty() || ! node.equals(tailSet.first()))
        {
            tailSet.add( node );
        }
        else {
            tailSet.first().increment();
        }
    }


    public boolean delete(BloomFilter filter) {
        Node node = new Node( filter );
        SortedSet<Node> tailSet = index.tailSet( node );
        if (!tailSet.isEmpty() && node.equals( tailSet.first()))
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
        if (node.equals(tailSet.first()))
        {
            filterCapture.add( tailSet.first().getFilter() );
            retval += tailSet.first().getCount();
        }

        Node lowerLimit = node.lowerLimitNode();
        Node upperLimit;

        while ( lowerLimit.compareTo( index.last() ) <= 0)
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
