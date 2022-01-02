package org.xenei.bloompaper.index.bftrie;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;

public class InnerNode implements Node {
    private final Node[] nodes;
    private final int level;
    private final int maxDepth;
    private final Shape shape;
    private final BFTrie trie;

    public InnerNode(int level, Shape shape, BFTrie trie) {
        this.level = level;
        this.shape = shape;
        this.trie = trie;
        this.maxDepth = shape.getNumberOfBits() / trie.getWidth();
        nodes = new Node[(1 << trie.getWidth())];
    }

    public boolean isBaseNode() {
        return level + 1 == maxDepth;
    }

    public Node[] getLeafNodes() {
        return nodes;
    }

    /**
     * Gets the nibble Info for the nibble specified by the filter.
     * @param filter the BloomFilter to get the nibble from.
     * @param level the level of the BFTrie we are at.
     * @return the NibbleInfo for that level
     */
    public byte getChunk(long[] buffer, int level) {
        int startBit = level * trie.getWidth();

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
    public void add(BFTrie trie, BloomFilter filter, long[] buffer) {
        int chunk = trie.getIndex(buffer, level);
        if (nodes[chunk] == null) {
            if ((level + 1) == maxDepth) {
                nodes[chunk] = new LeafNode(maxDepth == (shape.getNumberOfBits() / trie.getWidth()));
            } else {
                nodes[chunk] = new InnerNode(level + 1, shape, trie);
            }
        }
        nodes[chunk].add(trie, filter, buffer);
    }

    @Override
    public boolean find(long[] buffer) {
        byte nibble = getChunk(buffer, level);
        if (nodes[nibble] != null) {
            return nodes[nibble].find(buffer);
        }
        return false;
    }

    @Override
    public boolean remove(long[] buffer) {
        byte nibble = getChunk(buffer, level);
        if (nodes[nibble] != null) {
            if (nodes[nibble].remove(buffer)) {
                if (nodes[nibble].isEmpty()) {
                    nodes[nibble] = null;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void search(BFTrie trie, Consumer<BloomFilter> consumer, long[] buffer) {
        int[] nodeIdxs = trie.lookup(buffer, level);
        for (int i : nodeIdxs) {
            if (nodes[i] != null) {
                nodes[i].search(trie, consumer, buffer);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("InnerNode d:%s", level);
    }

}
