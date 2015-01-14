package test;

import geoname.GeoName;

public interface BloomFilterFactory {
	BloomFilter create( GeoName gn );
	BloomFilter create( String text);

}
