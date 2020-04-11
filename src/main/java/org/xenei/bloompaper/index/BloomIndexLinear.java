package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.Stats;


/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexLinear extends BloomIndex {
    private BloomFilter[] index;
    private int idx;

    List<NumericBloomFilter> found;

    public BloomIndexLinear(int population,Shape shape)
    {
        super(population, shape);
        this.index = new BloomFilter[population];
        this.idx = 0;
    }

    @Override
    public void add(BloomFilter filter)
    {
        index[idx++] = filter;
    }

    public List<NumericBloomFilter> getFound() {
        return found;
    }

    @Override
    public int count(BloomFilter filter)
    {
        found = new ArrayList<NumericBloomFilter>();
        int result = 0;
        // searching entire list
        for (BloomFilter candidate : index)
        {
            if (candidate.contains(filter))
            {
                found.add( NumericBloomFilter.makeInstance(candidate) );
                result++;
            }
        }
        return result;
    }

    @Override
    public void delete(BloomFilter filter)
    {
        long[] bits = filter.getBits();
        for (int i=idx-1;i>=0;i--)
        {
            if (Arrays.compare(index[i].getBits(), bits) == 0)
            {
                if (i < idx-1)
                {
                    System.arraycopy( index, i+1, index, i, idx-i-1);
                }
                index[--idx] = null;
                break;
            }
        }
    }

    @Override
    public String getName() {
        return "Linear";
    }

    @Override
    public int count() {
        return idx;
    }
}
