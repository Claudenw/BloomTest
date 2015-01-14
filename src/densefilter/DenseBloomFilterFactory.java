package densefilter;

import java.nio.ByteBuffer;

import org.apache.cassandra.utils.MurmurHash;

import geoname.GeoName;
import test.BloomFilter;
import test.BloomFilterFactory;

public class DenseBloomFilterFactory implements BloomFilterFactory {

	public DenseBloomFilterFactory() {
	}
	
	/**
	 * Create a bloom filter from the geoname. murmur3 hash x3 of name,
	 * country_code, feature_code
	 * 
	 * @param gn
	 */
	@Override
	public DenseBloomFilter create(GeoName gn) {
		long a = 0;
		long b = 0;
		long[] result = new long[2];
		ByteBuffer bb = ByteBuffer.wrap(gn.name.getBytes());
		MurmurHash.hash3_x64_128(bb, 0, bb.limit(), 1, result);

		a |= result[0];
		b |= result[1];

		bb = ByteBuffer.wrap(gn.country_code.getBytes());

		MurmurHash.hash3_x64_128(bb, 0, bb.limit(), 1, result);

		a |= result[0];
		b |= result[1];

		bb = ByteBuffer.wrap(gn.feature_code.getBytes());

		MurmurHash.hash3_x64_128(bb, 0, bb.limit(), 1, result);
		bb.position(0);
		a |= result[0];
		b |= result[1];

		return new DenseBloomFilter(a, b);
	}
	
	@Override
	public BloomFilter create(String text) {
	 long[] result = new long[2];
	ByteBuffer bb = ByteBuffer.wrap(text.getBytes());
	MurmurHash.hash3_x64_128(bb, 0, bb.limit(), 1, result);
	return new DenseBloomFilter( result[0], result[1]);
	}

}
