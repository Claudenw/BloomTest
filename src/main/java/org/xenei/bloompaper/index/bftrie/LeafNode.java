package org.xenei.bloompaper.index.bftrie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class LeafNode implements Node {
    private final List<BloomFilter> lst;
    private boolean checkEntries;

    public LeafNode(boolean checkEntries) {
        this.checkEntries = checkEntries;
        lst = new ArrayList<BloomFilter>();
    }

    public List<BloomFilter> getList() {
        return lst;
    }

    @Override
    public void add(BFTrie4 btree, BloomFilter filter, long[] buffer) {
        lst.add(filter);
    }

    @Override
    public boolean find(long[] buffer) {
        return !lst.isEmpty();
    }

    @Override
    public boolean remove(long[] buffer) {
        lst.remove(lst.size() - 1);
        return true;
    }

    @Override
    public boolean isEmpty() {
        return lst.isEmpty();
    }

    @Override
    public void search(Consumer<BloomFilter> result, long[] buffer) {
        if (checkEntries) {
            BitMapProducer bmp = BitMapProducer.fromLongArray(buffer);
            lst.stream().filter( b -> b.contains(bmp)).forEach(result);
        } else {
            lst.stream().forEach(result);
        }
    }

    @Override
    public String toString() {
        return String.format("LeafNode %s", lst);
    }

}
