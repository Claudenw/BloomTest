package org.xenei.bloompaper.index;

import java.util.Collection;
import java.util.function.Consumer;

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
    public void doSearch(Consumer<BloomFilter> result, BloomFilter filter) {
        bloofi.search(result, filter);
    }

    @Override
    public String getName() {
        return "Flat Bloofi";
    }

    @Override
    public int count() {
        return bloofi.count();
    }

}
