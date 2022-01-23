package org.xenei.bloompaper.index.flatbloofi;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.index.BitUtils;

/**
 * This is what Daniel called Bloofi2. Basically, instead of using a tree
 * structure like Bloofi (see BloomFilterIndex), we "transpose" the BitSets.
 *
 *
 * @author Daniel Lemire
 *
 * @param <E>
 */
public final class FlatBloofi {

    /*
     * each buffer entry accounts for 64 entries in the index. each there is one
     * long in each buffer entry for each bit in the bloom filter. each long is a
     * bit packed set of 64 flags, one for each entry.
     */
    private ArrayList<long[]> buffer;
    private BitSet busy;
    private final Shape shape;

    public FlatBloofi(int population, Shape shape) {
        this.shape = shape;
        buffer = new ArrayList<long[]>(0);
        busy = new BitSet(0);
    }

    public void add(BloomFilter bf) {
        int i = busy.nextClearBit(0);
        if (buffer.size() - 1 < BitUtils.getLongIndex(i)) {
            buffer.add(new long[shape.getNumberOfBits() + 1]);
        }
        setBloomAt(i, BloomFilter.asBitMapArray(bf));
        busy.set(i);
    }

    public void search(Consumer<BloomFilter> result, BloomFilter bf) {
        BitSet bs = BitSet.valueOf(BloomFilter.asBitMapArray(bf));

        for (int i = 0; i < buffer.size(); i++) {
            long w = ~0l;
            for (int l = bs.nextSetBit(0); l >= 0; l = bs.nextSetBit(l + 1)) {
                w &= buffer.get(i)[l];
            }

            while (w != 0) {
                long t = w & -w;
                int idx = Long.numberOfTrailingZeros(t) + (Long.SIZE * i);
                if (busy.get(idx)) {
                    result.accept(getBloomAt(idx));
                }
                w ^= t;
            }
        }
    }

    private BloomFilter getBloomAt(int idx) {
        IndexProducer indexProducer = new IndexProducer() {

            final long[] mybuffer = buffer.get(BitUtils.getLongIndex(idx));
            final long mask = BitUtils.getLongBit(idx);

            @Override
            public boolean forEachIndex(IntPredicate consumer) {
                for (int k = 0; k < mybuffer.length; k++) {
                    if ((mask & mybuffer[k]) > 0) {
                        if (!consumer.test(k)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
        BitMapProducer bitMapProducer = BitMapProducer.fromIndexProducer(indexProducer, shape.getNumberOfBits());
        return new SimpleBloomFilter(shape, bitMapProducer);
    }

    private void setBloomAt(int i, long[] bits) {
        final long[] mybuffer = buffer.get(BitUtils.getLongIndex(i));
        final long mask = BitUtils.getLongBit(i);
        for (int k = 0; k < mybuffer.length; k++) {
            if (BitUtils.isSet(bits, k)) {
                mybuffer[k] |= mask;
            } else {
                mybuffer[k] &= ~mask;
            }
        }
    }

    /**
     * Gets a packed index of entries that exactly match the filter.
     * @param filter the filter to match/
     * @return a packed index of entries.
     */
    private BitSet findExactMatch(BloomFilter filter) {
        long[] bits = BloomFilter.asBitMapArray(filter);
        long[] result = new long[buffer.size()];
        long[] busyBits = busy.toLongArray();
        /*
         * for each set of 64 filters in the index
         */
        for (int filterSetIdx = 0; filterSetIdx < result.length; filterSetIdx++) {
            /*
             * Each entry in the filter set is a map of 64 filters in the index to the bit
             * for the position. So filterSet[0] is a bit map of 64 index entries if the bit
             * is on in a specific position then that Bloom filter has bit 0 turned on.
             *
             */
            long[] filterSet = buffer.get(filterSetIdx);

            /*
             * Build a list of the 64 filters in this chunk of index that have the proper
             * bits turned on.
             */

            /*
             * remove is the map of all entries that we know do not match so remove all the
             * ones that are not in the busy list
             */
            long remove = ~busyBits[filterSetIdx];

            // is the list of all entries that might match.
            long keep = ~0L;
            boolean foundKeep = false;

            for (int idx = 0; idx < shape.getNumberOfBits(); idx++) {
                if (BitUtils.isSet(bits, idx)) {
                    foundKeep = true;
                    keep &= filterSet[idx];
                    if (keep == 0) {
                        // we are not keeping any so get out of the loop
                        break;
                    }
                } else {
                    remove |= filterSet[idx];
                    if (remove == ~0L) {
                        // we are removing them all so get out of the loop
                        break;
                    }
                }
            }

            if (foundKeep) {
                result[filterSetIdx] = keep & ~remove;
            }
        }

        return BitSet.valueOf(result);
    }

    public boolean delete(BloomFilter filter) {
        BitSet found = findExactMatch(filter);

        int delIdx = found.nextSetBit(0);
        if (delIdx > -1) {
            busy.clear(delIdx);
            return true;
        }
        return false;
    }

    public int count() {
        return busy.cardinality();
    }

}
