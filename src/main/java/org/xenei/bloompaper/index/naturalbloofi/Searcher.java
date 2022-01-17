package org.xenei.bloompaper.index.naturalbloofi;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class Searcher implements Consumer<Node> {
    private final Consumer<Node> baseConsumer;
    private final long[] target;
    private final double log;

    public Searcher(Consumer<Node> baseConsumer, BloomFilter filter) {
        this.baseConsumer = baseConsumer;
        this.target = BloomFilter.asBitMapArray(filter);
        this.log = Node.getApproximateLog( Node.getApproximateLogExponents( filter ));
    }

    @Override
    public void accept(Node node) {
        if (node.getLog() >= log && node.contains( target )) {
            baseConsumer.accept( node );
            node.forChildren( this );
        }
    }
}
