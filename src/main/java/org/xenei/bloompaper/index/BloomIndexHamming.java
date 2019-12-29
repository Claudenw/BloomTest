package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.InstrumentedBloomFilter;
import org.xenei.bloompaper.SortedList;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BloomIndexHamming extends BloomIndex {
    protected Map<Integer, HammingList> index;

    public BloomIndexHamming(int population, BloomFilter.Shape bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.index = new HashMap<Integer, HammingList>();
    }

    @Override
    public void add(InstrumentedBloomFilter filter) {
        Integer idx = filter.cardinality();
        HammingList hammingList = index.get(idx);
        if (hammingList == null) {
            hammingList = new HammingList();
            index.put(idx, hammingList);
        }
        hammingList.add(filter);
    }

    protected List<InstrumentedBloomFilter> pageGet( InstrumentedBloomFilter filter, int hFilter, Map.Entry<Integer,HammingList> entry )
    {
        List<InstrumentedBloomFilter> retval = new ArrayList<InstrumentedBloomFilter>();
        Iterator<InstrumentedBloomFilter> iter = null;
        if (entry.getKey() >= hFilter) {
            if (entry.getKey() == hFilter) {
                iter = entry.getValue().get(filter);
            }
            else {
                iter = entry.getValue().find(filter);
            }
            while (iter.hasNext()) {
                InstrumentedBloomFilter found = iter.next();
                if (found.contains(filter)) {
                    retval.add( found );
                }
            }
        }
        return retval;
    }


    protected int pageCount( InstrumentedBloomFilter filter, int hFilter, Map.Entry<Integer,HammingList> entry )
    {
        int retval = 0;
        Iterator<InstrumentedBloomFilter> iter = null;
        if (entry.getKey() >= hFilter) {
            if (entry.getKey() == hFilter) {
                iter = entry.getValue().get(filter);
            }
            else {
                iter = entry.getValue().find(filter);
            }
            while (iter.hasNext()) {
                InstrumentedBloomFilter found = iter.next();
                if (found.contains(filter)) {
                    retval++;
                }
            }
        }
        return retval;
    }

    @Override
    public int count(InstrumentedBloomFilter filter) {
        int retval = 0;
        int hFilter = filter.cardinality();

        for (Map.Entry<Integer,BloomIndexHamming.HammingList> entry : index.entrySet()) {
            retval += pageCount( filter, hFilter, entry );
        }
        return retval;
    }

    @Override
    public String getName() {
        return "Hamming";
    }

    public static class HammingList extends SortedList<InstrumentedBloomFilter> {

        public HammingList() {
            super(BloomComparator.INSTANCE);
        }

        public Iterator<InstrumentedBloomFilter> get(InstrumentedBloomFilter filter) {
            return new BloomIterator(filter, true);
        }

        public Iterator<InstrumentedBloomFilter> find(InstrumentedBloomFilter filter) {
            return new BloomIterator(filter, false);
        }

        public Iterator<InstrumentedBloomFilter> find(InstrumentedBloomFilter filter, double logLimit) {
            return new BloomIterator(filter, false, logLimit);
        }

        public class BloomIterator implements Iterator<InstrumentedBloomFilter> {
            private InstrumentedBloomFilter limit;
            private boolean limited;
            private int idx;
            private double logLimit;

            BloomIterator(InstrumentedBloomFilter start, boolean limited)
            {
                this(start, limited, Double.MAX_VALUE);
            }

            BloomIterator(InstrumentedBloomFilter start, boolean limited, double logLimit) {
                this.limit = start;
                this.limited = limited;
                this.logLimit = logLimit;
                this.idx = getSearchPoint(start);
            }

            @Override
            public boolean hasNext() {
                if (idx < 0 || idx >= size()) {
                    return false;
                }
                if (limited) {
                    return (0 == BloomComparator.INSTANCE.compare(limit, get(idx)));
                }
                return logLimit > get(idx).getLog();
            }

            @Override
            public InstrumentedBloomFilter next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(idx++);
            }

        }


    }

    public static class BloomComparator implements Comparator<InstrumentedBloomFilter> {
        public static BloomComparator INSTANCE = new BloomComparator();

        private BloomComparator() {
        }

        @Override
        public int compare(InstrumentedBloomFilter o1, InstrumentedBloomFilter o2) {
            return Double.compare(o1.getLog(), o2.getLog() );
        }
    }


}
