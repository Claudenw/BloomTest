package org.xenei.bloompaper.index.shardedlist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.BloomIndex;

/**
 * A multidimentional Bloom filter that constructs a list by creating sharded sub lists.
 * Each shard is fronted by a Bloom filter to determine if the shard should be searched.  Each
 * filter inserted in the list is used to generate an internal Bloom filter.
 * When all shards are full a new shard is created.
 */
public class ShardedList extends BloomIndex {
    private final static int shardSize = 10000;
    private List<Shard> root;
    private int count;
    private final Shape filterShape;

    public ShardedList(int population, Shape shape) {
        super(population, shape);
        int limit = (population / shardSize) + 1;
        root = new ArrayList<Shard>(limit);
        filterShape = Shape.fromNP(shardSize * shape.getNumberOfHashFunctions(), 0.1);

        for (int i = 0; i < limit; i++) {
            root.add(new Shard(filterShape, shardSize));
        }
        count = 0;
    }

    @Override
    public void add(BloomFilter filter) {
        int dist = Integer.MAX_VALUE;
        int bucket = -1;
        Shard candidate = null;
        Hasher filterHasher = BitUtils.ShardingHasherFactory.asHasher(filter);
        if (root.size() == 1) {
            candidate = root.get(0);
            if (candidate.hasSpace()) {
                bucket = 0;
            }
        } else {
            BloomFilter filterFilter = new SimpleBloomFilter(filterShape, filterHasher );
            for (int i = 0; i < root.size(); i++) {
                candidate = root.get(i);
                if (candidate.hasSpace()) {
                    int candidateDist = candidate.distance(filterFilter);
                    if (dist > candidateDist) {
                        dist = candidateDist;
                        bucket = i;
                    }
                }
            }
        }
        if (bucket == -1) {
            // no space
            bucket = root.size();
            candidate = new Shard(filterShape, shardSize);
            root.add(candidate);
        } else {
            candidate = root.get(bucket);
        }
        candidate.add(filter, filterHasher);
        count++;
    }

    @Override
    public boolean delete(BloomFilter filter) {
        Hasher filterHasher = BitUtils.ShardingHasherFactory.asHasher(filter);
        Shard candidate = null;
        for (int i = 0; i < root.size(); i++) {
            candidate = root.get(i);
            if (candidate.contains(filterHasher)) {
                if (candidate.delete(filter, filterHasher)) {
                    count--;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        Hasher filterHasher = BitUtils.ShardingHasherFactory.asHasher(filter);

        Shard candidate = null;

        for (int i = 0; i < root.size(); i++) {
            candidate = root.get(i);
            if (candidate.contains(filterHasher)) {
                candidate.doSearch(consumer, filter);
            }
        }
    }

    @Override
    public String getName() {
        return "ShardedList";
    }

    @Override
    public int count() {
        return count;
    }

}
