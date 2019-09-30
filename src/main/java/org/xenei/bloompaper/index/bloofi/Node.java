package org.xenei.bloompaper.index.bloofi;

import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public interface Node {
	public void setParent( InnerNode parent );
	public BloomFilter getFilter();

	public void add(BloomFilter filter);
	/**
	 * Return true if the node was removed.
	 * @param filter
	 * @return
	 */
	public boolean remove(BloomFilter filter);
	public void search(List<BloomFilter> results, BloomFilter filter);
	public int count(BloomFilter filter);
}
