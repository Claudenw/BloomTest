package org.xenei.bloompaper.index.naturalbloofi;

import java.io.PrintStream;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.index.BloomIndex;

public class NaturalBloofi extends BloomIndex {

    private Node root;
    private int id;
    private int count;

    public NaturalBloofi(int population, Shape shape) {
        super(population, shape);
        root = new Node(null, null, -1);
        id = 0;
        count = 0;
    }

    @Override
    public void add(BloomFilter filter) {
        new Node(root, filter, id++);
        count++;
    }

    @Override
    public void delete(BloomFilter filter) {
        Deleter deleter = new Deleter(n -> count--, filter);
        root.testChildren(deleter.target, deleter);
        deleter.cleanup();
    }

    private void mapper(Shape shape, Node n, Consumer<BloomFilter> consumer) {
        BloomFilter bf = new SimpleBloomFilter(shape, BitMapProducer.fromLongArray(n.bitMap));
        for (@SuppressWarnings("unused")
        int i : n.getIds()) {
            consumer.accept(bf);
        }
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        Searcher searcher = new Searcher(n -> mapper(filter.getShape(), n, consumer), filter);
        root.searchChildren(searcher.target, searcher);
    }

    @Override
    public String getName() {
        return "NaturalBlooofi";
    }

    @Override
    public int count() {
        return count;
    }

    public void graph(PrintStream out) {
        Consumer<Node> grapher = new Consumer<Node>() {

            @Override
            public void accept(Node t) {
                if (t.getParent() != null) {
                    if (t.getParent().getId() != -1) {
                        out.format("%s -> %s%n", t.getParent().getId(), t.getId());
                    } else {
                        if (t.hasChildren()) {
                            out.format("%s -> %s%n", t.getParent().getId(), t.getId());

                        }
                    }
                }
            }
        };
        root.walkTree(grapher);
    }

    private void checkPrint(long[] o1, long[] o2, int id1, int id2) {
        if (id1 != id2 && Node.BM_COMPARATOR.compare(o1, o2) == 0) {
            System.out.format("%s => %s%n,", id1, id2);
        }
    }

    public void report(PrintStream out) {
        // graph( out );
        Consumer<Node> rowMaker = new Consumer<Node>() {
            private Node root = null;

            @Override
            public void accept(Node t) {
                if (root == null) {
                    root = t.getParent();
                    while (root.getParent() != null) {
                        root = root.getParent();
                    }
                }
                root.forChildren((Consumer<Node>) n -> checkPrint(n.bitMap, t.bitMap, n.getId(), t.getId()));
            }

        };
        root.forChildren(rowMaker);
    }
}
