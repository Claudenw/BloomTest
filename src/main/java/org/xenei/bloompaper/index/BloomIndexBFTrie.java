package org.xenei.bloompaper.index;

import java.util.Collection;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.bftrie.BFTrie4;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexBFTrie extends BloomIndex {
    /**
     * The implementation of bftrie.
     */
    private BFTrie4 bftrie;

    /**
     * Constructs the index
     * @param population the expected population.
     * @param shape the Shape of the Bloom filters.
     */
    public BloomIndexBFTrie(int population, Shape shape)
    {
        super(population, shape);
        this.bftrie = new BFTrie4(shape);
    }

    @Override
    public void add(BloomFilter filter)
    {
        bftrie.add( filter );;
    }

    @Override
    public int count(BloomFilter filter)
    {
        return bftrie.count(filter);
    }

    @Override
    public String getName() {
        return "BF-Trie";
    }

    @Override
    public void delete(BloomFilter filter) {
        bftrie.remove( filter );
    }

    @Override
    public int count() {
        return bftrie.count();
    }

    public boolean find(BloomFilter filter) {
        return bftrie.find( filter );
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        bftrie.setFilterCapture( collection );
    }

}
