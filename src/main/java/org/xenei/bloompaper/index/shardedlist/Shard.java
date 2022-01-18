package org.xenei.bloompaper.index.shardedlist;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.xenei.bloompaper.index.BitUtils;

public class Shard {
    private static final Shape shape = Shape.Factory.fromNP(10000, 0.1);

    private final CountingBloomFilter gatekeeper;
    private List<BloomFilter> filters;

    public Shard() {
        gatekeeper = new ArrayCountingBloomFilter(shape);
        filters = new ArrayList<>(10000);
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
        return filters.size() < 10000;
    }

    public int distance( BloomFilter filterFilter ) {
        return SetOperations.hammingDistance(gatekeeper, filterFilter);
    }

    public boolean contains( BloomFilter filterFilter ) {
        return gatekeeper.contains( filterFilter );
    }

    public void add(BloomFilter filter, BloomFilter filterFilter) {
        gatekeeper.mergeInPlace( filterFilter );
        filters.add( filter );
    }

    public boolean delete(BloomFilter filter, BloomFilter filterFilter) {
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare(filter, BitUtils.BufferCompare.exact);
        Iterator<BloomFilter> iter = filters.iterator();
        while (iter.hasNext()) {
            if (comp.matches(iter.next())) {
                iter.remove();
                gatekeeper.remove( filterFilter );
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
        return String.format( "Shard n=%s", filters.size() );
    }
}
