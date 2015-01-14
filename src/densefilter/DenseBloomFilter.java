package densefilter;

import hamming.ByteInfo;
import hamming.DoubleLong;
import test.BloomFilter;

public class DenseBloomFilter extends DoubleLong implements BloomFilter {

	public DenseBloomFilter(final long a, final long b) {
		super(a, b);
	}

	@Override
	public boolean match(final BloomFilter bf) {
		if (bf instanceof DenseBloomFilter) {
			final DenseBloomFilter bf2 = (DenseBloomFilter) bf;

			for (int i = 0; i < BYTES; i++) {
				if ((this.val[i].getVal() & bf2.val[i].getVal()) != this.val[i]
						.getVal()) {
					return false;
				}
			}
			return true;
		}
		throw new IllegalArgumentException("Filter was not an instance of "
				+ DenseBloomFilter.class);
	}

	@Override
	public int getHammingDistance(BloomFilter other) {
		if (other.getWidth() != this.getWidth())
		{
			throw new  IllegalArgumentException( "This instance can only get hamming distance to bloom filters with width of "+getWidth());
		}
		int result = 0;
		for (int i=0;i<BYTES;i++)
		{
			result += ByteInfo.BYTE_INFO[(0xFF & (val[i].getVal() ^ other.getByte(i).getVal()))].getHammingWeight();
		}
		return result;
	}
	
	public void merge(BloomFilter bf)
	{
		if (bf.getWidth() != this.getWidth())
		{
			throw new  IllegalArgumentException( "This instance can only merge bloom filters with width of "+getWidth());
		}
		for (int i=0;i<BYTES;i++)
		{
			val[i] = ByteInfo.BYTE_INFO[ (0xFF & (val[i].getVal() | bf.getByte(i).getVal()))];
		}
	}
	
	@Override
	public int getWidth() {
		return DoubleLong.WIDTH;
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
			for (int i=0;i<val.length;i++)
			{
				if (val[i].getVal() != other.getByte(i).getVal())
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
}
