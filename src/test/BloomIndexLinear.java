package test;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain ol' linear search.
 *
 */
public class BloomIndexLinear extends BloomIndex {
	BloomFilter[] index;
	int idx;
	
	public BloomIndexLinear(int limit, int width)
	{
		super(limit, width);
		this.index = new BloomFilter[limit];
		this.idx = 0;
	}

	@Override
	public void add(BloomFilter filter)
	{
		index[idx++] = filter;
	}
	
	@Override
	public List<BloomFilter> get(BloomFilter filter)
	{
		List<BloomFilter> result = new ArrayList<BloomFilter>();
				// searching entire list
				for (BloomFilter candidate : index)
				{
					if (filter.match(candidate))
					{
						result.add(candidate);
					}
				}
			return result;
	
	}

	@Override
	public int count(BloomFilter filter)
	{
		int result = 0;
				// searching entire list
				for (BloomFilter candidate : index)
				{
					if (filter.match(candidate))
					{
						result++;
					}
				}
			return result;
	
	}

	@Override
	public String getName() {
		return "Linear";
	}

}
