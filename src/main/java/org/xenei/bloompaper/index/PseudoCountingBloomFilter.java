package org.xenei.bloompaper.index;

import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BitCountProducer;
import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.LongBiPredicate;
import org.apache.commons.collections4.bloomfilter.Shape;

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
    public Shape getShape() {
        return delegate.getShape();
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
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean forEachCount(BitCountConsumer action) {
        BitCountProducer producer = (delegate instanceof CountingBloomFilter) ? ((CountingBloomFilter) delegate)
                : BitCountProducer.from(delegate);
        return producer.forEachCount(action);
    }

    @Override
    public boolean isSparse() {
        return delegate.isSparse();
    }

    @Override
    public boolean contains(IndexProducer indexProducer) {
        return delegate.contains(indexProducer);
    }

    @Override
    public boolean contains(BitMapProducer bitMapProducer) {
        return delegate.contains(bitMapProducer);
    }

    @Override
    public boolean mergeInPlace(BloomFilter other) {
        throw new UnsupportedOperationException();
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
    public boolean add(BitCountProducer other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean subtract(BitCountProducer other) {
        throw new UnsupportedOperationException();
    }

    /**
     * Clones the filter.  Used to create merged values.
     * @return A clone of this filter.
     */
    protected ArrayCountingBloomFilter makeClone() {
        ArrayCountingBloomFilter filter = new ArrayCountingBloomFilter(delegate.getShape());
        filter.add(this);
        return filter;
    }

    @Override
    public CountingBloomFilter merge(BloomFilter other) {
        Objects.requireNonNull(other, "other");
        CountingBloomFilter filter = makeClone();
        filter.add(BitCountProducer.from(other));
        return filter;
    }

    @Override
    public CountingBloomFilter merge(Hasher hasher) {
        Objects.requireNonNull(hasher, "hasher");
        ArrayCountingBloomFilter filter = makeClone();
        filter.add(BitCountProducer.from(hasher.indices(delegate.getShape())));
        return filter;
    }

    @Override
    public BloomFilter copy() {
        return new PseudoCountingBloomFilter( delegate.copy() );
    }

    @Override
    public LongPredicate makePredicate(LongBiPredicate func) {
        return delegate.makePredicate(func);
    }

    @Override
    public long[] asBitMapArray() {
        return delegate.asBitMapArray();
    }

    @Override
    public int[] asIndexArray() {
        return delegate.asIndexArray();
    }

    @Override
    public boolean mergeInPlace(Hasher hasher) {
        return delegate.mergeInPlace(hasher);
    }

    @Override
    public boolean isFull() {
        return delegate.isFull();
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



}
