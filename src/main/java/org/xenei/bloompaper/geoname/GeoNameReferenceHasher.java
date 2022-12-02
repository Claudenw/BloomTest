package org.xenei.bloompaper.geoname;

import org.xenei.bloompaper.HasherCollection;
import org.xenei.bloompaper.index.NullHasher;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.EnhancedDoubleHasher;

public class GeoNameReferenceHasher {

    public static HasherCollection createHasherCollection(GeoName geoName) {
        HasherCollection hashers = new HasherCollection();
        hashers.add(hasherFor(geoName.name));
        hashers.add(hasherFor(geoName.feature_code));
        hashers.add(hasherFor(geoName.country_code));
        return hashers;
    }

    public static Hasher hasherFor(String s) {
        String n = s.trim();
        if (n.length() == 0) {
            return NullHasher.INSTANCE;
        }
        long[] longs = MurmurHash3.hash128(n.getBytes(StandardCharsets.UTF_8));
        return new EnhancedDoubleHasher(longs[0], longs[1]);
    }

}
