package org.xenei.bloompaper.index;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;
import org.xenei.bloompaper.hamming.HammingUtils;
import org.xenei.bloompaper.index.btree.InnerNode;

/**
 * Implementation that uses hamming based index.
 */
public class BloomIndexLimitedHamming extends BloomIndexHamming {
    private int count;

    public BloomIndexLimitedHamming(int population, BloomFilterConfiguration bloomFilterConfig) {
        super(population, bloomFilterConfig);
        this.count = 0;
    }

    public int externalCount(BloomFilter filter, List<BloomFilter> lst) {
        int count = 0;
        for (BloomFilter other : lst) {
            if (filter.matches(other)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<BloomFilter> get(BloomFilter filter) {
        int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), count, InnerNode.BUCKETS);
        if (filter.getHammingWeight() >= minHam) {
            return super.get(filter);
        } else {
            List<BloomFilter> retval = new ArrayList<BloomFilter>();
            for (HammingList hList : index.values()) {
                for (BloomFilter other : hList) {
                    if (filter.matches(other)) {
                        retval.add(other);
                    }
                }
            }
            return retval;
        }
    }

    @Override
    public int count(BloomFilter filter) {
        int minHam = HammingUtils.minimumHamming(bloomFilterConfig.getNumberOfBits(), count, InnerNode.BUCKETS);
        if (filter.getHammingWeight() >= minHam) {
            return super.count(filter);
        } else {
            int retval = 0;
            for (HammingList hList : index.values()) {
                for (BloomFilter other : hList) {
                    if (filter.matches(other)) {
                        retval++;
                    }
                }
            }
            return retval;
        }
    }

    @Override
    public String getName() {
        return "LimitedHamming";
    }

}
