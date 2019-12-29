package org.xenei.bloompaper.index.bloofi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;


public class Bloofi {

    private InnerNode root;

    public Bloofi(int limit, BloomFilter.Shape shape) {
        root = new InnerNode( null, shape );
    }


    public void add(AbstractBloomFilter candidate)
    {
        root.add(candidate);
        while (root.getParent() != null)
        {
            root = root.getParent();
        }
    }

    public List<AbstractBloomFilter> get(BloomFilter filter)
    {
        List<AbstractBloomFilter> retval = new ArrayList<AbstractBloomFilter>();
        root.search(retval, filter);
        return retval;
    }

    public int count(BloomFilter filter)
    {
        return root.count( filter );
    }

}
