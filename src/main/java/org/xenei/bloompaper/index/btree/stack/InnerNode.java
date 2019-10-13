package org.xenei.bloompaper.index.btree.stack;

import java.util.List;
import java.util.Queue;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.hamming.NibbleInfo;
import org.xenei.bloompaper.index.btree.stack.BTree.QueueEntry;


public class InnerNode implements Node {
	public static final int WIDTH=4;
	private static final int BUCKETS=16; // 2^WIDTH
	private final Node[] nodes;
	private final int level;
	private final int maxDepth;
	private final BloomFilterConfiguration bloomFilterConfig;

	public InnerNode(int level,BloomFilterConfiguration bloomFilterConfig)
	{
		this( level, bloomFilterConfig, bloomFilterConfig.getNumberOfBits()/WIDTH);
	}

	public InnerNode(int level,BloomFilterConfiguration bloomFilterConfig, int maxDepth)
	{
	    this.bloomFilterConfig = bloomFilterConfig;
		this.level = level;
		this.maxDepth = maxDepth;
		nodes = new Node[BUCKETS];
	}

	private NibbleInfo getNibble(BloomFilter filter, int level )
    {
	    int idx = level/2;
	    byte[] buff = filter.getBitSet().toByteArray();
	    if (idx>=buff.length)
        {
            return NibbleInfo.NIBBLE_INFO[0];
        }
        byte b = buff[idx];
        if ( level % 2  == 0)
        {
            int x = 0x0F & (b >> 4);
            return NibbleInfo.NIBBLE_INFO[x];
        } else {
        int x = 0x0F & b;
        return  NibbleInfo.NIBBLE_INFO[x];
        }
    }

	@Override
	public void add(BloomFilter filter) {
		NibbleInfo nibble = getNibble( filter,level );
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
		nodes[nibble.getVal()].add(filter);
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
	public void search(List<BloomFilter> result, QueueEntry entry, Queue<QueueEntry> queue) {
		BloomFilter filter = entry.getFilter();
		int[] nodeIdxs = BTree.nibbleTable[getNibble(filter,level).getVal()];
		for (int i : nodeIdxs)
		{
			if (nodes[i] != null)
			{
				queue.add( new QueueEntry( nodes[i], filter ));
			}
		}
	}

	@Override
	public int count(QueueEntry entry, Queue<QueueEntry> queue) {
		BloomFilter filter = entry.getFilter();
		int[] nodeIdxs = BTree.nibbleTable[getNibble(filter,level).getVal()];
		for (int i : nodeIdxs)
		{
			if (nodes[i] != null)
			{
				queue.add( new QueueEntry( nodes[i], filter ));
			}
		}
		return 0;
	}

	@Override
	public String toString()
	{
		return String.format( "InnerNode d:%s", level );
	}

}
