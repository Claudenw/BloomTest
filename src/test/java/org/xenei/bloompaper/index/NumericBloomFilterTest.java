package org.xenei.bloompaper.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.junit.Test;
import org.xenei.bloompaper.RandomBloomFilter;

public class NumericBloomFilterTest {
    int n = 3;
    double p = 1.0 / 100000;
    Shape shape = Shape.Factory.fromNP(n, p);

    @Test
    public void hashCodeTest() {
        FrozenBloomFilter target = new FrozenBloomFilter(shape,
                BitMapProducer.fromLongArray(new long[] { -3025718536694661252L }));
        assertEquals(target.hashCode(), target.hashCode());

        FrozenBloomFilter two = new FrozenBloomFilter(shape,
                BitMapProducer.fromLongArray(new long[] { -3025718536694661252L }));
        assertEquals(target.hashCode(), two.hashCode());

    }

    @Test
    public void collectionTest() {

        FrozenBloomFilter target = new FrozenBloomFilter(shape,
                BitMapProducer.fromLongArray(new long[] { -3025718536694661252L }));
        Set<FrozenBloomFilter> set = new HashSet<FrozenBloomFilter>();

        set.add(FrozenBloomFilter.makeInstance(new RandomBloomFilter(shape)));
        set.add(target);
        set.add(FrozenBloomFilter.makeInstance(new RandomBloomFilter(shape)));

        assertTrue("Target missing from set", set.contains(target));

    }

    @Test
    public void defensiveCopyTest() {
        long bits[] = new long[1];
        bits[0] = -3025718536694661252L;

        FrozenBloomFilter target = new FrozenBloomFilter(shape, BitMapProducer.fromLongArray(bits));

        int hash = target.hashCode();

        bits[0] = 0;

        assertEquals("Hash Changed", hash, target.hashCode());

        assertEquals(target.hashCode(), target.hashCode());

    }
}
