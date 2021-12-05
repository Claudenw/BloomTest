package org.xenei.bloompaper.index;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
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

    public BloomIndexHamming(int population, Shape shape) {
        super(population, shape);
        this.index = new BFHamming(shape);
    }

    @Override
    public void add(BloomFilter filter) {
        index.add(filter);
        count++;
    }

    @Override
    public void delete(BloomFilter filter) {
        if (index.delete(filter)) {
            count--;
        }
    }

    public List<FrozenBloomFilter> getFound() {
        return index.found;
    }

    @Override
    public String getName() {
        return "Hamming";
    }

    @Override
    public int count() {
        return count;
    }

    public int scan(BloomFilter filter) {
        return index.scan(filter);
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        index.search(consumer,filter);
    }
}
