package org.xenei.bloompaper.index;

import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;


/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexLinear extends BloomIndex {
    private BloomFilter[] index;
    private int idx;
    private Collection<BloomFilter> filterCapture;


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

    @Override
    public int count(BloomFilter filter)
    {
        int result = 0;
        // searching entire list
        for (BloomFilter candidate : index)
        {
            if (candidate.contains(filter))
            {
                filterCapture.add( candidate );
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

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        filterCapture = collection;
    }

}
