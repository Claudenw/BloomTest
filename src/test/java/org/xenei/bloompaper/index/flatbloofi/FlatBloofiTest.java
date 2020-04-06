package org.xenei.bloompaper.index.flatbloofi;

import static org.junit.Assert.assertEquals;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.function.Murmur128x86Cyclic;
import org.junit.Test;
import org.xenei.bloompaper.RandomBloomFilter;
import org.xenei.bloompaper.index.NumericBloomFilter;

public class FlatBloofiTest {

    @Test
    public void testDelete()
    {
        int n = 3;
        double p = 1.0 / 100000;
        Shape shape = new Shape(new Murmur128x86Cyclic(), n, p);

        FlatBloofi flatBloofi = new FlatBloofi(500, shape);

        BloomFilter bf = new NumericBloomFilter(shape, new long[] { 1, 1 });
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
        Shape shape = new Shape(new Murmur128x86Cyclic(), n, p);

        FlatBloofi flatBloofi = new FlatBloofi(500, shape);

        BloomFilter bf = new NumericBloomFilter(shape, new long[] { 1, 1 });
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
