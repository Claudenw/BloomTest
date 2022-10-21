package org.xenei.bloompaper.index.naturalbloofi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import org.apache.commons.collections4.bloomfilter.BloomFilter;

public class Node extends NodeContainer implements Comparable<Node> {

    public static class BFComp implements LongPredicate {
        /**
         * The calculated cardinality
         */
        private int result = 0;
        /**
         * The index into the array of BitMaps
         */
        private int idx = 0;
        /**
         * The array of BitMaps
         */
        private long[] bitMaps;

        BFComp(long[] bitMaps) {
            this.bitMaps = bitMaps;
        }

        public long[] getBitmap() {
            return bitMaps;
        }

        @Override
        public boolean test(long bitMap) {
            if (idx < bitMaps.length) {
                int r = Long.compareUnsigned(bitMaps[idx++], bitMap);
                if (r != 0) {
                    result = r;
                }
            } else {
                result = 1;
                return false;
            }
            return true;
        }

        public int getResult() {
            return (idx < bitMaps.length) ? -1 : result;
        }

        public void reset() {
            idx = 0;
        }
    };

    public static final Comparator<long[]> BM_COMPARATOR = new Comparator<long[]>() {

        @Override
        public int compare(long[] o1, long[] o2) {
            int result = Integer.compare(o1.length, o2.length);
            int i = o1.length - 1;
            while (i >= 0 && result == 0) {
                result = Long.compareUnsigned(o1[i], o2[i]);
                i--;
            }
            return result;
        }
    };

    final long[] bitMap;
    private List<Integer> ids;
    private final int hashValue;
    private final double log;

    public Node(NodeContainer parent, BloomFilter bloomFilter, int id) {
        super(parent, null);
        this.bitMap = bloomFilter == null ? new long[0] : bloomFilter.asBitMapArray();

        this.ids = new ArrayList<>(1);
        this.ids.add(id);
        this.log = bloomFilter == null ? (double) Integer.MAX_VALUE
                : getApproximateLog(getApproximateLogExponents(bloomFilter));
        this.hashValue = Double.hashCode(log);
        if (parent != null) {
            parent.addChild(this);
        }
    }

    @Override
    public int getId() {
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Node) {
            return compareTo((Node) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashValue;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public double getLog() {
        return log;
    }

    /**
     * Get the approximate log for this filter. If the bloom filter is considered as
     * an unsigned number what is the approximate base 2 log of that value. The
     * depth argument indicates how many extra bits are to be considered in the log
     * calculation. At least one bit must be considered. If there are no bits on
     * then the log value is 0.
     *
     * @param depth the number of bits to consider.
     * @return the approximate log.
     */
    static double getApproximateLog(int[] exp) {
        /*
         * this approximation is calculated using a derivation of
         * http://en.wikipedia.org/wiki/Binary_logarithm#Algorithm
         */
        // the mantissa is the highest bit that is turned on.
        if (exp[0] < 0) {
            // there are no bits so return 0
            return 0;
        }
        double result = exp[0];
        // now we move backwards from the highest bit until the requested
        // is achieved.
        double exp2;
        for (int i = 1; i < exp.length; i++) {
            if (exp[i] == -1) {
                return result;
            }
            exp2 = exp[i] - exp[0]; // should be negative
            result += Math.pow(2.0, exp2);
        }
        return result;
    }

    /**
     * The mantissa of the log in in position position 0. The remainder are
     * characteristic powers.
     *
     * @param depth
     * @return An array of depth integers that are the exponents.
     */
    static final int[] getApproximateLogExponents(BloomFilter filter) {
        // reverse order set
        Set<Integer> exponents = new TreeSet<Integer>(new Comparator<Integer>() {

            @Override
            public int compare(Integer x, Integer y) {
                return Integer.compare(y, x);
            }
        });

        filter.forEachIndex(x -> {
            exponents.add(x);
            return true;
        });
        int[] exp = new int[exponents.size() + 1];
        exp[exponents.size()] = -1;
        Iterator<Integer> iter = exponents.iterator();
        for (int i = 1; i < exponents.size(); i++) {
            exp[i] = iter.next().intValue();
            if (exp[i] - exp[0] < -25) {
                exp[i] = -1;
                return exp;
            }
        }
        return exp;
    }

    boolean contains(Node other) {
        return contains(other.bitMap);
    }

    boolean contains(long[] otherBitMap) {
        if (otherBitMap.length > bitMap.length) {
            return false;
        }
        for (int i = 0; i < otherBitMap.length; i++) {
            if ((bitMap[i] & otherBitMap[i]) != otherBitMap[i]) {
                return false;
            }
        }
        return true;
    }

    public void walkTree(Consumer<Node> consumer) {
        consumer.accept(this);
        forChildren((Consumer<Node>) (n -> n.walkTree(consumer)));
    }

    @Override
    public int compareTo(Node other) {
        int result = Double.compare(this.log, other.log);
        return result != 0 ? result : BM_COMPARATOR.compare(this.bitMap, other.bitMap);
    }
}
