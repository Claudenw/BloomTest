package org.xenei.bloompaper.index;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.bloofi.Bloofi;
import org.xenei.bloompaper.index.bloofi.LeafNode;
import org.xenei.bloompaper.index.bloofi.Node;

/**
 * Implementation of Bloofi.
 *
 */
public class BloomIndexBloofi extends BloomIndex {
    private Bloofi bloofi;

    public BloomIndexBloofi(int population, Shape bloomFilterConfig) {
        super(population, bloomFilterConfig);
        Node.Counter.reset();
        this.bloofi = new Bloofi(population, bloomFilterConfig);
    }

    @Override
    public void add(BloomFilter filter) {
        bloofi.add(filter);
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

    private static LeafNode testing;

    public static void setTesting(Object lastCreated) {
        testing = (LeafNode) lastCreated;
    }

    public static LeafNode getTesting() {
        return testing;
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        bloofi.search(consumer, filter);
    }

}
