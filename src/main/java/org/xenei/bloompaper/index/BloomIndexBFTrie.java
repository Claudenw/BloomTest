package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;
import org.xenei.bloompaper.index.bftrie.BFTrie4;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexBFTrie extends BloomIndex {
    private BFTrie4 bftrie;

    public BloomIndexBFTrie(int population, BloomFilter.Shape shape)
    {
        super(population, shape);
        this.bftrie = new BFTrie4(shape);
    }

    @Override
    public void add(InstrumentedBloomFilter filter)
    {
        bftrie.add( filter );;
    }

    @Override
    public int count(InstrumentedBloomFilter filter)
    {
        return bftrie.count(filter);
    }

    @Override
    public String getName() {
        return "BF-Trie";
    }
}
