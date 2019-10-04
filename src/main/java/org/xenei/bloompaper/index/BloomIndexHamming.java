package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.SortedList;

/**
 * Implementation that uses hamming based index.
 *
 * As set of lists is created based on hamming value. The lists are sorted by
 * estimated Log value.
 */
public class BloomIndexHamming extends BloomIndex {
    protected Map<Integer, HammingList> index;
    protected BloomFilterConfiguration bloomFilterConfig;

    public BloomIndexHamming(int population, BloomFilterConfiguration bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.index = new HashMap<Integer, HammingList>();
        this.bloomFilterConfig = bloomFilterConfig;
    }

    @Override
    public void add(BloomFilter filter) {
        Integer idx = filter.getHammingWeight();
        HammingList hammingList = index.get(idx);
        if (hammingList == null) {
            hammingList = new HammingList();
            index.put(idx, hammingList);
        }
        hammingList.add(filter);
    }

    @Override
    public List<BloomFilter> get(BloomFilter filter) {
        List<BloomFilter> retval = new ArrayList<BloomFilter>();
        int hFilter = filter.getHammingWeight();
        Iterator<BloomFilter> iter = null;
        for (Integer idx : index.keySet()) {
            if (idx >= hFilter) {
                if (idx == hFilter) {
                    iter = index.get(idx).get(filter);
                } else {
                    iter = index.get(idx).find(filter);
                }
                while (iter.hasNext()) {
                    BloomFilter found = iter.next();
                    if (filter.matches(found)) {
                        retval.add(found);
                    }
                }
            }
        }
        return retval;
    }

    @Override
    public int count(BloomFilter filter) {
        int retval = 0;
        int hFilter = filter.getHammingWeight();
        Iterator<BloomFilter> iter = null;
        for (Integer idx : index.keySet()) {
            if (idx >= hFilter) {
                if (idx == hFilter) {
                    iter = index.get(idx).get(filter);
                }
                else {
                    iter = index.get(idx).find(filter);
                }
                while (iter.hasNext()) {
                    BloomFilter found = iter.next();
                    if (filter.matches(found)) {
                        retval++;
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

    public static class HammingList extends SortedList<BloomFilter> {

        public HammingList() {
            super(BloomComparator.INSTANCE);
        }

        public Iterator<BloomFilter> get(BloomFilter filter) {
            return new BloomIterator(filter, true);
        }

        public Iterator<BloomFilter> find(BloomFilter filter) {
            return new BloomIterator(filter, false);
        }

        public class BloomIterator implements Iterator<BloomFilter> {
            private BloomFilter limit;
            private boolean limited;
            private int idx;

            BloomIterator(BloomFilter start, boolean limited) {
                this.limit = start;
                this.limited = limited;
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
                return true;
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

    public static class BloomComparator implements Comparator<BloomFilter> {
        public static BloomComparator INSTANCE = new BloomComparator();

        private BloomComparator() {
        }

        @Override
        public int compare(BloomFilter o1, BloomFilter o2) {
            return Double.compare(o1.getLog(), o2.getLog());
        }
    }

}
