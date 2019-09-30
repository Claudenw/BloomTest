package org.xenei.bloompaper.index.btree.stack;

import java.util.List;
import java.util.Queue;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.btree.stack.BTree.QueueEntry;

public interface Node {
	public void add(BloomFilter filter);
	/**
	 * Return true if the node was removed.
	 * @param filter
	 * @return
	 */
	public boolean remove(BloomFilter filter);

	public void search(List<BloomFilter> results, QueueEntry entry, Queue<QueueEntry> queue);
	public int count(QueueEntry entry, Queue<QueueEntry> queue);
}
