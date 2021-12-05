package org.xenei.bloompaper.index.bftrie;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

public class BFTrie4 {
    public static final int[][] nibbleTable = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF },
        { 1, 3, 5, 7, 9, 0xB, 0xD, 0xF }, { 2, 3, 6, 7, 0xA, 0xB, 0xE, 0xF }, { 3, 7, 0xB, 0xF },
        { 4, 5, 6, 7, 0xC, 0xD, 0xE, 0xF }, { 5, 7, 0xD, 0xF }, { 6, 7, 0xE, 0xF }, { 7, 0xF },
        { 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF }, { 9, 0xB, 0xD, 0xF }, { 0xA, 0xB, 0xE, 0xF }, { 0xB, 0xF },
        { 0xC, 0xD, 0xE, 0xF }, { 0xD, 0xF }, { 0xE, 0xF }, { 0xF }, };

    private InnerNode root;
    private int count;
    private final Shape shape;

    public BFTrie4(Shape shape) {
        this.shape = shape;
        root = new InnerNode(0, shape);
        count = 0;
    }

    public int count() {
        return count;
    }

    public void add(BloomFilter filter) {
        root.add(this, filter, BloomFilter.asBitMapArray(filter));
        count++;
    }

    public boolean find(BloomFilter filter) {
        return root.find(BloomFilter.asBitMapArray(filter));
    }

    public void remove(BloomFilter filter) {
        if (root.remove(BloomFilter.asBitMapArray(filter))) {
            count--;
        }
    }

    public void search(Consumer<BloomFilter> consumer, BloomFilter filter) {
        // estimate result size as % of key space.
        int f = shape.getNumberOfBits() - filter.cardinality();
        int initSize = count * f / shape.getNumberOfBits();
        root.search(consumer, BloomFilter.asBitMapArray(filter));
    }

}
