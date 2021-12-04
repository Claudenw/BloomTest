package org.xenei.bloompaper.index;

import java.util.Collection;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.flatbloofi.FlatBloofi;

/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexFlatBloofi extends BloomIndex {
    private FlatBloofi bloofi;

    public BloomIndexFlatBloofi(int population, Shape shape) {
        super(population, shape);
        this.bloofi = new FlatBloofi(population, shape);
    }

    @Override
    public void add(BloomFilter filter) {
        bloofi.add(filter);
        ;
    }

    @Override
    public void delete(BloomFilter filter) {
        bloofi.delete(filter);
    }

    @Override
    public int count(BloomFilter filter) {
        return bloofi.count(filter);
    }

    @Override
    public String getName() {
        return "Flat Bloofi";
    }

    @Override
    public int count() {
        return bloofi.count();
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        bloofi.setFilterCapture(collection);
    }
}
