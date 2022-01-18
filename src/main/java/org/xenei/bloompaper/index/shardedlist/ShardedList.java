package org.xenei.bloompaper.index.shardedlist;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.index.BloomIndex;

public class ShardedList extends BloomIndex {

    private List<Shard> root;
    private int id;
    private int count;

    public ShardedList(int population, Shape shape) {
        super(population, shape);
        int limit = (population/10000)+1;
        root = new ArrayList<Shard>(limit);

        for (int i=0;i<limit;i++) {
            root.add( new Shard() );
        }
        id = 0;
        count = 0;
    }

    @Override
    public void add(BloomFilter filter) {
        BloomFilter filterFilter = Shard.forFilter(filter);
        int dist = Integer.MAX_VALUE;
        int bucket=-1;
        Shard candidate = null;
        for (int i=0;i<root.size();i++) {
            candidate = root.get(i);
            if (candidate.hasSpace()) {
                int candidateDist = candidate.distance(filterFilter);
                if (dist > candidateDist) {
                    dist = candidateDist;
                    bucket = i;
                }
            }
        }

        if (bucket == -1) {
            // no space
            bucket = root.size();
            candidate = new Shard( );
            root.add( candidate );
        } else {
            candidate = root.get(bucket);
        }
        candidate.add( filter, filterFilter );
        count++;
    }

    @Override
    public void delete(BloomFilter filter) {
        BloomFilter filterFilter = Shard.forFilter(filter);
        Shard candidate = null;
        for (int i=0;i<root.size();i++) {
            candidate = root.get(i);
            if (candidate.contains( filterFilter )) {
                if (candidate.delete( filter, filterFilter )) {
                    count--;
                    return;
                }
            }
        }
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        BloomFilter filterFilter = Shard.forFilter(filter);
        Shard candidate = null;
        for (int i=0;i<root.size();i++) {
            candidate = root.get(i);
            if (candidate.contains( filterFilter )) {
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
