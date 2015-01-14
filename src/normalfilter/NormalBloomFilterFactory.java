package normalfilter;

import geoname.GeoName;
import test.BloomFilter;
import test.BloomFilterFactory;

public class NormalBloomFilterFactory implements BloomFilterFactory {
	private final NormalBloomFilter.Builder builder;

	// see http://hur.st/bloomfilter
	// values for 3 items 1/100,000 collisions.
	private static final int K = 17;
	public static final int WIDTH = 72;

	// values for 3 items 1/1,000,000 collisions
	// private static final int K = 20;
	// private static final int WIDTH = 87;

	public NormalBloomFilterFactory() {
		builder = new NormalBloomFilter.Builder( WIDTH, K);
	}

	@Override
	public BloomFilter create(final GeoName gn) {
		builder.reset();
		builder.add(gn.name);
		builder.add(gn.country_code);
		builder.add(gn.feature_code);
		return builder.build();
	}

	@Override
	public BloomFilter create(final String text) {
		builder.reset();
		builder.add(text);
		return builder.build();
	}
	
	public NormalBloomFilter.Builder getBuilder()
	{
		return new NormalBloomFilter.Builder( WIDTH, K);
	}
	
}
