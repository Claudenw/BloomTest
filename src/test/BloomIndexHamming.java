package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexHamming extends BloomIndex {
	// public Map<Integer,HammingList> index;
	// must be an array list.
	protected ArrayList<BloomFilter> lst;
	private HammingStats stats;
	private final int bytes;
	protected int width;
	protected final BloomComparator comparator;
	private static final int BINARYSEARCH_THRESHOLD = 5000;

	public BloomIndexHamming(final int limit, final int width) {
		super(limit, width);
		this.lst = new ArrayList<BloomFilter>(limit);
		this.width = width;

		stats = new HammingStats(width);
		bytes = Double.valueOf(Math.ceil(width / 8.0)).intValue();
		comparator = new BloomComparator();
	}

	@Override
	public void add(final BloomFilter filter) {
		stats.add( filter );
	}

	@Override
	public List<BloomFilter> get(final BloomFilter filter) {
		List<BloomFilter> retval = null;

		final int hStart = filter.getHammingWeight();
		if (hStart >= stats.getMinimumHamming())
		{
			// handle the exact match block
			Block block = stats.getBlock(hStart);
			if (block != null) {
				retval = block.get(filter);
			}
		}
		else
		{
			retval = new ArrayList<BloomFilter>();
		}
		
		// handle the remaining blocks
		int start = Math.max(hStart+1, stats.getMinimumHamming());
		for (int idx = start; idx <= stats.getMaximumHamming(); idx++) {
			Block block = stats.getBlock(idx);
			if ( block != null )
			{
				block.scan( filter, retval );
			}
		}

		return retval;
	}

	@Override
	public int count(final BloomFilter filter) {
		int retval = 0;
		final int hStart = filter.getHammingWeight();
		if (hStart >= stats.getMinimumHamming())
		{
			// handle the exact match block
			Block block = stats.getBlock(hStart);
			if (block != null) {
				retval = block.count(filter);
			}
		}
		
		
		// handle the remaining blocks
		int start = Math.max(hStart+1, stats.getMinimumHamming());
		for (int idx = start; idx <= stats.getMaximumHamming(); idx++) {
			Block block = stats.getBlock(idx);
			if ( block != null )
			{
				retval += block.countMatch( filter );
			}
		}

		return retval;
	}

	/**
	 * Gets the ith element from the given list by repositioning the specified
	 * list listIterator.
	 */
	private BloomFilter get(final ListIterator<BloomFilter> i, final int index) {
		BloomFilter obj = null;
		int pos = i.nextIndex();
		if (pos <= index) {
			do {
				obj = i.next();
			} while (pos++ < index);
		}
		else {
			do {
				obj = i.previous();
			} while (--pos > index);
		}
		return obj;
	}

	// From java.util.Collections.java

	/*
	 * Tuning parameters for algorithms - Many of the List algorithms have two
	 * implementations, one of which is appropriate for RandomAccess lists, the
	 * other for "sequential." Often, the random access variant yields better
	 * performance on small sequential access lists. The tuning parameters below
	 * determine the cutoff point for what constitutes a "small" sequential
	 * access list for each algorithm. The values below were empirically
	 * determined to work well for LinkedList. Hopefully they should be
	 * reasonable for other sequential access List implementations. Those doing
	 * performance work on this code would do well to validate the values of
	 * these parameters from time to time. (The first word of each tuning
	 * parameter name is the algorithm to which it applies.)
	 */

	protected int binarySearch(final List<BloomFilter> l, final BloomFilter key,
			final boolean checkDups, boolean findMax) {
		if (l instanceof RandomAccess || l.size() < BINARYSEARCH_THRESHOLD) {
			return indexedBinarySearch(l, key, checkDups, findMax);
		}
		else {
			return iteratorBinarySearch(l, key, checkDups, findMax);
			
		}
	}

	private int iteratorBinarySearch(final List<BloomFilter> l,
			final BloomFilter key, final boolean checkDups, final boolean findMax) {
		int low = 0;
		int high = l.size() - 1;
		final ListIterator<BloomFilter> i = l.listIterator();

		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final BloomFilter midVal = get(i, mid);
			final int cmp = comparator.compare(midVal, key);

			if (cmp < 0) {
			low = mid + 1;
		}
		else if (cmp > 0) {
			high = mid - 1;
		}
		else {
				if (checkDups) {
					if (findMax)
					{
						return adjustUpForDups(mid, high, l, key);
					}
					else
					{
						return adjustDownForDups(low, mid, l, key);
					}
				}
				return mid; // key found
			}
		}
		return -(low + 1); // key not found
	}

	private int indexedBinarySearch(final List<BloomFilter> l,
			final BloomFilter key, final boolean checkDups, final boolean findMax) {
		int low = 0;
		int high = l.size() - 1;

		while (low <= high) {
			final int mid = (low + high) >>> 1;
				final BloomFilter midVal = l.get(mid);
			final int cmp = comparator.compare(midVal, key);

			if (cmp < 0) {
					low = mid + 1;
				}
				else if (cmp > 0) {
					high = mid - 1;
				}
				else {
				if (checkDups) {
					if (findMax)
					{
						return adjustUpForDups(mid, high, l, key);
					}
					else
					{
						return adjustDownForDups(low, mid, l, key);
					}

				}
				return mid; // key found
			}
		}
		return -(low + 1); // key not found
	}

	private int adjustDownForDups(final int low, int mid,
			final List<BloomFilter> l, final BloomFilter key) {
		BloomFilter midVal = null;
		while (mid > low) {
			mid--;
			midVal = l.get(mid);
			if (0 != comparator.compare(midVal, key)) {
				return mid + 1;
			}
		}
		return mid;
	}
	
	private int adjustUpForDups(int mid, final int high,
			final List<BloomFilter> l, final BloomFilter key) {
		BloomFilter midVal = null;
		while (mid < high) {
			mid++;
			midVal = l.get(mid);
			if (0 != comparator.compare(midVal, key)) {
				return mid - 1;
			}
		}
		return mid;
	}

	@Override
	public String getName() {
		return "Hamming";
	}

	public class BloomComparator implements Comparator<BloomFilter> {

		private BloomComparator() {
		}

		@Override
		public int compare(final BloomFilter o1, final BloomFilter o2) {
			for (int i = 0; i < bytes; i++) {
				final int chk = o1.getByte(i).getVal() - o2.getByte(i).getVal();
				if (chk != 0) {
					return chk;
				}
			}
			return 0;
		}
	}
	
	protected void setStats( HammingStats stats )
	{
		this.stats = stats;
	}
	
	public HammingStats getStats()
	{
		return stats;
	}
	
	public class HammingStats {
		private int minHamming;
		private int maxHamming;
		private int[][] blocks;
		
		public HammingStats(int width) {
			blocks = new int[width+1][2];
			minHamming = width+1;
			maxHamming = 0;
		}
		
		public int getMinimumHamming()
		{
			return minHamming;
		}
		
		public int getMaximumHamming()
		{
			return maxHamming;
		}
		
		public void add(BloomFilter filter)
		{
			final int hStart = filter.getHammingWeight();
			if (hStart < minHamming)
			{
				minHamming = hStart;
			}
			if (hStart > maxHamming)
			{
				maxHamming = hStart;
			}
			Block block = newBlock(blocks[hStart]);
			block.add( filter );
			
			// adjust the indexes
			for( int i =hStart+1;i<blocks.length;i++)
			{
				blocks[i][Block.START]++;
			}		
		}
		
		protected Block newBlock(int[] block)
		{
			return new Block(block);
		}
		
		public Block getBlock( int hamming )
		{
			if (hamming<minHamming || hamming>maxHamming || blocks[hamming][Block.LENGTH] == 0)
			{
				return null;
			}
			return newBlock(blocks[hamming]);
		}
		
	}
	
	public class Block {
		private int [] data;
		private static final int START=0;
		private static final int LENGTH=1;
		
		Block(int[] data) {
			this.data=data;
		}
		
		public int getLength()
		{
			return data[LENGTH];
		}
		
		protected List<BloomFilter> getSubList()
		{
			final int limit =data[START] + data[LENGTH];
			return lst.subList(data[START], limit);
		}
		
		public void add(BloomFilter filter)
		{
			int blockPos = 0;
			if (data[LENGTH]>0)
			{
				List<BloomFilter> subList = getSubList();
				blockPos = binarySearch(subList, filter, false, false);
				if (blockPos < 0) {
					blockPos = Math.abs(blockPos + 1);
				}
			}
			// can add to block pos because it may be null list
			final int pos = data[START] + blockPos;
			lst.add(pos, filter);
			data[LENGTH]++;
		}
		
		public void incrementStart()
		{
			data[START]++;
		}
		
		// 0 length blocks are filtered out before here
		public List<BloomFilter> get(BloomFilter filter)
		{
			List<BloomFilter> subList = getSubList();
			int pos = binarySearch(subList, filter, true, false);
			if (pos >= 0) {
				// we found some.
				BloomFilter candidate;
				List<BloomFilter> retval = new ArrayList<BloomFilter>();
				while (pos < data[LENGTH]) {
					 candidate = subList.get(pos);
					 if( filter.match(candidate)) {
						 retval.add(subList.get(pos));
					 }
					pos++;
				}
				return retval;
			}
			return Collections.emptyList();
		}

		// TODO optimize this based on size
		// exact match
		public int count(BloomFilter filter) 
		{
			int retval = 0;
			List<BloomFilter> subList = getSubList();
			int pos = binarySearch(subList, filter, true, false);
			if (pos >= 0) {
				// we found some.

				while (pos < data[LENGTH] && filter.match(subList.get(pos))) {
					retval++;
					pos++;
				}
			}
			return retval;
		}
				
		// range match
		public void scan(BloomFilter filter, List<BloomFilter> result)
		{
			List<BloomFilter> subList = getSubList();
			int pos = binarySearch(subList, filter, true, false);
			if (pos < 0) {
				pos = Math.abs(pos + 1);
			}
			for (int i = pos; i < data[LENGTH]; i++) {
				if (filter.match(subList.get(i))) {
					result.add(subList.get(i));
				}
			}
		}
		
		public int countMatch(BloomFilter filter)
		{
			int retval = 0;
			List<BloomFilter> subList = getSubList();
			int pos = binarySearch(subList, filter, true, false);
			if (pos < 0) {
				pos = Math.abs(pos + 1);
			}
			for (int i = pos; i < data[LENGTH]; i++) {
				if (filter.match(subList.get(i))) {
					retval++;
				}
			}
			return retval;
		}
		
	}

}
