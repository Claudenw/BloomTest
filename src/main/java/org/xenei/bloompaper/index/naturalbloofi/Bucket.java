package org.xenei.bloompaper.index.naturalbloofi;

import java.util.TreeSet;
import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.SetOperations;
import org.apache.commons.collections4.bloomfilter.Shape;

public class Bucket extends NodeContainer {

    private final CountingBloomFilter filter;
    private final int bucketSize;
    private final int id;
    private int count;

    public Bucket(int id, Shape filterShape, int bucketSize) {
        super(null, new TreeSet<Node>());
        filter = new ArrayCountingBloomFilter(filterShape);
        count = 0;
        this.id = id;
        this.bucketSize = bucketSize;
    }

    public int getCount() {
        return count;
    }

    public boolean hasSpace() {
        return filter.estimateN() < bucketSize;
    }

    public int distance(BloomFilter filter) {
        return SetOperations.hammingDistance(this.filter, filter);
    }

    public boolean contains(Hasher hasher) {
        return this.filter.contains(hasher);
    }

    public void add(BloomFilter filter, int id, Hasher filterHasher) {
        this.filter.mergeInPlace(filterHasher);
        new Node(this, filter, id);
        count++;
    }

    public boolean delete(BloomFilter filter, Hasher filterHasher) {
        Deleter deleter = new Deleter(n -> count--, filter);
        boolean result = !testChildren(deleter.target, deleter);
        deleter.cleanup();
        if (result) {
            this.filter.remove(filterHasher);
        }
        return result;
    }

    @Override
    public int getId() {
        return id;
    }

}
