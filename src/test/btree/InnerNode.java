package test.btree;

import java.util.List;

import hamming.DoubleLong;
import hamming.NibbleInfo;
import test.BloomFilter;

public class InnerNode implements Node {
	public static final int WIDTH=4;
	public static final int BUCKETS=16; // 2^WIDTH
	private final Node[] nodes;
	private final int level;
	private final int maxDepth;
	
	public static int calcMaxDepth( int filterWidth )
	{
		return Double.valueOf( Math.ceil((1.0*filterWidth)/WIDTH )).intValue();
	}
	
	public InnerNode(int level, int filterWidth)
	{
		this.level = level;
		this.maxDepth = calcMaxDepth( filterWidth );
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
	
	@Override
	public void add(BTree btree, BloomFilter filter) {
		NibbleInfo nibble = filter.getNibble( level );
		if (nodes[nibble.getVal()] == null)
		{
			if ((level+1) == maxDepth)
			{
				nodes[nibble.getVal()] = new LeafNode( maxDepth==calcMaxDepth(filter.getWidth()));
			}
			else
			{
				nodes[nibble.getVal()] = new InnerNode( level+1, filter.getWidth() );
			}
		}
		nodes[nibble.getVal()].add(btree,filter);	
	}

	@Override
	public boolean remove(BloomFilter filter) {
		NibbleInfo nibble = filter.getNibble( level );
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
		int[] nodeIdxs = BTree.nibbleTable[filter.getNibble(level).getVal()];
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
		int[] nodeIdxs = BTree.nibbleTable[filter.getNibble(level).getVal()];
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
