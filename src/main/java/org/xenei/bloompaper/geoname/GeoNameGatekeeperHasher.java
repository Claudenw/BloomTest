package org.xenei.bloompaper.geoname;

import org.apache.commons.collections4.bloomfilter.hasher.NullHasher;
import org.apache.commons.collections4.bloomfilter.hasher.SimpleHasher;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.MurmurHash3;

public class GeoNameGatekeeperHasher {

    public static Hasher createHasher(GeoName geoName) {
        return hasherFor(geoName.geonameid);
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
