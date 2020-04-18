package org.xenei.bloompaper.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class NullCollection implements Collection<BloomFilter> {

    public static NullCollection INSTANCE = new NullCollection();

    private NullCollection() {
    }

    @Override
    public boolean add(BloomFilter arg0) {
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends BloomFilter> arg0) {
        return true;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean contains(Object arg0) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Iterator<BloomFilter> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean remove(Object arg0) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object[] toArray() {
        return Collections.EMPTY_LIST.toArray();
    }

    @Override
    public <T> T[] toArray(T[] arg0) {
        return Collections.emptyList().toArray(arg0);
    }

}
