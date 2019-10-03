package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;


/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexHamming extends BloomIndex {
	public Map<Integer,HammingList> index;
	BloomFilterConfiguration config;

	public BloomIndexHamming( int population, BloomFilterConfiguration bloomFilterConfig )
	{
		super(population, bloomFilterConfig);
		this.index = new HashMap<Integer,HammingList>();
		this.config = bloomFilterConfig;
	}

	@Override
	public void add(BloomFilter filter)
	{
		Integer idx = filter.getHammingWeight();
		HammingList hammingList = index.get(idx);
		if (hammingList == null)
		{
			hammingList = new HammingList();
			index.put(idx, hammingList);
		}
		hammingList.add( filter );
	}

	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		List<BloomFilter> retval = new ArrayList<BloomFilter>();
		// do direct check;
		Integer hFilter = filter.getHammingWeight();
		Iterator<BloomFilter> iter = null;
		HammingList hList = null;
		for (Integer idx : index.keySet())
		{
			if (idx == hFilter)
			{
				hList = index.get(idx);
				if (hList != null)
				{
					iter = hList.get(filter);
					while (iter.hasNext())
					{
						retval.add( iter.next() );
					}
				}
			}
			if (idx > hFilter)
			{
				hList = index.get(idx);
				if (hList != null)
				{
					iter = hList.find(filter);
					while (iter.hasNext())
					{
						BloomFilter found = iter.next();
						if (filter.matches( found ))
						{
							retval.add( found );
						}
					}
				}
			}
		}
		return retval;
	}

	@Override
	public int count(BloomFilter filter)
	{
		int retval = 0;
		// do direct check;
		Integer hFilter = filter.getHammingWeight();
		Iterator<BloomFilter> iter = null;
		HammingList hList = null;
		for (Integer idx : index.keySet())
		{
			if (idx == hFilter)
			{
				hList = index.get(idx);
				if (hList != null)
				{
					iter = hList.get(filter);
					while (iter.hasNext())
					{
						retval++;
						iter.next();
					}
				}
			}
			if (idx > hFilter)
			{
				hList = index.get(idx);
				if (hList != null)
				{
					iter = hList.find(filter);
					while (iter.hasNext())
					{
    					BloomFilter found = iter.next();
    					if (filter.matches( found ))
    					{
    						retval++;
    					}
					}
				}
			}
		}
		return retval;
	}

	@Override
	public String getName() {
		return "Hamming";
	}


	public static class HammingList
	{
		private List<BloomFilter> lst;

		public HammingList()
		{
			lst = new ArrayList<BloomFilter>();
		}

		public void add(BloomFilter filter)
		{
			if (lst.isEmpty()) {
				lst.add(filter);
			}
			int i = Collections.binarySearch( lst, filter, BloomComparator.INSTANCE );
			if (i<0)
			{
			   lst.add(Math.abs(i+1), filter);
			}
			else {
			    lst.add(i, filter);
			}
		}

		public Iterator<BloomFilter> get(BloomFilter filter)
		{
			return new BloomIterator( filter, true );
		}

		public Iterator<BloomFilter> find(BloomFilter filter)
		{
			return new BloomIterator( filter, false );
		}

		public class BloomIterator implements Iterator<BloomFilter> {
			private BloomFilter limit;
			private boolean limited;
			private int idx;

			BloomIterator( BloomFilter start, boolean limited )
			{
				this.limit = start;
				this.limited = limited;
				this.idx = Collections.binarySearch( lst, limit, BloomComparator.INSTANCE );
			}

			@Override
			public boolean hasNext() {
				if (idx<0 || idx >= lst.size())
				{
					return false;
				}
				if (limited)
				{
					return (0 == BloomComparator.INSTANCE.compare(limit, lst.get(idx)));
				}
				return true;
			}

			@Override
			public BloomFilter next() {
				if (!hasNext())
				{
					throw new NoSuchElementException();
				}
				return lst.get(idx++);
			}

		}

	}

	public static class BloomComparator implements Comparator<BloomFilter> {
		public static BloomComparator INSTANCE = new BloomComparator();

		private BloomComparator(){}

		@Override
		public int compare(BloomFilter o1, BloomFilter o2) {
		   return  Double.compare( o1.getLog(), o2.getLog());
		}
	}


}
