package org.xenei.bloompaper.index;

import java.util.PrimitiveIterator.OfInt;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;

/**
 * A Counting filter implementation on a regular bloom filter.
 * This counting bloom filter returns 1 as the count for every bit enabled.
 * It does not support remove, add, or subtract operations and will throw
 * UnsupportedOperationExceptions for those operations.
 */
public class PseudoCountingBloomFilter implements CountingBloomFilter {

    private final BloomFilter delegate;

    public PseudoCountingBloomFilter(BloomFilter filter) {
        delegate = filter;
    }

    @Override
    public int andCardinality(BloomFilter other) {
        return delegate.andCardinality(other);
    }

    @Override
    public int cardinality() {
        return delegate.cardinality();
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
    public long[] getBits() {
        return delegate.getBits();
    }

    @Override
    public StaticHasher getHasher() {
        return delegate.getHasher();
    }

    @Override
    public Shape getShape() {
        return delegate.getShape();
    }

    @Override
    public int orCardinality(BloomFilter other) {
        return delegate.orCardinality(other);
    }

    @Override
    public int xorCardinality(BloomFilter other) {
        return delegate.xorCardinality(other);
    }

    @Override
    public void merge(BloomFilter other) {
        delegate.merge(other);
    }

    @Override
    public void merge(Hasher other) {
        delegate.merge( other );
    }

    @Override
    public boolean remove(BloomFilter other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Hasher hasher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(CountingBloomFilter other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean subtract(CountingBloomFilter other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void forEachCount(BitCountConsumer action) {
        if (delegate instanceof CountingBloomFilter) {
            ((CountingBloomFilter) delegate).forEachCount(action);
        } else {
            OfInt iter = getHasher().getBits(getShape());
            while (iter.hasNext()) {
                action.accept(iter.nextInt(), 1);
            }
        }
    }

}
