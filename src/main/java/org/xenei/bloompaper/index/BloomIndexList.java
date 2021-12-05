package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexList extends BloomIndex {
    private List<BloomFilter> index;
    private Collection<BloomFilter> filterCapture;

    public BloomIndexList(int population, Shape shape) {
        super(population, shape);
        this.index = new ArrayList<BloomFilter>(population);
    }

    @Override
    public void add(BloomFilter filter) {
        index.add(filter);
    }

    @Override
    public void delete(BloomFilter filter) {
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare(filter, BitUtils.BufferCompare.exact);

        Iterator<BloomFilter> iter = index.iterator();
        while (iter.hasNext()) {
            if (comp.matches(iter.next())) {
                iter.remove();
                return;
            }
        }
    }

    @Override
    public String getName() {
        return "List";
    }

    @Override
    public int count() {
        return index.size();
    }

    @Override
    public void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        for (BloomFilter candidate : index ) {
            if (candidate.contains( filter )) {
                consumer.accept(candidate);
            }
        }
    }

}
