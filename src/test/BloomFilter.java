package test;

import java.nio.ByteBuffer;

import org.apache.cassandra.utils.MurmurHash;

import geoname.GeoName;
import hamming.ByteInfo;
import hamming.DoubleLong;
import hamming.NibbleInfo;

public interface BloomFilter {

	public boolean match(BloomFilter bf);
	
	public int getHammingWeight();
	
	public int getWidth();
	
	public ByteInfo getByte( int i);
	
	public NibbleInfo getNibble( int i );
	
	public void merge(BloomFilter other);
	
	public int getHammingDistance( BloomFilter other );
}
