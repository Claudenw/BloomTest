package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.index.bloofi.Bloofi;


/**
 * Implementation of BT
 * ree Nibble search.
 *
 */
public class BloomIndexBloofi extends BloomIndex {
    private Bloofi bloofi;

    public BloomIndexBloofi(int population, Shape bloomFilterConfig)
    {
        super(population, bloomFilterConfig);
        this.bloofi = new Bloofi(population, bloomFilterConfig);
    }

    @Override
    public void add(BloomFilter filter)
    {
        bloofi.add( filter );;
    }


    @Override
    public int count(BloomFilter filter)
    {
        return bloofi.count(filter);
    }

    @Override
    public String getName() {
        return "Bloofi Impl";
    }

    @Override
    public void delete(BloomFilter filter) {
        bloofi.delete(filter);
    }

    @Override
    public int count() {
        return bloofi.count();
    }

}
