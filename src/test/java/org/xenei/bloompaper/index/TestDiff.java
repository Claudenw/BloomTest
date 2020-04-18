package org.xenei.bloompaper.index;

import java.util.Iterator;


import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.function.Murmur128x86Cyclic;
import org.xenei.bloompaper.index.FrozenBloomFilter;
import org.xenei.bloompaper.index.hamming.Node;

public class TestDiff {
    int n = 3;
    double p = 1.0 / 100000;
    Shape shape = new Shape(new Murmur128x86Cyclic(), n, p);

//    NumericBloomFilter filter = new NumericBloomFilter( shape, 2315448351954444800L, 34 );
//    NumericBloomFilter target = new NumericBloomFilter( shape, -5570068171581312439L, 34  );
  FrozenBloomFilter filter = new FrozenBloomFilter( shape, 5764629513350676800L );
  FrozenBloomFilter target = new FrozenBloomFilter( shape, -3025718536694661252L  );

    public static void main(String[] args) {
        new TestDiff().execute();
    }

    public void execute() {
        Node tNode = new Node( target );
        Node fNode = new Node( filter );
        System.out.println( "TcF="+target.contains( filter ));
        System.out.println( "FcT="+filter.contains( target ));

        System.out.println( "Target: "+tNode );
        Iterator<Integer> iter = target.getHasher().getBits(shape);
        while (iter.hasNext() ) {
            System.out.print( String.format( "%s ", iter.next()));
        }
        System.out.println();

        System.out.println( "Filter: "+fNode );
        iter = filter.getHasher().getBits(shape);
        while (iter.hasNext() ) {
            System.out.print( String.format( "%s ", iter.next()));
        }
        System.out.println();
    }
}
