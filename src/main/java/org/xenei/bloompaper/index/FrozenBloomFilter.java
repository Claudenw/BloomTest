package org.xenei.bloompaper.index;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.LongBiPredicate;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;

/**
 * A Bloom filter is 'frozen' so no modifications can be made on it.
 * <p>
 * This filter is immutable, any attempt
 * to change it throws an UnsupportedOperationException.
 * </p><p>
 * This filter implements hashCode() and equals() and is suitable for
 * use in Hash based collections.
 * </p>
 *
 */
public class FrozenBloomFilter implements BloomFilter, Comparable<FrozenBloomFilter> {
    private final BloomFilter wrapped;
    private final int cardinality;
    private long[] bitMap;

    /**
     * Method to create a frozen filter from a standard filter.
     * If the standard filter is already a FrozenBloomFilter then
     * it is returned unchanged.
     * @param bf the Bloom filter to freeze
     * @return a FrozenBloomFilter.
     */
    public static FrozenBloomFilter makeInstance(BloomFilter bf) {
        if (bf instanceof FrozenBloomFilter) {
            return (FrozenBloomFilter) bf;
        }
        return new FrozenBloomFilter(bf);
    }

    /**
     * Creates a new Frozen bloom filter from another filter.
     * @param bf The other bloom filter.
     */

    private FrozenBloomFilter(BloomFilter bf) {
        wrapped = bf;
        cardinality = bf.cardinality();
    }

    /**
     * Create a new Frozen bloom filter from a shape and a BitMapProducer.
     * @param shape the Shape of the filter.
     * @param producer the BitMapProducer to create the bitmaps.
     */
    public FrozenBloomFilter(Shape shape, BitMapProducer producer) {
        this(new SimpleBloomFilter(shape, producer));
    }

    /**
     * Get the Bitmap as an array of long.
     * @return the BitMaps
     */
    @Override
    public long[] asBitMapArray() {
        if (bitMap == null) {
            bitMap = wrapped.asBitMapArray();
        }
        return bitMap;
    }

    @Override
    public boolean mergeInPlace(Hasher hasher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean mergeInPlace(BloomFilter other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FrozenBloomFilter ? compareTo((FrozenBloomFilter) obj) == 0 : false;
    }

    @Override
    public int hashCode() {
        return getShape().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("FrozenBloomFilter %s, [ ", getShape()));
        forEachBitMap(l -> {
            sb.append(String.format("0x%08x, ", l));
            return true;
        });
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int compareTo(FrozenBloomFilter other) {
        if (this == other) {
            return 0;
        }
        int result = Integer.compare(getShape().getNumberOfBits(), other.getShape().getNumberOfBits());
        if (result == 0) {
                    result = Integer.compare(getShape().getNumberOfHashFunctions(), other.getShape().getNumberOfHashFunctions());
        }
        if (result == 0) {
            long[] oBitMap = other.asBitMapArray();
            int limit = asBitMapArray().length < oBitMap.length ? asBitMapArray().length : oBitMap.length;
            int i = 0;
            while (i < limit && result == 0) {
                result = Long.compare(bitMap[i], oBitMap[i]);
                i++;
            }
            if (result == 0) {
                result = Integer.compare(bitMap.length, oBitMap.length);
            }
        }
        return result;
    }

    @Override
    public boolean forEachIndex(IntPredicate consumer) {
        return wrapped.forEachIndex(consumer);
    }

    @Override
    public boolean forEachBitMap(LongPredicate consumer) {
        return wrapped.forEachBitMap(consumer);
    }

    @Override
    public boolean isSparse() {
        return wrapped.isSparse();
    }

    @Override
    public Shape getShape() {
        return wrapped.getShape();
    }

    @Override
    public boolean contains(IndexProducer indexProducer) {
        return wrapped.contains(indexProducer);
    }

    @Override
    public boolean contains(BitMapProducer bitMapProducer) {
        return wrapped.contains(bitMapProducer);
    }

    @Override
    public boolean isFull() {
        return wrapped.isFull();
    }

    @Override
    public int cardinality() {
        return cardinality;
    }

    @Override
    public BloomFilter copy() {
        return new FrozenBloomFilter( wrapped.copy() );
    }

    @Override
    public boolean contains(BloomFilter other) {
        return wrapped.contains(other);
    }

    @Override
    public LongPredicate makePredicate(LongBiPredicate func) {
        return wrapped.makePredicate(func);
    }

    @Override
    public boolean contains(Hasher hasher) {
        return wrapped.contains(hasher);
    }

    @Override
    public int[] asIndexArray() {
        return wrapped.asIndexArray();
    }

    @Override
    public BloomFilter merge(BloomFilter other) {
        return wrapped.merge(other);
    }

    @Override
    public BloomFilter merge(Hasher hasher) {
        return wrapped.merge(hasher);
    }

    @Override
    public int estimateN() {
        return wrapped.estimateN();
    }

    @Override
    public int estimateUnion(BloomFilter other) {
        return wrapped.estimateUnion(other);
    }

    @Override
    public int estimateIntersection(BloomFilter other) {
        return wrapped.estimateIntersection(other);
    }

    private class ShapeComparator implements Comparator<Shape> {
        @Override
        public int compare(Shape shape1, Shape other) {
            int i = Integer.compare(shape1.getNumberOfBits(), other.getNumberOfBits());
            return i == 0 ? Integer.compare(shape1.getNumberOfHashFunctions(), other.getNumberOfHashFunctions()) : i;
        }
    }
}
