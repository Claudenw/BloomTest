package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.index.hamming.BFHamming;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BloomIndexHamming extends BloomIndex {

    private BFHamming index;
    private int count;

    public BloomIndexHamming(int population, Shape bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.index = new BFHamming();
    }

    @Override
    public void add(BloomFilter filter) {
        index.add( filter );
        count++;
    }


    @Override
    public void delete(BloomFilter filter) {
        if (index.delete( filter ))
        {
            count--;
        }
    }

    @Override
    public int count(BloomFilter filter) {
        return index.count( filter );
    }

    @Override
    public String getName() {
        return "Hamming";
    }

    @Override
    public int count() {
        return count;
    }
}
