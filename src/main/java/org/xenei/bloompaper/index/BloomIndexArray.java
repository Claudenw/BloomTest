package org.xenei.bloompaper.index;

import java.util.function.Consumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexArray extends BloomIndex {
    private BloomFilter[] index;
    private int idx;

    public BloomIndexArray(int population, Shape shape) {
        super(population, shape);
        this.index = new BloomFilter[population];
        this.idx = 0;
    }

    @Override
    public void add(BloomFilter filter) {
        index[idx++] = filter;
    }

    @Override
    public void delete(BloomFilter filter) {
        BitUtils.BufferCompare comp = new BitUtils.BufferCompare(filter, BitUtils.BufferCompare.exact);

        for (int i = idx - 1; i >= 0; i--) {
            if (comp.matches(index[i])) {
                if (i < idx - 1) {
                    System.arraycopy(index, i + 1, index, i, idx - i - 1);
                }
                index[--idx] = null;
                break;
            }
        }
    }

    @Override
    public String getName() {
        return "Linear";
    }

    @Override
    public int count() {
        return idx;
    }

    @Override
    public void doSearch(Consumer<BloomFilter> consumer, BloomFilter filter) {
        for (int i = 0; i < idx; i++) {
            if (index[i].contains(filter)) {
                consumer.accept(index[i]);
            }
        }
    }

}
