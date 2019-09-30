package org.xenei.bloompaper.index.btree.stack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;


public class BTree   {
	public static final int[][] nibbleTable = {
		{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF},
		{1, 3, 5, 7, 9, 0xB, 0xD, 0xF},
		{2, 3, 6, 7, 0xA, 0xB, 0xE, 0xF},
		{3, 7, 0xB, 0xF},
		{4, 5, 6, 7, 0xC, 0xD, 0xE, 0xF},
		{5, 7, 0xD, 0xF},
		{6, 7, 0xE, 0xF},
		{7, 0xF},
		{8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF},
		{9, 0xB, 0xD, 0xF},
		{0xA, 0xB, 0xE, 0xF},
		{0xB, 0xF},
		{0xC, 0xD, 0xE, 0xF},
		{0xD, 0xF},
		{0xE, 0xF},
		{0xF},
	};

	public static class QueueEntry {
		private Node nextNode;
		private BloomFilter filter;

		public QueueEntry( Node nextNode, BloomFilter filter )
		{
			this.nextNode = nextNode;
			this.filter = filter;
		}

		public Node getNextNode() {
			return nextNode;
		}
		public BloomFilter getFilter() {
			return filter;
		}

		@Override
        public String toString()
		{
			return String.format( "%s : %s", filter, nextNode);
		}
	}

	private final BloomFilterConfiguration bloomFilterConfig;
	private InnerNode root;
	private int count;

	public BTree(BloomFilterConfiguration bloomFilterConfig)
	{
	    this.bloomFilterConfig = bloomFilterConfig;
		root = new InnerNode(0, bloomFilterConfig);
		count = 0;
	}

	public BTree( BloomFilterConfiguration bloomFilterConfig, int maxDepth )
	{
	    this.bloomFilterConfig = bloomFilterConfig;
		root = new InnerNode(0, bloomFilterConfig, maxDepth);
		count = 0;
	}


	public void add(BloomFilter filter) {
		root.add(filter);
		count++;
	}


	public void remove(BloomFilter filter) {
		root.remove(filter);
		count--;
	}

	public List<BloomFilter> search(BloomFilter filter) {
		// estimate result size as % of key space.
		int f = bloomFilterConfig.getNumberOfBits()-filter.getHammingWeight();
		int initSize = count * f / bloomFilterConfig.getNumberOfBits();
		Queue<QueueEntry> queue = new LinkedList<QueueEntry>();
		List<BloomFilter> retval = new ArrayList<BloomFilter>(initSize);
		QueueEntry entry = new QueueEntry( root, filter);
		while (entry != null)
		{
			entry.getNextNode().search(retval, entry, queue);
			entry = queue.poll();
		}
		return retval;
	}

	public int count(BloomFilter filter) {
		int retval = 0;
		Queue<QueueEntry> queue = new LinkedList<QueueEntry>();
		QueueEntry entry = new QueueEntry( root, filter);
		while (entry != null)
		{
			retval += entry.getNextNode().count(entry, queue);
			entry = queue.poll();
		}
		return retval;
	}

}
