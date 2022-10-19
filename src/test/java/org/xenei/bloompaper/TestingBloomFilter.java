package org.xenei.bloompaper;

import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.LongBiPredicate;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.EnhancedDoubleHasher;

/**
 * An arbitrary bloom filter.
 *
 * Creates arbitrary Bloom filters that are unique within the current execution.
 * Will repeat after 2^64 calls.
 *
 */
public class TestingBloomFilter implements BloomFilter {
    private SimpleBloomFilter delegate;
    private static int counter = Integer.MIN_VALUE;
    private static int pfx = 1;

    /**
     * Constructor.
     */
    public TestingBloomFilter(Shape shape) {
        delegate= new SimpleBloomFilter(shape);
        delegate.merge(new EnhancedDoubleHasher(Integer.toUnsignedLong(pfx), Integer.toUnsignedLong(counter)));
        counter++;
        if (counter == Integer.MIN_VALUE) {
            pfx++;
        }
    }

    @Override
    public boolean contains(BloomFilter other) {
        return delegate.contains(other);
    }

    @Override
    public boolean contains(Hasher hasher) {
        return delegate.contains(hasher);
    }

    @Override
    public int[] asIndexArray() {
        return delegate.asIndexArray();
    }

    @Override
    public long[] asBitMapArray() {
        return delegate.asBitMapArray();
    }

    @Override
    public boolean contains(BitMapProducer bitMapProducer) {
        return delegate.contains(bitMapProducer);
    }

    @Override
    public boolean forEachBitMapPair(BitMapProducer other, LongBiPredicate func) {
        return delegate.forEachBitMapPair(other, func);
    }

    @Override
    public SimpleBloomFilter copy() {
        return delegate.copy();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean merge(IndexProducer indexProducer) {
        return delegate.merge(indexProducer);
    }

    @Override
    public boolean merge(BitMapProducer bitMapProducer) {
        return delegate.merge(bitMapProducer);
    }
    @Override
    public boolean merge(BloomFilter other) {
        return delegate.merge(other);
    }

    @Override
    public boolean merge(Hasher hasher) {
        return delegate.merge(hasher);
    }

    @Override
    public Shape getShape() {
        return delegate.getShape();
    }

    @Override
    public int characteristics () {
        return delegate.characteristics();
    }

    @Override
    public int cardinality() {
        return delegate.cardinality();
    }

    @Override
    public boolean contains(IndexProducer indexProducer) {
        return delegate.contains(indexProducer);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public boolean isFull() {
        return delegate.isFull();
    }

    @Override
    public boolean forEachIndex(IntPredicate consumer) {
        return delegate.forEachIndex(consumer);
    }

    @Override
    public boolean forEachBitMap(LongPredicate consumer) {
        return delegate.forEachBitMap(consumer);
    }

    @Override
    public int estimateN() {
        return delegate.estimateN();
    }

    @Override
    public int estimateUnion(BloomFilter other) {
        return delegate.estimateUnion(other);
    }

    @Override
    public int estimateIntersection(BloomFilter other) {
        return delegate.estimateIntersection(other);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
