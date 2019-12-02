package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilter.Shape;

public class BloomIndexConfiguration {
    private final Shape config;

    public BloomIndexConfiguration( Shape config)
    {
        this.config = config;
    }

    public Shape getConfig() {
        return config;
    }


}
