package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;


/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexLinear extends BloomIndex {
    private InstrumentedBloomFilter[] index;
    private int idx;

    public BloomIndexLinear(int population,BloomFilter.Shape bloomFilterConfig)
    {
        super(population, bloomFilterConfig);
        this.index = new InstrumentedBloomFilter[population];
        this.idx = 0;
    }

    @Override
    public void add(InstrumentedBloomFilter filter)
    {
        index[idx++] = filter;
    }

    @Override
    public int count(InstrumentedBloomFilter filter)
    {
        int result = 0;
        // searching entire list
        for (InstrumentedBloomFilter candidate : index)
        {
            if (candidate.contains(filter))
            {
                result++;
            }
        }
        return result;

    }

    @Override
    public String getName() {
        return "Linear";
    }

}
