package org.xenei.bloompaper.index.flatbloofi;

import static org.junit.Assert.assertEquals;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.junit.Test;
import org.xenei.bloompaper.RandomBloomFilter;
import org.xenei.bloompaper.index.FrozenBloomFilter;

public class FlatBloofiTest {

    @Test
    public void testDelete()
    {
        int n = 3;
        double p = 1.0 / 100000;
        Shape shape = Shape.Factory.fromNP( n, p);

        FlatBloofi flatBloofi = new FlatBloofi(500, shape);

        BloomFilter bf = new FrozenBloomFilter(shape, BitMapProducer.fromLongArray(new long[] { 1, 1 }));
        flatBloofi.add( bf );
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));

        assertEquals( 6, flatBloofi.count() );
        flatBloofi.delete( bf );
        assertEquals( 5, flatBloofi.count() );

    }

    @Test
    public void testDuplicateDelete()
    {
        int n = 3;
        double p = 1.0 / 100000;
        Shape shape = Shape.Factory.fromNP( n, p);

        FlatBloofi flatBloofi = new FlatBloofi(500, shape);

        BloomFilter bf = new FrozenBloomFilter(shape, BitMapProducer.fromLongArray( new long[] { 1, 1 }));
        flatBloofi.add( bf );
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( bf );
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));
        flatBloofi.add( new RandomBloomFilter(shape));

        assertEquals( 7, flatBloofi.count() );
        flatBloofi.delete( bf );
        assertEquals( 6, flatBloofi.count() );

    }

}
