package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
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
public class FrozenBloomFilter extends SimpleBloomFilter {

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
        return new FrozenBloomFilter( bf );
    }

    /**
     * Create a new Frozen bloom filter from a shape and a set of longs.
     * @param shape the Shape of the filter.
     * @param bits the enabled bits encoded as an array of longs.
     */
    private FrozenBloomFilter( BloomFilter bf )
    {
        super(  bf.getShape(), bf );
    }

    @Override
    public boolean mergeInPlace(Hasher hasher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean mergeInPlace(BloomFilter other) {
        throw new UnsupportedOperationException();
    }


}
