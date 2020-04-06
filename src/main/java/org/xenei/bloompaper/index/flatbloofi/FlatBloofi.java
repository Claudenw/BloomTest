package org.xenei.bloompaper.index.flatbloofi;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.BloomIndex;
import org.xenei.bloompaper.index.NumericBloomFilter;

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

    /*
     * each buffer entry accounts for 64 entries in the index.
     * each there is one long in each buffer entry for each bit
     * in the bloom filter.
     * each long is a bit packed set of 64 flags, one for each entry.
     */
    private ArrayList<long[]> buffer;
    private BitSet busy;
    private Set<BloomFilter> toDelete = new TreeSet<BloomFilter>( new Comparator<BloomFilter>() {

        @Override
        public int compare(BloomFilter arg0, BloomFilter arg1) {
            return Arrays.compare( arg0.getBits(), arg1.getBits());
        }});

    public FlatBloofi(int population, Shape shape) {
        super(population, shape);
        buffer = new ArrayList<long[]>(0);
        busy = new BitSet(0);
        toDelete.add( new NumericBloomFilter( shape, 1490127952317283331l, 192 ));
toDelete.add( new NumericBloomFilter( shape, -5866426439917060973l, 235));
toDelete.add( new NumericBloomFilter( shape, 332175928074042883l, 242));
toDelete.add( new NumericBloomFilter( shape, -7703665306996998765l, 209));
toDelete.add( new NumericBloomFilter( shape, 1529483428526887379l, 17));
toDelete.add( new NumericBloomFilter( shape, 1229782938247364606l, 17));
toDelete.add( new NumericBloomFilter( shape, 1247806132851905809l, 147));
toDelete.add( new NumericBloomFilter( shape, 6869418595293663580l, 17));
toDelete.add( new NumericBloomFilter( shape, -5660673895261988461l, 215));
toDelete.add( new NumericBloomFilter( shape, 619682041822808520l, 138));
toDelete.add( new NumericBloomFilter( shape, 1347179666688512345l, 147));
toDelete.add( new NumericBloomFilter( shape, 1347181874317558347l, 147));
toDelete.add( new NumericBloomFilter( shape, 3653022813325434985l, 179));
toDelete.add( new NumericBloomFilter( shape, 691739640172480969l, 139));
toDelete.add( new NumericBloomFilter( shape, 1844678805954337225l, 154));
toDelete.add( new NumericBloomFilter( shape, 5608376568908695500l, 94));
toDelete.add( new NumericBloomFilter( shape, 1347181874317558347l, 147));
toDelete.add( new NumericBloomFilter( shape, 1229782938247303441l, 17));
    }

    @Override
    public void add(BloomFilter bf) {
        int i = busy.nextClearBit(0);
        if (buffer.size()-1 < BitUtils.getLongIndex(i))
        {
            buffer.add(new long[shape.getNumberOfBits()+1]);
        }
        setBloomAt(i, bf.getBits());
        busy.set(i);
    }

    @Override
    public int count(BloomFilter bf) {
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

    private void setBloomAt(int i, long[] bits) {
        final long[] mybuffer = buffer.get( BitUtils.getLongIndex(i));
        final long mask = BitUtils.getLongBit(i);
        for (int k=0;k<mybuffer.length;k++) {
            if (BitUtils.isSet( bits, k ))
            {
                mybuffer[k] |= mask;
            } else {
                mybuffer[k] &= ~mask;
            }
        }

        if (population == 10000 && i == 1000)
        {
            for (int k=0;k<mybuffer.length;k++)
            {
                if (BitUtils.isSet(bits, k))
                {
                    if ((mybuffer[k] & mask)==0)
                    {
                        System.err.println( "Did not set 1000 in mybuffer "+k);
                    }
                }
                else
                {
                    if ((mybuffer[k] & mask)!=0)
                    {
                        System.err.println( "Did set 1000 in mybuffer "+k);
                    }

                }

            }
        }
    }

    /**
     * Gets a packed index of entries that exactly match the filter.
     * @param filter the filter to match/
     * @return a packed index of entries.
     */
    private BitSet findExactMatch( BloomFilter filter)
    {
        long[] bits = filter.getBits();
        long[] result = new long[ buffer.size() ];
        long[] busyBits = busy.toLongArray();
        /*
         * for each set of 64 filters in the index
         */
        for (int filterSetIdx=0;filterSetIdx<result.length;filterSetIdx++)
        {
            /*
             * Each entry in the filter set is a map of 64 filters in the index
             * to the bit for the position.  So filterSet[0] is a bit map of 64
             * index entries if the bit is on in a specific position then that
             * Bloom filter has bit 0 turned on.
             *
             */
            long[] filterSet = buffer.get(filterSetIdx);

            /*
             * Build a list of the 64 filters in this chunk of index that
             * have the proper bits turned on.
             */

            /* remove is the map of all entries that we know do not match
             * so remove all the ones that are not in the busy list
            */
            long remove = ~busyBits[ filterSetIdx ];

            // is the list of all entries that might match.
            long keep = ~0L;
            boolean foundKeep = false;


            for (int idx=0;idx<shape.getNumberOfBits();idx++) {
                if (BitUtils.isSet( bits, idx ))
                {
                    foundKeep = true;
                    keep &= filterSet[idx];
                    if (keep == 0)
                    {
                        // we are not keeping any so get out of the loop
                        break;
                    }
                } else {
                    remove |= filterSet[idx];
                    if (remove == ~0L)
                    {
                        // we are removing them all so get out of the loop
                        break;
                    }
                }
            }

            if (foundKeep) {
                result[filterSetIdx]= keep & ~remove;
            }
        }

        return BitSet.valueOf(result);
   }

    @Override
    public void delete(BloomFilter filter) {
        BitSet found = findExactMatch( filter );

        int delIdx = found.nextSetBit(0);
        if (delIdx > -1)
        {
            busy.clear( delIdx );
        }
    }

    @Override
    public String getName() {
        return "Flat Bloofi";
    }

    @Override
    public int count() {
        return busy.cardinality();
    }

}
