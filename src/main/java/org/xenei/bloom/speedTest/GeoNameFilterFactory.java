package org.xenei.bloom.speedTest;


import org.apache.commons.collections4.bloomfilter.hasher.DynamicHasher;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.function.MD5Cyclic;
import org.xenei.bloom.speedTest.geoname.GeoName;


public class GeoNameFilterFactory {

    /**
     * Create a bloom filter from the geoname name,
     * country_code and , feature_code
     *
     * @param gn
     */
    public static Hasher create(GeoName gn) {
        return new DynamicHasher.Builder( new MD5Cyclic() ).with( gn.name )
        .with( gn.country_code)
        .with( gn.feature_code ).build();
    }

    public static Hasher create(String text) {
        return new DynamicHasher.Builder( new MD5Cyclic() ).with( text ).build();
    }


}
