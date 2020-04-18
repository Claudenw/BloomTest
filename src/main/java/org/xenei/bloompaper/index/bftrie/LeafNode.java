package org.xenei.bloompaper.index.bftrie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class LeafNode implements Node {
    private final List<BloomFilter> lst;
    private boolean checkEntries;
    private Collection<BloomFilter> filterCapture;

    public LeafNode(boolean checkEntries) {
        this.checkEntries = checkEntries;
        lst = new ArrayList<BloomFilter>();
    }

    public List<BloomFilter> getList()
    {
        return lst;
    }

    @Override
    public void add(BFTrie4 btree, BloomFilter filter)
    {
        lst.add( filter );
    }

    @Override
    public boolean find(BloomFilter filter)
    {
        return !lst.isEmpty();
    }

    @Override
    public boolean remove(BloomFilter filter) {
        lst.remove( lst.size()-1 );
        return true;
    }

    @Override
    public boolean isEmpty() {
        return lst.isEmpty();
    }

    @Override
    public void search(List<BloomFilter> result, BloomFilter filter) {
        if (checkEntries)
        {
            for (BloomFilter candidate : lst)
            {
                if(candidate.contains( filter))
                {
                    result.add( candidate );
                }
            }
        }
        else
        {
            result.addAll( lst );
        }
    }

    @Override
    public int count(BloomFilter filter) {
        if (checkEntries)
        {
            int retval = 0;
            for (BloomFilter candidate : lst)
            {
                if(candidate.contains(filter))
                {
                    filterCapture.add( candidate );
                    retval++;
                }
            }
            return retval;
        }
        else
        {
            filterCapture.addAll( lst );
            return lst.size();
        }
    }

    @Override
    public String toString()
    {
        return String.format( "LeafNode %s", lst );
    }

    @Override
    public void setFilterCapture(Collection<BloomFilter> collection) {
        this.filterCapture = collection;
    }
}
