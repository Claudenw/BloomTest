package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.hamming.Node;

public class TestDiff {
    int n = 3;
    double p = 1.0 / 100000;
    Shape shape = Shape.Factory.fromNP(n, p);

    // NumericBloomFilter filter = new NumericBloomFilter( shape,
    // 2315448351954444800L, 34 );
    // NumericBloomFilter target = new NumericBloomFilter( shape,
    // -5570068171581312439L, 34 );
    FrozenBloomFilter filter = new FrozenBloomFilter(shape,
            BitMapProducer.fromLongArray(new long[] { 5764629513350676800L }));
    FrozenBloomFilter target = new FrozenBloomFilter(shape,
            BitMapProducer.fromLongArray(new long[] { -3025718536694661252L }));

    public static void main(String[] args) {
        new TestDiff().execute();
    }

    public void execute() {
        Node tNode = new Node(target);
        Node fNode = new Node(filter);
        System.out.println("TcF=" + target.contains(filter));
        System.out.println("FcT=" + filter.contains(target));

        System.out.println("Target: " + tNode);
        target.forEachIndex((x) -> {System.out.print(String.format("%s ", x)); return true;});
        System.out.println();

        System.out.println("Filter: " + fNode);
        filter.forEachIndex((x) -> {System.out.print(String.format("%s ", x)); return true;});
        System.out.println();
    }
}
