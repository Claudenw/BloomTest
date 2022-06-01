package org.xenei.bloompaper;

import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.SimpleHasher;

/**
 * An arbitrary bloom filter.
 *
 * Creates arbitrary Bloom filters that are unique within the current execution.
 * Will repeat after 2^64 calls.
 *
 */
public class TestingBloomFilter extends SimpleBloomFilter {

    private static int counter = Integer.MIN_VALUE;
    private static int pfx = 1;

    /**
     * Constructor.
     */
    public TestingBloomFilter(Shape shape) {
        super(shape, new SimpleHasher(Integer.toUnsignedLong(pfx), Integer.toUnsignedLong(counter)));
        counter++;
        if (counter == Integer.MIN_VALUE) {
            pfx++;
        }
    }

}
