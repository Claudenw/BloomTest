package org.xenei.bloompaper.index.bftrie;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;

public class BFTrie8 implements BFTrie {
    public static int[][] byteTable;

    static {
        for (int i = 0; i < 256; i++) {

            int[] accum = new int[256];
            int counter = 0;
            for (int j = 0; j < 256; j++) {
                if ((i & j) == i) {
                    accum[counter++] = j;
                }
            }
            byteTable[i] = accum;
        }
    }

    private InnerNode root;
    private int count;

    public BFTrie8(Shape shape) {
        root = new InnerNode(0, shape, this);
        count = 0;
    }

    @Override
    public int getWidth() {
        return Byte.SIZE;
    }

    @Override
    public int count() {
        return count;
    }

    public void add(BloomFilter filter) {
        root.add(this, filter, filter.asBitMapArray());
        count++;
    }

    @Override
    public boolean find(BloomFilter filter) {
        return root.find(filter.asBitMapArray());
    }

    @Override
    public boolean remove(BloomFilter filter) {
        if (root.remove(filter.asBitMapArray())) {
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
        root.search(this, consumer, filter.asBitMapArray());
    }

    /**
     * Gets the nibble Info for the nibble specified by the filter.
     * @param filter the BloomFilter to get the nibble from.
     * @param level the level of the BFTrie we are at.
     * @return the NibbleInfo for that level
     */
    @Override
    public int getIndex(long[] buffer, int level) {

        int idx = BitUtils.getLongIndex(level);
        // buffer may be short if upper values are zero
        if (idx >= buffer.length) {
            return 0;
        }

        int shift = level % Long.SIZE;
        long mask = (0xFFL << shift);
        long value = buffer[idx] & mask;
        return (int) ((value >> shift) & 0xFF);
    }

    @Override
    public int[] lookup(long[] buffer, int level) {
        return byteTable[getIndex(buffer, level)];
    }
}
