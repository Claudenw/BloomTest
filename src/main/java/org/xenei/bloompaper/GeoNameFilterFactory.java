package org.xenei.bloompaper;

import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.DynamicHasher;
import org.apache.commons.collections4.bloomfilter.hasher.function.Murmur128x86Cyclic;
import org.xenei.bloompaper.geoname.GeoName;


public class GeoNameFilterFactory {

    /**
     * Create a bloom filter from the geoname name,
     * country_code and , feature_code
     *
     * @param gn
     */
    public static Hasher create(GeoName gn) {
        return new DynamicHasher.Builder( new Murmur128x86Cyclic() ).with( gn.name )
                .with( gn.country_code)
                .with( gn.feature_code ).build();
    }

    public static Hasher create(String text) {
        return new DynamicHasher.Builder( new Murmur128x86Cyclic() ).with( text ).build();
    }


}
