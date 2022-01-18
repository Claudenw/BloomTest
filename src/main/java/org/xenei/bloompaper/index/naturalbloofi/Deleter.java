package org.xenei.bloompaper.index.naturalbloofi;

import java.util.Iterator;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class Deleter implements Predicate<Node> {
    private final IntConsumer baseConsumer;
    final Node target;
    private Node found;

    public Deleter(IntConsumer consumer, BloomFilter filter) {
        this.baseConsumer = consumer;
        this.target = new Node(null, filter, -3);
        this.found = null;
    }

    public void cleanup() {
        if (found != null && found.getParent() != null) {
            found.getParent().removeChild(found);
        }
    }

    @Override
    public boolean test(Node node) {
        if (node.contains(target)) {
            if (0 == Node.BM_COMPARATOR.compare(target.bitMap, node.bitMap)) {
                Iterator<Integer> idIterator = node.getIds().iterator();
                baseConsumer.accept(idIterator.next());
                idIterator.remove();
                if (!idIterator.hasNext()) {
                    found = node;
                }
                return false;
            } else {
                return node.testChildren(target, this);
            }
        }
        return true;
    }
}
