package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.LongConsumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.exceptions.NoMatchException;


/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexList extends BloomIndex {
    private List<BloomFilter> index;
    private Collection<BloomFilter> filterCapture;


    public BloomIndexList(int population,Shape shape)
    {
        super(population, shape);
        this.index = new ArrayList<BloomFilter>(population);
    }

    @Override
    public void add(BloomFilter filter)
    {
        index.add(filter);
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
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare( filter, (x,y) -> x.equals(y) );

        Iterator<BloomFilter> iter = index.iterator();
        while (iter.hasNext()) {
            if (comp.matches( iter.next() )) {
                iter.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "List";
    }

    @Override
    public int count() {
        return index.size();
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        filterCapture = collection;
    }

}
