package org.xenei.bloompaper.index.bftrie;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;
import org.xenei.bloompaper.index.BitUtils;


public class InnerNode implements Node {
    public static final int WIDTH=4;
    public static final int BUCKETS=16; // 2^WIDTH
    private final Node[] nodes;
    private final int level;
    private final int maxDepth;
    private final Shape shape;

    public InnerNode(int level, Shape shape)
    {
        this( level, shape, shape.getNumberOfBits()/WIDTH);
    }

    public InnerNode(int level, Shape bloomFilterConfig, int maxDepth)
    {
        this.shape = bloomFilterConfig;
        this.level = level;
        this.maxDepth = maxDepth;
        nodes = new Node[BUCKETS];
    }

    public boolean isBaseNode()
    {
        return level+1 == maxDepth;
    }

    public Node[] getLeafNodes()
    {
        return nodes;
    }

    /**
     * Gets the nibble Info for the nibble specified by the filter.
     * @param filter the BloomFilter to get the nibble from.
     * @param level the level of the BFTrie we are at.
     * @return the NibbleInfo for that level
     */
    static public byte getNibble(BloomFilter filter, int level )
    {
        long[] buffer = filter.getBits();

        int startBit = level*4;



        int idx = BitUtils.getLongIndex(startBit);
        // buffer may  be short if upper values are zero
        if (idx >= buffer.length) {
            return (byte)0;
        }

        int shift = startBit % Long.SIZE;
        long mask = (0xFL << shift);
        long value = buffer[idx] & mask;
        return (byte)((value >> shift) & 0x0F);
    }

    @Override
    public void add(BFTrie4 btree, BloomFilter filter) {
        byte nibble = getNibble( filter, level );
        if (nodes[nibble] == null)
        {
            if ((level+1) == maxDepth)
            {
                nodes[nibble] = new LeafNode( maxDepth==(shape.getNumberOfBits()/WIDTH));
            }
            else
            {
                nodes[nibble] = new InnerNode( level+1, shape, maxDepth );
            }
        }
        nodes[nibble].add(btree,filter);
    }

    @Override
    public boolean find(BloomFilter filter) {
        byte nibble = getNibble( filter, level );
        if (nodes[nibble] != null)
        {
            return nodes[nibble].find(filter);
        }
        return false;
    }

    @Override
    public boolean remove(BloomFilter filter) {
        byte nibble = getNibble( filter, level );
        if (nodes[nibble] != null)
        {
            if (nodes[nibble].remove(filter))
            {
                if (nodes[nibble].isEmpty())
                {
                    nodes[nibble] = null;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (int i=0;i<BUCKETS;i++)
        {
            if (nodes[i] != null)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void search(List<BloomFilter> result, BloomFilter filter) {
        int[] nodeIdxs = BFTrie4.nibbleTable[getNibble(filter,level)];
        for (int i : nodeIdxs)
        {
            if (nodes[i] != null)
            {
                nodes[i].search( result, filter );
            }
        }
    }

    @Override
    public int count(BloomFilter filter) {
        int retval = 0;
        int[] nodeIdxs = BFTrie4.nibbleTable[getNibble(filter,level)];
        for (int i : nodeIdxs)
        {
            if (nodes[i] != null)
            {
                retval += nodes[i].count(filter);
            }
        }
        return retval;
    }

    @Override
    public String toString()
    {
        return String.format( "InnerNode d:%s", level );
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        for (Node n : nodes) {
            if (n != null)
            {
                n.setFilterCapture(collection);
            }
        }
    }

}
