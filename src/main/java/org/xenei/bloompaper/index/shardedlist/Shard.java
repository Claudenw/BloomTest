package org.xenei.bloompaper.index.shardedlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.SetOperations;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.xenei.bloompaper.index.BitUtils;

public class Shard {

    private final CountingBloomFilter gatekeeper;
    private final List<BloomFilter> filters;
    private final int shardSize;

    public Shard(Shape filterShape, int shardSize) {
        gatekeeper = new ArrayCountingBloomFilter(filterShape);
        this.shardSize = shardSize;
        filters = new ArrayList<>(shardSize);
    }

    public boolean hasSpace() {
        return filters.size() < shardSize;
    }

    public int distance(BloomFilter filter) {
        return SetOperations.hammingDistance(gatekeeper, filter);
    }

    public boolean contains(Hasher filterHasher) {
        return gatekeeper.contains(filterHasher);
    }

    public void add(BloomFilter filter, BloomFilter filterFilter) {
        gatekeeper.mergeInPlace(filterFilter);
        filters.add(filter);
    }

    public boolean delete(BloomFilter filter, Hasher filterHasher) {
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare(filter, BitUtils.BufferCompare.exact);
        Iterator<BloomFilter> iter = filters.iterator();
        while (iter.hasNext()) {
            if (comp.matches(iter.next())) {
                iter.remove();
                gatekeeper.remove(filterHasher);
                return true;
            }
        }
        return false;
    }

    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        for (BloomFilter candidate : filters) {
            if (candidate.contains(filter)) {
                consumer.accept(candidate);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Shard n=%s", filters.size());
    }
}
