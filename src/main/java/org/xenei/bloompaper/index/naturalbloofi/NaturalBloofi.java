package org.xenei.bloompaper.index.naturalbloofi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.BloomIndex;

public class NaturalBloofi extends BloomIndex {
    private List<Bucket> root;
    private int id;
    private final int bucketPopulation = 10000;
    private final Shape filterShape;

    public NaturalBloofi(int population, Shape shape) {
        super(population, shape);
        int limit = (population / bucketPopulation) + 1;
        root = new ArrayList<Bucket>(limit);
        filterShape = Shape.Factory.fromNP(bucketPopulation * shape.getNumberOfHashFunctions(), 0.1);
        for (int i = 0; i < limit; i++) {
            int bucketNumber = (i + 1) * bucketPopulation * -1;
            root.add(new Bucket(bucketNumber, filterShape, bucketPopulation));
        }
        id = 0;
    }

    @Override
    public void add(BloomFilter filter) {
        Hasher filterHasher = new BitUtils.ShardingHasher(filter);

        int dist = Integer.MAX_VALUE;
        int bucket = -1;
        int childDist = Integer.MAX_VALUE;
        int childBucket = -1;
        Bucket candidate = null;
        if (root.size() == 1) {
            candidate = root.get(0);
            if (candidate.hasSpace()) {
                bucket = 0;
            }
        } else {
            BloomFilter filterFilter = new SimpleBloomFilter(filterShape, filterHasher);
            for (int i = 0; i < root.size() && dist != 0; i++) {
                candidate = root.get(i);
                if (candidate.contains(filterHasher)) {
                    int candidateDist = candidate.distance(filterFilter);
                    if (childDist > candidateDist) {
                        childDist = candidateDist;
                        childBucket = i;
                    }
                } else {
                    if (candidate.hasSpace()) {
                        int candidateDist = candidate.distance(filterFilter);
                        if (dist > candidateDist) {
                            dist = candidateDist;
                            bucket = i;
                        }
                    }
                }
            }
        }

        if (childBucket != -1) {
            candidate = root.get(childBucket);
        } else {
            if (bucket == -1) {
                // no space
                bucket = root.size();
                int bucketNumber = (bucket + 1) * -10000;
                candidate = new Bucket(bucketNumber, filterShape, bucketPopulation);
                root.add(candidate);
            } else {
                candidate = root.get(bucket);
            }
        }
        candidate.add(filter, id++, filterHasher);
    }

    @Override
    public boolean delete(BloomFilter filter) {
        Hasher filterHasher = new BitUtils.ShardingHasher(filter);
        Bucket candidate = null;
        for (int i = 0; i < root.size(); i++) {
            candidate = root.get(i);
            if (candidate.contains(filterHasher)) {
                if (candidate.delete(filter, filterHasher)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void mapper(Shape shape, Node n, Consumer<BloomFilter> consumer) {
        BloomFilter bf = new SimpleBloomFilter(shape, BitMapProducer.fromLongArray(n.bitMap));
        for (@SuppressWarnings("unused")
        int i : n.getIds()) {
            consumer.accept(bf);
        }
    }

    @Override
    protected void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        Searcher searcher = new Searcher(n -> mapper(filter.getShape(), n, consumer), filter);
        Hasher filterHasher = new BitUtils.ShardingHasher(filter);
        Bucket candidate = null;
        for (int i = 0; i < root.size(); i++) {
            candidate = root.get(i);
            if (candidate.contains(filterHasher)) {
                candidate.searchChildren(searcher.target, searcher);
            }
        }
    }

    @Override
    public String getName() {
        return "NaturalBlooofi";
    }

    @Override
    public int count() {
        int i = 0;
        for (Bucket b : root) {
            i += b.getCount();
        }
        return i;
    }

    public void reportStatus() {
        for (Bucket bucket : root) {
            System.out.format("%s%n", bucket.getCount());
        }
    }
}
