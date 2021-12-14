package org.xenei.bloompaper.geoname;

import org.apache.commons.collections4.bloomfilter.hasher.HasherCollection;
import org.apache.commons.collections4.bloomfilter.hasher.NullHasher;
import org.apache.commons.collections4.bloomfilter.hasher.SimpleHasher;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.Shape;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.MurmurHash3;

public class GeoNameHasher {

//    public final static int POPULATION = 3; // number of items in each filter
//    public final static double PROBABILITY = 1.0 / 2000000; // 1 in 2 million
//    public final static Shape shape = Shape.Factory.fromNP(POPULATION, PROBABILITY);

    public static Hasher createHasher(GeoName geoName) {
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
        return new SimpleHasher(longs[0], longs[1]);
    }

}
