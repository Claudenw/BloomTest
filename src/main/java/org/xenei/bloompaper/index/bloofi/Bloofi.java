package org.xenei.bloompaper.index.bloofi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Shape;


public class Bloofi {

    private InnerNode root;

    public Bloofi(int limit, Shape shape) {
        root = new InnerNode( null, shape );
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
