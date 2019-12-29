package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class BloomIndexConfiguration {
    private final BloomFilter.Shape config;

    public BloomIndexConfiguration( BloomFilter.Shape config)
    {
        this.config = config;
    }

    public BloomFilter.Shape getConfig() {
        return config;
    }


}
