package test.btree;

import java.util.List;
import test.BloomFilter;

public interface Node {
	public void add(BTree btree, BloomFilter filter);
	/**
	 * Return true if the node was removed.
	 * @param filter
	 * @return
	 */
	public boolean remove(BloomFilter filter);
	
	public void search(List<BloomFilter> results, BloomFilter filter);
	public int count(BloomFilter filter);
}
