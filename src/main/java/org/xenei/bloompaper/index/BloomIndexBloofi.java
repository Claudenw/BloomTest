package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.InstrumentedBloomFilter;
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
        return "Bloofi Impl";
    }

}
