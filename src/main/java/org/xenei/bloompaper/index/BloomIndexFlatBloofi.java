package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;
import org.xenei.bloompaper.index.flatbloofi.FlatBloofi;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexFlatBloofi extends BloomIndex {
    private FlatBloofi bloofi;

    public BloomIndexFlatBloofi(int population, BloomFilter.Shape shape)
    {
        super(population, shape);
        this.bloofi = new FlatBloofi(population, shape);
    }

    @Override
    public void add(InstrumentedBloomFilter filter)
    {
        bloofi.add( filter );;
    }



    @Override
    public int count(InstrumentedBloomFilter filter)
    {
        return bloofi.count(filter);
    }

    @Override
    public String getName() {
        return "Flat Bloofi";
    }
}
