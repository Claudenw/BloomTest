package org.xenei.bloompaper.index.flatbloofi;


import java.util.ArrayList;
import java.util.BitSet;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;
import org.xenei.bloompaper.index.BloomIndex;

/**
 * This is what Daniel called Bloofi2. Basically, instead of using a tree
 * structure like Bloofi (see BloomFilterIndex), we "transpose" the BitSets.
 *
 *
 * @author Daniel Lemire
 *
 * @param <E>
 */
public final class FlatBloofi extends BloomIndex {


    ArrayList<long[]> buffer;
    BitSet busy;

    public FlatBloofi(int population, BloomFilter.Shape shape) {
        super(population, shape);
        buffer = new ArrayList<long[]>(0);
        //        buffer.add( new long[shape.getNumberOfBits()+1]);
        busy = new BitSet(0);
    }

    @Override
    public void add(InstrumentedBloomFilter bf) {
        int i = busy.nextClearBit(0);
        BitSet bs = BitSet.valueOf( bf.getBits() );
        if (buffer.size()-1 < i/Long.SIZE)
        {
            buffer.add(new long[shape.getNumberOfBits()+1]);
        }
        setBloomAt(i, bs);
        busy.set(i);
    }

    @Override
    public int count(InstrumentedBloomFilter bf) {
        int count = 0;
        BitSet bs = BitSet.valueOf( bf.getBits() );

        for (int i = 0; i < buffer.size(); i++) {
            long w = ~0l;
            for (int l = bs.nextSetBit(0); l >= 0; l = bs.nextSetBit(l+1)) {
                w &= buffer.get(i)[l];
            }

            while (w != 0) {
                long t = w & -w;
                count++;
                w ^= t;
            }
        }
        return count;
    }



    private void setBloomAt(int i, BitSet bs) {
        final long[] mybuffer = buffer.get(i / Long.SIZE);
        final long mask = (1l << i);
        for (int k = bs.nextSetBit(0); k >= 0; k = bs.nextSetBit(k + 1)) {
            mybuffer[k] |= mask;
        }
    }



    @Override
    public String getName() {
        return "Flat Bloofi";
    }


}
