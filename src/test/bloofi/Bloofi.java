package test.bloofi;

import java.util.ArrayList;
import java.util.List;

import test.BloomFilter;

public class Bloofi {
	
	private InnerNode root;
	
	public Bloofi(int width) {
		root = new InnerNode( null, width );
	}
	

	public void add(BloomFilter candidate)
	{
		root.add(candidate);
		while (root.getParent() != null)
		{
			root = root.getParent();
		}
	}
	
	public List<BloomFilter> get(BloomFilter filter)
	{
		List<BloomFilter> retval = new ArrayList<BloomFilter>();
		root.search(retval, filter);
		return retval;
	}

	public int count(BloomFilter filter)
	{
		return root.count( filter );
	}

}
