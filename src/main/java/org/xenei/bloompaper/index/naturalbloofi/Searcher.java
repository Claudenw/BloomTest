package org.xenei.bloompaper.index.naturalbloofi;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class Searcher implements Consumer<Node> {
    private final Consumer<Node> baseConsumer;
    final Node target;

    public Searcher(Consumer<Node> baseConsumer, BloomFilter filter) {
        this.baseConsumer = baseConsumer;
        target = new Node(null, filter, -2);
    }

    @Override
    public void accept(Node node) {
        if (node.getLog() >= target.getLog() && node.contains(target)) {
            baseConsumer.accept(node);
            node.searchChildren(target, this);
        }
    }
}
