package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexHamming extends BloomIndex {
	// public Map<Integer,HammingList> index;
	// must be an array list.
	protected ArrayList<BloomFilter> lst;
	private final int[][] offsets;
	private final int bytes;
	protected int width;
	public static final int START = 0;
	public static final int LENGTH = 1;
	private final BloomComparator comparator;
	private static final int BINARYSEARCH_THRESHOLD = 5000;

	public BloomIndexHamming(final int limit, final int width) {
		super(limit, width);
		this.lst = new ArrayList<BloomFilter>(limit);
		this.width = width;
		offsets = new int[width + 1][2];
		bytes = Double.valueOf(Math.ceil(width / 8.0)).intValue();
		comparator = new BloomComparator();
	}

	@Override
	public void add(final BloomFilter filter) {
		final int hStart = filter.getHammingWeight();
		final List<BloomFilter> block = getBlock(hStart);
		int blockPos = binarySearch(block, filter, false);
		if (blockPos < 0) {
			blockPos = Math.abs(blockPos + 1);
		}

		// can add to block pos because it may be null list
		final int pos = offsets[hStart][START] + blockPos;
		lst.add(pos, filter);

		// adjust the indexes
		++offsets[hStart][LENGTH];
		for (int i = hStart + 1; i < filter.getWidth(); i++) {
			++offsets[i][START];
		}
	}

	private List<BloomFilter> getBlock(final int hStart) {

		if (offsets[hStart][LENGTH] > 0) {
			final int limit = offsets[hStart][START] + offsets[hStart][LENGTH];

			return lst.subList(offsets[hStart][START], limit);
		}
		return Collections.emptyList();
	}

	@Override
	public List<BloomFilter> get(final BloomFilter filter) {
		final List<BloomFilter> retval = new ArrayList<BloomFilter>();

		final int hStart = filter.getHammingWeight();

		// handle the exact match block
		List<BloomFilter> block = getBlock(hStart);
		if (block.size() > 0) {
			int pos = Collections.binarySearch(block, filter, comparator);
			if (pos >= 0) {
				while ((pos < block.size()) && filter.match(block.get(pos))) {
					retval.add(block.get(pos));
					pos++;
				}
			}

		}

		// handle the remaining blocks
		for (int idx = hStart + 1; idx <= width; idx++) {
			block = getBlock(idx);
			if (block.size() > 0) {
				int pos = Collections.binarySearch(block, filter, comparator);
				if (pos < 0) {
					pos = Math.abs(pos + 1);
				}
				for (int i = pos; i < block.size(); i++) {
					if (filter.match(block.get(i))) {
						retval.add(block.get(i));
					}
				}

			}
		}

		return retval;
	}

	@Override
	public int count(final BloomFilter filter) {
		int retval = 0;
		final int hStart = filter.getHammingWeight();

		// do direct check;
		// handle the exact match block
		List<BloomFilter> block = getBlock(hStart);
		if (block.size() > 0) {
			int pos = binarySearch(block, filter, true);
			if (pos >= 0) {
				while ((pos < block.size()) && filter.match(block.get(pos))) {
					retval++;
					pos++;
				}
			}

		}

		// handle the remaining blocks
		for (int idx = hStart + 1; idx <= width; idx++) {
			block = getBlock(idx);
			if (block.size() > 0) {
				int pos = binarySearch(block, filter, true);
				if (pos < 0) {
					pos = Math.abs(pos + 1);
				}
				for (int i = pos; i < block.size(); i++) {
					if (filter.match(block.get(i))) {
						retval++;
					}
				}

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

	private int binarySearch(final List<BloomFilter> l, final BloomFilter key,
			final boolean checkDups) {
		if (l.size() < BINARYSEARCH_THRESHOLD) {
			return indexedBinarySearch(l, key, checkDups);
		}
		else {
			return iteratorBinarySearch(l, key, checkDups);
		}
	}

	private int iteratorBinarySearch(final List<BloomFilter> l,
			final BloomFilter key, final boolean checkDups) {
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
					return adjustForDups(low, mid, l, key);

				}
				return mid; // key found
			}
		}
		return -(low + 1); // key not found
	}

	private int indexedBinarySearch(final List<BloomFilter> l,
			final BloomFilter key, final boolean checkDups) {
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
					return adjustForDups(low, mid, l, key);

				}
				return mid; // key found
			}
		}
		return -(low + 1); // key not found
	}

	private int adjustForDups(final int low, int mid,
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
	
	public int[][] getOffsets()
	{
		return offsets;
	}
	
	

}
