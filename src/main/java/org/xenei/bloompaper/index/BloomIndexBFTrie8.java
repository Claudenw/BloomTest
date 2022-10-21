package org.xenei.bloompaper.index;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.bftrie.BFTrie4;

/**
 * Implementation of BTree Bytes sized search.
 *
 */
public class BloomIndexBFTrie8 extends BloomIndex {
    /**
     * The implementation of bftrie.
     */
    private BFTrie4 bftrie;

    /**
     * Constructs the index
     * @param population the expected population.
     * @param shape the Shape of the Bloom filters.
     */
    public BloomIndexBFTrie8(int population, Shape shape) {
        super(population, shape);
        this.bftrie = new BFTrie4(shape);
    }

    @Override
    public void add(BloomFilter filter) {
        bftrie.add(filter);
        ;
    }

    @Override
    public String getName() {
        return "BF-Trie8";
    }

    @Override
    public boolean delete(BloomFilter filter) {
        return bftrie.remove(filter);
    }

    @Override
    public int count() {
        return bftrie.count();
    }

    public boolean find(BloomFilter filter) {
        return bftrie.find(filter);
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        bftrie.search(consumer, filter);
    }

}
