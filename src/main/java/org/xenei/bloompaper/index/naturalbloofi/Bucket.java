package org.xenei.bloompaper.index.naturalbloofi;

import java.nio.ByteBuffer;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.collections4.bloomfilter.ArrayCountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.BitMap;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.SetOperations;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.SparseBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.SimpleHasher;

public class Bucket extends NodeContainer {
    private static final Shape shape = Shape.Factory.fromNP(10000, 0.1);

    private final CountingBloomFilter filter;
    private int count;
    private int id;

    public Bucket(int id) {
        super( null, new TreeSet<Node>() );
        filter = new ArrayCountingBloomFilter(shape);
        count = 0;
    }

    public static BloomFilter forFilter(BloomFilter filter) {
        byte[] buffer = new byte[BitMap.numberOfBitMaps(filter.getShape().getNumberOfBits())*Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap( buffer );
        filter.forEachBitMap( x -> {bb.putLong(x);return true;} );
        long[] parts = MurmurHash3.hash128(buffer);
        Hasher hasher = new SimpleHasher( parts[0], parts[1] );
        return new SparseBloomFilter( shape, hasher );
    }

    public boolean hasSpace() {
        return count < 10000;
    }
    public int distance( BloomFilter filter ) {
        return SetOperations.hammingDistance(this.filter, filter);
    }

    public boolean contains( BloomFilter filter ) {
        return this.filter.contains( filter );
    }

    public void add(BloomFilter filter, int id, BloomFilter filterFilter) {
        this.filter.mergeInPlace( filterFilter );
        new Node( this, filter, id);
        count++;
    }

    public boolean delete(BloomFilter filter, BloomFilter filterFilter) {
        Deleter deleter = new Deleter(n -> count--, filter);
        boolean result = !testChildren(deleter.target, deleter);
        deleter.cleanup();
        if (result) {
            this.filter.remove(filterFilter);
            count--;
        }
        return result;
    }



    @Override
    public int getId() {
        return id;
    }


}
