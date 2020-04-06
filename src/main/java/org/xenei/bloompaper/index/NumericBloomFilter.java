package org.xenei.bloompaper.index;

import java.util.BitSet;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;


public class NumericBloomFilter extends AbstractBloomFilter {

    private long[] bits;

    public NumericBloomFilter( Shape shape, long ... bits)
    {
        super(shape);
        this.bits = bits;
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
        throw new IllegalStateException( new OperationNotSupportedException() );
    }
    @Override
    public void merge(Hasher hasher) {
        throw new IllegalStateException( new OperationNotSupportedException() );
    }

}
