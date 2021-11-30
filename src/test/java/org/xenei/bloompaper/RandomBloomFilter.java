package org.xenei.bloompaper;

import java.util.BitSet;
import java.util.Random;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilter;
import org.apache.commons.collections4.bloomfilter.UpdatableBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;
import org.xenei.bloompaper.index.BitUtils;


public class RandomBloomFilter extends AbstractBloomFilter {

    private long[] bits;

    private static Random random = new Random();

    public RandomBloomFilter( Shape shape)
    {
        super(shape);
        int len = (int) Math.ceil( shape.getNumberOfBits() * 1.0 / Long.SIZE );
        this.bits = new long[len];

        for ( int i=0;i<len;i++)
        {
            bits[i] = random.nextLong();
        }
        int lastBits = shape.getNumberOfBits() % Long.SIZE;
        long mask = 0;
        for (int i=0;i<lastBits;i++)
        {
            mask |= BitUtils.getLongBit(i);
        }
        bits[len-1] &= mask;
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
    public void merge(UpdatableBloomFilter other) {
        throw new IllegalStateException( new OperationNotSupportedException() );
    }
    @Override
    public void merge(Hasher hasher) {
        throw new IllegalStateException( new OperationNotSupportedException() );
    }

}
