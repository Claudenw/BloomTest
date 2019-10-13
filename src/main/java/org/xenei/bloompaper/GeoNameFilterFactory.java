package org.xenei.bloompaper;

import org.apache.commons.collections4.bloomfilter.ProtoBloomFilter;
import org.xenei.bloompaper.geoname.GeoName;


public class GeoNameFilterFactory {

    /**
     * Create a bloom filter from the geoname name,
     * country_code and , feature_code
     *
     * @param gn
     */
    public static ProtoBloomFilter create(GeoName gn) {
        return ProtoBloomFilter.builder().with( gn.name )
        .with( gn.country_code)
        .with( gn.feature_code ).build();
    }

    public static ProtoBloomFilter create(String text) {
        return ProtoBloomFilter.builder().with( text ).build();
    }


}
