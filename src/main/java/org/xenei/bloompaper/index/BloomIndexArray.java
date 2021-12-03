package org.xenei.bloompaper.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.LongConsumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.exceptions.NoMatchException;


/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexArray extends BloomIndex {
    private BloomFilter[] index;
    private int idx;
    private Collection<BloomFilter> filterCapture;


    public BloomIndexArray(int population,Shape shape)
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
        for (int i=0;i<idx;i++)
        {
            if (index[i].contains(filter))
            {
                filterCapture.add( index[i] );
                result++;
            }
        }
        return result;
    }


    @Override
    public void delete(BloomFilter filter)
    {
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare( filter, (x,y) -> x.equals(y));

        for (int i=idx-1;i>=0;i--)
        {
            if (comp.matches( index[i] ))
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
