package org.xenei.bloompaper.index.hamming;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.FrozenBloomFilter;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BFHamming {

    private TreeSet<Node> index = new TreeSet<Node>();
    public List<FrozenBloomFilter> found;

    public BFHamming(Shape shape) {
        Node.setEmpty(shape);
    }

    public void add(BloomFilter filter) {
        Node node = new Node(filter);
        SortedSet<Node> tailSet = index.tailSet(node);
        if (tailSet.isEmpty() || !node.equals(tailSet.first())) {
            tailSet.add(node);
        } else {
            tailSet.first().increment();
        }
    }

    public boolean delete(BloomFilter filter) {
        Node node = new Node(filter);
        SortedSet<Node> tailSet = index.tailSet(node);
        if (!tailSet.isEmpty() && node.equals(tailSet.first())) {
            if (tailSet.first().decrement()) {
                tailSet.remove(tailSet.first());
            }
            return true;
        }
        return false;
    }

    /**
     * Finds the matching nodes
     * @param result
     * @param filter
     * @return
     */
    public void search(Consumer<BloomFilter> result, BloomFilter filter) {

        Node node = new Node(filter);
        // int retval = 0;

        SortedSet<Node> tailSet = index.tailSet(node);
        if (tailSet.isEmpty()) {
            return;
        }
        if (node.equals(tailSet.first())) {
            tailSet.first().getCount(result);
        }

        Node lowerLimit = node.lowerLimitNode();
        Node upperLimit;

        while (lowerLimit.compareTo(index.last()) <= 0) {
            upperLimit = lowerLimit.upperLimitNode();
            tailSet.tailSet(lowerLimit).headSet(upperLimit).stream().filter(n -> n.getFilter().contains(filter))
                    .forEach((n) -> n.getCount(result));
            lowerLimit = upperLimit.lowerLimitNode();
        }
    }

    public int scan(BloomFilter bf) {
        BloomIndex.Incrementer incr = new BloomIndex.Incrementer();
        index.stream().map((n) -> n.getFilter()).filter((f) -> f.contains(bf)).forEach(incr);
        return incr.count;

    }

}
