package org.xenei.bloompaper.index.bftrie;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter.Shape;
import org.xenei.bloompaper.hamming.NibbleInfo;


public class InnerNode implements Node {
    public static final int WIDTH=4;
    public static final int BUCKETS=16; // 2^WIDTH
    private final Node[] nodes;
    private final int level;
    private final int maxDepth;
    private final Shape bloomFilterConfig;

    public InnerNode(int level, Shape bloomFilterConfig)
    {
        this( level, bloomFilterConfig, bloomFilterConfig.getNumberOfBits()/WIDTH);
    }

    public InnerNode(int level, Shape bloomFilterConfig, int maxDepth)
    {
        this.bloomFilterConfig = bloomFilterConfig;
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

    private NibbleInfo getNibble(BloomFilter filter, int level )
    {
        long[] buffer = filter.getBits();

        int idx = level / Long.BYTES;
        if (idx >= buffer.length) {
            return NibbleInfo.NIBBLE_INFO[0];
        }
        int ofs = Math.floorMod(level * 4, Long.SIZE);

        int nibble = (int) ((buffer[idx] >> ofs) & 0x0F);
        return NibbleInfo.NIBBLE_INFO[nibble];
        //	    byte[] buff = filter.getBitSet().toByteArray();
        //	    int idx = level/2;
        //	    if (idx>=buff.length)
        //	    {
        //	        return NibbleInfo.NIBBLE_INFO[0];
        //	    }
        //	    byte b = buff[idx];
        //	    if ( level % 2  == 0)
        //	    {
        //	        int x = 0x0F & (b >> 4);
        //	        return NibbleInfo.NIBBLE_INFO[x];
        //	    } else {
        //        int x = 0x0F & b;
        //        return  NibbleInfo.NIBBLE_INFO[x];
        //	    }
    }

    @Override
    public void add(BFTrie4 btree, BloomFilter filter) {
        NibbleInfo nibble = getNibble( filter, level );
        if (nodes[nibble.getVal()] == null)
        {
            if ((level+1) == maxDepth)
            {
                nodes[nibble.getVal()] = new LeafNode( maxDepth==(bloomFilterConfig.getNumberOfBits()/WIDTH));
            }
            else
            {
                nodes[nibble.getVal()] = new InnerNode( level+1, bloomFilterConfig, maxDepth );
            }
        }
        nodes[nibble.getVal()].add(btree,filter);
    }

    @Override
    public boolean remove(BloomFilter filter) {
        NibbleInfo nibble = getNibble( filter, level );
        if (nodes[nibble.getVal()] != null)
        {
            if (nodes[nibble.getVal()].remove(filter))
            {
                nodes[nibble.getVal()] = null;
            }
            for (int i=0;i<BUCKETS;i++)
            {
                if (nodes[i] != null)
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void search(List<BloomFilter> result, BloomFilter filter) {
        int[] nodeIdxs = BFTrie4.nibbleTable[getNibble(filter,level).getVal()];
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
        int[] nodeIdxs = BFTrie4.nibbleTable[getNibble(filter,level).getVal()];
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

}
