package org.xenei.bloompaper.index;

import java.util.Arrays;
import java.util.BitSet;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;


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
 * @see BloomFilter#getBits()
 * @author claude
 *
 */
public class FrozenBloomFilter extends AbstractBloomFilter {

    /**
     * The bits for the filter.
     */
    private long[] bits;

    /**
     * Method to create a frozen filter from a standard filter.
     * If the standard filter is already a FrozenBloomFilter then
     * it is returned unchanged.
     * @param bf the Bloom filter to freeze
     * @return a FrozenBloomFilter.
     */
    public static FrozenBloomFilter makeInstance( BloomFilter bf )
    {
        if (bf instanceof FrozenBloomFilter)
        {
            return (FrozenBloomFilter)bf;
        }
        return new FrozenBloomFilter( bf.getShape(), bf.getBits());
    }

    /**
     * Create a new Frozen bloom filter from a shape and a set of longs.
     * @param shape the Shape of the filter.
     * @param bits the enabled bits encoded as an array of longs.
     */
    public FrozenBloomFilter( Shape shape, long ... bits)
    {
        super(shape);
        this.bits = bits.clone();
    }

    @Override
    public long[] getBits() {
        return bits;
    }
    @Override
    public StaticHasher getHasher() {
        BitSet bitset = BitSet.valueOf( bits );
        return new StaticHasher( bitset.stream().iterator(), getShape());
    }
    @Override
    public void merge(BloomFilter other) {
        throw new UnsupportedOperationException( "merge not supported" );
    }
    @Override
    public void merge(Hasher hasher) {
        throw new UnsupportedOperationException( "merge not supported" );
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bits);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FrozenBloomFilter)
        {
            return Arrays.equals( bits, ((FrozenBloomFilter)other).bits);
        }
        return false;
    }

    @Override
    public String toString() {
        return BitUtils.format( bits );
    }
}
