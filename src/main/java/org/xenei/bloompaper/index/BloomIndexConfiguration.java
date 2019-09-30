package org.xenei.bloompaper.index;

import org.apache.commons.collections4.bloomfilter.BloomFilterConfiguration;

public class BloomIndexConfiguration {
    private final BloomFilterConfiguration config;

    public BloomIndexConfiguration( BloomFilterConfiguration config)
    {
        this.config = config;
    }

    public BloomFilterConfiguration getConfig() {
        return config;
    }


}
