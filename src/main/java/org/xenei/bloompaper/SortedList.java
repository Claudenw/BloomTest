package org.xenei.bloompaper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class SortedList<T> implements List<T> {

    List<T> wrapped;
    Comparator<? super T> comparator;

    public SortedList() {
        wrapped = new ArrayList<T>();
    }

    public SortedList(Comparator<T> comparator) {
        wrapped = new ArrayList<T>();
        this.comparator = comparator;
    }

    private int makeIndex(int candidate) {
        return candidate < 0 ? (-1 - candidate) : candidate;
    }

    @SuppressWarnings("unchecked")
    private int search(T t) {
        if (comparator == null) {
            return Collections.binarySearch(((List<? extends Comparable<? super T>>) wrapped), t);
        } else {
            return Collections.binarySearch(wrapped, t, comparator);
        }
    }

    /**
     * Get the location to insert a value
     *
     * @param t the value to insert.
     * @return the insertion point.
     */
    public int getInsertPoint(T t) {
        return makeIndex(search(t));
    }

    /**
     * Gets the location to start searching from
     *
     * @param t the value to search for.
     * @return the search point
     */
    public int getSearchPoint(T t) {
        int idx = search(t);
        if (idx < 0) {
            return makeIndex(idx);
        }
        // because we accept duplicates we have to back up
        for (int idx2 = idx - 1; idx2 >= 0; idx2--) {
            int comp;
            if (comparator == null) {
                @SuppressWarnings("unchecked")
                Comparable<? super T> ct = (Comparable<? super T>) t;
                comp = ct.compareTo(get(idx2));
            } else {
                comp = comparator.compare(t, get(idx2));
            }
            if (comp != 0) {
                return idx2 + 1;
            }
        }
        return 0;
    }

    @Override
    public boolean add(T arg0) {
        if (wrapped.isEmpty()) {
            wrapped.add(arg0);
        } else {
            int idx = getInsertPoint(arg0);
            wrapped.add(idx, arg0);
        }
        return true;
    }

    @Override
    public void add(int arg0, T arg1) {
        this.add(arg1);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void sort() {
        Object[] a = wrapped.toArray();
        if (comparator == null) {
            Arrays.sort(a);
        } else {
            Arrays.sort(a, (Comparator) comparator);
        }
        ListIterator<T> i = wrapped.listIterator();
        for (int j = 0; j < a.length; j++) {
            i.next();
            i.set((T) a[j]);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        boolean result = wrapped.addAll(arg0);
        sort();
        return result;
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        boolean result = wrapped.addAll(arg0, arg1);
        sort();
        return result;
    }

    @Override
    public void clear() {
        wrapped.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object arg0) {
        return search((T) arg0) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        return wrapped.containsAll(arg0);
    }

    @Override
    public boolean equals(Object arg0) {
        return wrapped.equals(arg0);
    }

    @Override
    public void forEach(Consumer<? super T> arg0) {
        wrapped.forEach(arg0);
    }

    @Override
    public T get(int arg0) {
        return wrapped.get(arg0);
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    @Override
    public int indexOf(Object arg0) {
        @SuppressWarnings("unchecked")
        int idx = search((T) arg0);
        return idx < 0 ? -1 : idx;
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return wrapped.iterator();
    }

    @Override
    public int lastIndexOf(Object arg0) {
        return wrapped.lastIndexOf(arg0);
    }

    @Override
    public ListIterator<T> listIterator() {
        return wrapped.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int arg0) {
        return wrapped.listIterator(arg0);
    }

    @Override
    public Stream<T> parallelStream() {
        return wrapped.parallelStream();
    }

    @Override
    public boolean remove(Object arg0) {
        return wrapped.remove(arg0);
    }

    @Override
    public T remove(int arg0) {
        return wrapped.remove(arg0);
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        return wrapped.removeAll(arg0);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return wrapped.removeIf(filter);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        wrapped.replaceAll(operator);
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        return wrapped.retainAll(arg0);
    }

    @Override
    public T set(int arg0, T arg1) {
        return wrapped.set(arg0, arg1);
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public void sort(Comparator<? super T> arg0) {
        this.comparator = arg0;
        sort();
    }

    @Override
    public Spliterator<T> spliterator() {
        return wrapped.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return wrapped.stream();
    }

    @Override
    public List<T> subList(int arg0, int arg1) {
        return wrapped.subList(arg0, arg1);
    }

    @Override
    public Object[] toArray() {
        return wrapped.toArray();
    }

    @Override
    public <E> E[] toArray(E[] arg0) {
        return wrapped.toArray(arg0);
    }

    @Override
    public <E> E[] toArray(IntFunction<E[]> generator) {
        return wrapped.toArray(generator);
    }

}