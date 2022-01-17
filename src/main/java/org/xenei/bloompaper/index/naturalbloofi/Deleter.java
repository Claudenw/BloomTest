package org.xenei.bloompaper.index.naturalbloofi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class Deleter implements Predicate<Node> {
    private final IntConsumer baseConsumer;
    private final long[] bitMap;
    private Node found;

    public Deleter(IntConsumer consumer, BloomFilter filter) {
        this.baseConsumer = consumer;
        this.bitMap = BloomFilter.asBitMapArray(filter);
        this.found = null;
    }

    public void cleanup() {
        if (found != null && found.getParent() != null) {
            found.getParent().removeChild(found);
        }
    }

    @Override
    public boolean test(Node node) {
        if (node.contains( bitMap )) {
            if (0 == Node.BM_COMPARATOR.compare(bitMap , node.bitMap)) {
                Iterator<Integer> idIterator = node.getIds().iterator();
                baseConsumer.accept( idIterator.next() );
                idIterator.remove();
                if (!idIterator.hasNext()) {
                    found = node;
                }
                return false;
            } else {
                return node.testChildren( this );
            }
        }
        return true;
    }
}
