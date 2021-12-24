package org.xenei.bloompaper.index.bftrie;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public interface BFTrie {

    public int getWidth();

    public void remove(BloomFilter filter);

    public int count();

    public boolean find(BloomFilter filter);

    public void search(Consumer<BloomFilter> consumer, BloomFilter filter);

    public int getIndex(long[] buffer, int level);

    public int[] lookup( long[] buffer, int level);
}
