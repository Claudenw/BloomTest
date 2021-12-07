package org.xenei.bloompaper.index;

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;

/**
 * A Bloom filter that is created from a list of long values that
 * represent the encoded bits.
 * <p>
 * This filter is immutable, any attempt
 * to change it throws an UnsupportedOperationException.
 * </p><p>
 * This filter implements hashCode() and equals() and is suitable for
 * use in Hash based collections.
 * </p>
 * @see UpdatableBloomFilter#getBits()
 * @author claude
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
     * Create a new Frozen bloom filter from a shape and a set of longs.
     * @param shape the Shape of the filter.
     * @param bits the enabled bits encoded as an array of longs.
     */
    private FrozenBloomFilter(BloomFilter bf) {
        wrapped = bf;
        cardinality = bf.cardinality();
    }

    public FrozenBloomFilter(Shape shape, BitMapProducer producer) {
        this(new SimpleBloomFilter(shape, producer));
    }

    public long[] getBitMap() {
        if (bitMap == null) {
            bitMap = BloomFilter.asBitMapArray(wrapped);
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
        forEachBitMap(l -> sb.append(String.format("0x%08x, ", l)));
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int compareTo(FrozenBloomFilter other) {
        if (this == other) {
            return 0;
        }
        int result = this.getShape().compareTo(other.getShape());
        if (result == 0) {
            long[] oBitMap = other.getBitMap();
            int limit = getBitMap().length < oBitMap.length ? getBitMap().length : oBitMap.length;
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
    public void forEachIndex(IntConsumer consumer) {
        wrapped.forEachIndex(consumer);
    }

    @Override
    public void forEachBitMap(LongConsumer consumer) {
        wrapped.forEachBitMap(consumer);
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

}
