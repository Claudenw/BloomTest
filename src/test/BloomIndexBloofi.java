package test;

import java.util.List;

import test.bloofi.Bloofi;


/**
 * Implementation of BTree Nibble search.
 *
 */
public class BloomIndexBloofi extends BloomIndex {
	Bloofi bloofi;
	
	public BloomIndexBloofi(int limit, int width)
	{
		super(limit, width);
		this.bloofi = new Bloofi(width);
	}

	@Override
	public void add(BloomFilter filter)
	{
		bloofi.add( filter );
	}
	
	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		return bloofi.get(filter);
	}

	@Override
	public int count(BloomFilter filter)
	{
		return bloofi.count(filter);
	}
	
	@Override
	public String getName() {
		return "Bloofi";
	}
	
}
