package org.xenei.bloompaper.index.bftrie;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;

public class BFTrie4 implements BFTrie {
    public static final int[][] nibbleTable = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF },
        { 1, 3, 5, 7, 9, 0xB, 0xD, 0xF }, { 2, 3, 6, 7, 0xA, 0xB, 0xE, 0xF }, { 3, 7, 0xB, 0xF },
        { 4, 5, 6, 7, 0xC, 0xD, 0xE, 0xF }, { 5, 7, 0xD, 0xF }, { 6, 7, 0xE, 0xF }, { 7, 0xF },
        { 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF }, { 9, 0xB, 0xD, 0xF }, { 0xA, 0xB, 0xE, 0xF }, { 0xB, 0xF },
        { 0xC, 0xD, 0xE, 0xF }, { 0xD, 0xF }, { 0xE, 0xF }, { 0xF }, };

    private InnerNode root;
    private int count;

    public BFTrie4(Shape shape) {
        root = new InnerNode(0, shape, this);
        count = 0;
    }

    @Override
    public int count() {
        return count;
    }

    public void add(BloomFilter filter) {
        root.add(this, filter, BloomFilter.asBitMapArray(filter));
        count++;
    }

    @Override
    public boolean find(BloomFilter filter) {
        return root.find(BloomFilter.asBitMapArray(filter));
    }

    @Override
    public boolean remove(BloomFilter filter) {
        if (root.remove(BloomFilter.asBitMapArray(filter))) {
            count--;
            return true;
        }
        return false;
    }

    @Override
    public void search(Consumer<BloomFilter> consumer, BloomFilter filter) {
        // estimate result size as % of key space.
        // int f = shape.getNumberOfBits() - filter.cardinality();
        // int initSize = count * f / shape.getNumberOfBits();
        root.search(this, consumer, BloomFilter.asBitMapArray(filter));
    }

    @Override
    public int getWidth() {
        return 4;
    }

    /**
     * Gets the nibble Info for the nibble specified by the filter.
     * @param filter the BloomFilter to get the nibble from.
     * @param level the level of the BFTrie we are at.
     * @return the NibbleInfo for that level
     */
    @Override
    public int getIndex(long[] buffer, int level) {
        int startBit = level * 4;

        int idx = BitUtils.getLongIndex(startBit);
        // buffer may be short if upper values are zero
        if (idx >= buffer.length) {
            return (byte) 0;
        }

        int shift = startBit % Long.SIZE;
        long mask = (0xFL << shift);
        long value = buffer[idx] & mask;
        return (byte) ((value >> shift) & 0x0F);
    }

    @Override
    public int[] lookup(long[] buffer, int level) {
        return nibbleTable[getIndex(buffer, level)];
    }

}
