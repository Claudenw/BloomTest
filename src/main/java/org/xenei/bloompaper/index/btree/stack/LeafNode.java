package org.xenei.bloompaper.index.btree.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.btree.stack.BTree.QueueEntry;


public class LeafNode implements Node {
	private final List<BloomFilter> lst;
	private boolean checkEntries;

	public LeafNode(boolean checkEntries) {
		lst = new ArrayList<BloomFilter>();
	}

	@Override
    public void add(BloomFilter filter)
	{
		lst.add( filter );
	}

	@Override
	public boolean remove(BloomFilter filter) {
		lst.remove( lst.size()-1 );
		return lst.isEmpty();
	}

	@Override
	public void search(List<BloomFilter> result, QueueEntry entry, Queue<QueueEntry> queue) {
		if (checkEntries)
		{
			for (BloomFilter candidate : lst)
			{
				if( entry.getFilter().match(candidate))
				{
					result.add( candidate );
				}
			}
		}
		else
		{
			result.addAll( lst );
		}
	}

	@Override
	public int count(QueueEntry entry, Queue<QueueEntry> queue) {
		if (checkEntries)
		{
			int retval = 0;
			for (BloomFilter candidate : lst)
			{
				if( entry.getFilter().match(candidate))
				{
					retval++;
				}
			}
			return retval;
		}
		else
		{
			return lst.size();
		}
	}

	@Override
	public String toString()
	{
		return String.format( "LeafNode %s", lst );
	}
}
