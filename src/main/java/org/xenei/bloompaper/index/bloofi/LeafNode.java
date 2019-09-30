package org.xenei.bloompaper.index.bloofi;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class LeafNode implements Node {
	private final BloomFilter filter;
	private int count;

	public LeafNode(InnerNode parent, BloomFilter candidate) {
		this.filter = candidate;
		this.count = 1;
	}

	@Override
	public void setParent(InnerNode parent)
	{
	}

	@Override
	public void add(BloomFilter filter)
	{
		++count;
	}

	@Override
	public boolean remove(BloomFilter filter) {
		count = count>0?count-1:0;
		return count == 0;
	}

	@Override
	public void search(List<BloomFilter> result, BloomFilter filter) {
		if (filter.match(this.filter))
		{
			for (int i=0;i<count;i++)
			{
				result.add( this.filter );
			}
		}
	}

	@Override
	public int count(BloomFilter filter) {
		if (filter.match(this.filter))
		{
			return count;
		}
		return 0;
	}

	@Override
	public String toString()
	{
		return String.format( "LeafNode %s", filter );
	}

	@Override
	public BloomFilter getFilter() {
		return filter;
	}
}
