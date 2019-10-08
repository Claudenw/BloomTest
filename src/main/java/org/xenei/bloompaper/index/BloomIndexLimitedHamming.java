package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.apache.commons.collections4.bloomfilter.StandardBloomFilter;
import org.xenei.bloompaper.hamming.HammingUtils;
import org.xenei.bloompaper.index.btree.InnerNode;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexLimitedHamming extends BloomIndexHamming {
    private int count;

    public BloomIndexLimitedHamming(int population, BloomFilterConfiguration bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.count = 0;
    }

    public int externalCount(BloomFilter filter, List<BloomFilter> lst) {
        int count = 0;
        for (BloomFilter other : lst) {
            if (filter.matches(other)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<BloomFilter> get(BloomFilter filter) {
        int hT = filter.getHammingWeight();
        List<BloomFilter> retval = new ArrayList<BloomFilter>();
        for (Entry<Integer,HammingList> entry : index.entrySet()) {
            if (entry.getKey() == hT)
            {
                retval.addAll( pageGet( filter, hT, entry) );
            } else if (entry.getKey() > hT)
            {
                Iterator<BloomFilter> iter = filterByLimit( filter, entry, bloomFilterConfig);
                while (iter.hasNext()) {
                    BloomFilter other = iter.next();
                    if (filter.matches(other)) {
                        retval.add(other);
                    }
                }
            }
        }
        return retval;
    }

    private Iterator<BloomFilter> filterByLimit(BloomFilter filter, Entry<Integer,HammingList> entry, BloomFilterConfiguration config)
    {
        BitSet t = filter.getBitSet();
        if (t.get( config.getNumberOfBits()))
        {
            return entry.getValue().find(filter);
        }

        int s = entry.getKey()-t.cardinality();
        int maxBit = config.getNumberOfBits()-s;
        if (maxBit < t.length())
        {
            return entry.getValue().find(filter);
        }
        for (int idx = 0;idx<s;idx++)
        {
            if (t.get(config.getNumberOfBits())) {
                t.set(t.previousClearBit(config.getNumberOfBits()));
            } else {
                t.set(config.getNumberOfBits());
            }
        }
        return entry.getValue().find(filter, new StandardBloomFilter( t ).getLog());
    }

    @Override
    public int count(BloomFilter filter) {
        int hT = filter.getHammingWeight();
        int retval = 0;
        for (Entry<Integer,HammingList> entry : index.entrySet()) {
            if (entry.getKey() == hT)
            {
                retval += pageCount( filter, hT, entry);
            } else if (entry.getKey() > hT)
            {
                Iterator<BloomFilter> iter = filterByLimit( filter, entry, bloomFilterConfig);
                while (iter.hasNext()) {
                    if (filter.matches(iter.next())) {
                        retval++;
                    }
                }
            }
        }
        return retval;
    }


    @Override
    public String getName() {
        return "LimitedHamming";
    }

}
