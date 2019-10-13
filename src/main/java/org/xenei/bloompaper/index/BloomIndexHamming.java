package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.apache.commons.collections4.bloomfilter.BloomFilterFunctions;
import org.xenei.bloompaper.SortedList;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BloomIndexHamming extends BloomIndex {
    protected Map<Integer, HammingList> index;

    public BloomIndexHamming(int population, BloomFilterConfiguration bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.index = new HashMap<Integer, HammingList>();
    }

    @Override
    public void add(BloomFilter filter) {
        Integer idx = filter.getHammingWeight();
        HammingList hammingList = index.get(idx);
        if (hammingList == null) {
            hammingList = new HammingList();
            index.put(idx, hammingList);
        }
        hammingList.add(new HammingEntry(filter));
    }

    protected List<BloomFilter> pageGet( BloomFilter filter, int hFilter, Map.Entry<Integer,HammingList> entry )
    {
        List<BloomFilter> retval = new ArrayList<BloomFilter>();
        Iterator<BloomFilter> iter = null;
        if (entry.getKey() >= hFilter) {
            if (entry.getKey() == hFilter) {
                iter = entry.getValue().get(filter);
            }
            else {
                iter = entry.getValue().find(filter);
            }
            while (iter.hasNext()) {
                BloomFilter found = iter.next();
                if (filter.matches(found)) {
                    retval.add( found );
                }
            }
        }
        return retval;
    }


    protected int pageCount( BloomFilter filter, int hFilter, Map.Entry<Integer,HammingList> entry )
    {
        int retval = 0;
        Iterator<BloomFilter> iter = null;
        if (entry.getKey() >= hFilter) {
            if (entry.getKey() == hFilter) {
                iter = entry.getValue().get(filter);
            }
            else {
                iter = entry.getValue().find(filter);
            }
            while (iter.hasNext()) {
                BloomFilter found = iter.next();
                if (filter.matches(found)) {
                    retval++;
                }
            }
        }
        return retval;
    }

    @Override
    public int count(BloomFilter filter) {
        int retval = 0;
        int hFilter = filter.getHammingWeight();

        for (Map.Entry<Integer,BloomIndexHamming.HammingList> entry : index.entrySet()) {
            retval += pageCount( filter, hFilter, entry );
        }
        return retval;
    }

    @Override
    public String getName() {
        return "Hamming";
    }

    public static class HammingEntry implements BloomFilter {
        BloomFilter wrapped;
        double log;

        HammingEntry(BloomFilter filter)
        {
            wrapped = filter;
            log = BloomFilterFunctions.getApproximateLog(filter);
        }

        @Override
        public boolean inverseMatches(BloomFilter other) {
            return wrapped.inverseMatches(other);
        }

        @Override
        public boolean matches(BloomFilter other) {
            return wrapped.matches(other);
        }

        @Override
        public int getHammingWeight() {
            return wrapped.getHammingWeight();
        }

        @Override
        public BloomFilter merge(BloomFilter other) {
            return wrapped.merge(other);
        }

        @Override
        public BitSet getBitSet() {
            return wrapped.getBitSet();
        }

        public double getLog() {
            return log;
        }

    }

    public static class HammingList extends SortedList<HammingEntry> {

        public HammingList() {
            super(BloomComparator.INSTANCE);
        }

        public Iterator<BloomFilter> get(BloomFilter filter) {
            return new BloomIterator(filter, true);
        }

        public Iterator<BloomFilter> find(BloomFilter filter) {
            return new BloomIterator(filter, false);
        }

        public Iterator<BloomFilter> find(BloomFilter filter, double logLimit) {
            return new BloomIterator(filter, false, logLimit);
        }

        public class BloomIterator implements Iterator<BloomFilter> {
            private HammingEntry limit;
            private boolean limited;
            private int idx;
            private double logLimit;

            BloomIterator(BloomFilter start, boolean limited)
            {
                this(start, limited, Double.MAX_VALUE);
            }

            BloomIterator(BloomFilter start, boolean limited, double logLimit) {
                this.limit = new HammingEntry(start);
                this.limited = limited;
                this.logLimit = logLimit;
                this.idx = getSearchPoint(limit);
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
            public BloomFilter next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(idx++);
            }

        }

    }

    public static class BloomComparator implements Comparator<HammingEntry> {
        public static BloomComparator INSTANCE = new BloomComparator();

        private BloomComparator() {
        }

        @Override
        public int compare(HammingEntry o1, HammingEntry o2) {
            return Double.compare(o1.getLog(), o2.getLog());
        }
    }

}
