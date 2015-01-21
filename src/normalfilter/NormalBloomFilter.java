package normalfilter;

import geoname.GeoName;
import hamming.ByteInfo;
import hamming.NibbleInfo;

import java.nio.ByteBuffer;
import java.util.BitSet;

import org.apache.cassandra.utils.MurmurHash;

import test.BloomFilter;
import test.BloomFilterFactory;

public class NormalBloomFilter implements BloomFilter {
	private final byte[] data;
	private final int width;
	
	NormalBloomFilter(byte[] data, int width) {
		this.data = data;
		this.width = width;
	}

	private static int calcBytes(int width)
	{
		return Double.valueOf(Math.ceil( 1.0 * width / 8)).intValue();
	}
	
	public NormalBloomFilter(int width) {
		this( new byte[calcBytes(width)], width);
	}
	
	public boolean match(BloomFilter bf) {
		if (bf instanceof NormalBloomFilter)
		{
			byte[] other = ((NormalBloomFilter)bf).data;
			for (int i=0;i<data.length;i++)
			{
				
				if ( (data[i] & other[i]) != data[i] )
				{
					return false;
				}
			}
			return true;
		}
		throw new IllegalArgumentException( "BloomFilter is not an instance of "+NormalBloomFilter.class);
	}
	
	@Override
	public int getHammingDistance(BloomFilter other) {
		if (other.getWidth() != this.getWidth())
		{
			throw new  IllegalArgumentException( "This instance can only get hamming distance to bloom filters with width of "+getWidth());
		}
		int result = 0;
		for (int i=0;i<data.length;i++)
		{
			result += ByteInfo.BYTE_INFO[(0xFF & (data[i] ^ other.getByte(i).getVal()))].getHammingWeight();
		}
		return result;
	}
	
	public void merge(BloomFilter bf)
	{
		if (bf.getWidth() != this.getWidth())
		{
			throw new  IllegalArgumentException( "This instance can only merge bloom filters with width of "+getWidth());
		}
		for (int i=0;i<data.length;i++)
		{
			data[i] = (byte) (0xFF & (data[i] | bf.getByte(i).getVal()));
		}
	}
	
	public String toString()
	{	
		
		StringBuilder sb = new StringBuilder();
		for (byte b : data)
		{
			sb.append( ByteInfo.BYTE_INFO[ 0xFF & b ].getHexPattern());
		}
		return sb.append( "(").append(getHammingWeight()).append(")").toString();
	}
	
	@Override
	public int getHammingWeight() {
		int value = 0;
		for (byte b : data)
		{
			value += ByteInfo.BYTE_INFO[ 0xFF & b ].getHammingWeight();
		}
		return value;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public ByteInfo getByte(int i) {
		int ii = 0xFF & data[i];
		return ByteInfo.BYTE_INFO[ii];
	}

	@Override
	public NibbleInfo getNibble(int i) {
		return getByte( i/2 ).getNibbles()[ i % 2];
	}
	
	@Override
	public boolean equals( Object o )
	{
		if (o instanceof BloomFilter)
		{
			BloomFilter other = (BloomFilter)o;
			if (getWidth() != other.getWidth())
			{
				return false;
			}
			for (int i=0;i<data.length;i++)
			{
				if (data[i] != other.getByte(i).getVal())
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override 
	public int hashCode()
	{
		return getByte(0).hashCode();
	}
	
	public static class Builder {
		private int k;
		private int width;
		private int bytes;
		private BitSet bitSet;

		
		public Builder( int width, int k )
		{
			this.k = k;
			this.width = width;
			this.bytes = Double.valueOf( Math.ceil( width / 8.0)).intValue();
			reset();
		}
		
		public Builder reset()
		{
			bitSet = new BitSet(width);
			return this;
		}
		
		public Builder add(String s)
		{
			return add( ByteBuffer.wrap(s.getBytes()));
		}
		
		public Builder set(int i)
		{
			bitSet.set(i);
			return this;
		}
		
		public Builder add(ByteBuffer bb)
		{
			long[] result = new long[2];
			for (int i=0;i<k;i++)
			{
				MurmurHash.hash3_x64_128(bb, 0, bb.limit(), i, result);
				bitSet.set( Math.abs((int)result[0]%width));
				//bitSet.set( Math.abs((int)result[1]%width));
			}
			return this;
		}
		
		public Builder add(final GeoName gn)
		{
			add(gn.name);
			add(gn.country_code);
			return add(gn.feature_code);
		}
		
		public BloomFilter build() {
			BloomFilter bf = this.build( this.bitSet );
			reset();
			return bf;
		}
		
		public BloomFilter build(BitSet bitset) {
			
			byte[] buff = new byte[bytes];
			byte[] data = bitSet.toByteArray();
			for (int i=0;i<data.length;i++)
			{
				buff[bytes-i-1]=data[i];
			}
			return  new NormalBloomFilter(buff, width );
		}

		public BitSet getBitSet()
		{
			return bitSet;
		}
		
		public static BitSet toBitSet(BloomFilter bf)
		{
			int bytes = bf.getWidth();
			byte[] buff = new byte[bytes];
			for (int i=0;i<buff.length;i++)
			{
				buff[bytes-i-1]=(byte) bf.getByte(i).getVal();
			}
			return BitSet.valueOf(buff);
		}
	}
}
