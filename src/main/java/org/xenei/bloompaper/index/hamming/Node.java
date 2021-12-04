package org.xenei.bloompaper.index.hamming;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.FrozenBloomFilter;

public class Node implements Comparable<Node> {
    protected static BloomFilter empty;
    private final FrozenBloomFilter wrapped;

    private int hash;
    protected int count;
    protected Double log;

    static void setEmpty( Shape shape ) {
        empty = new SimpleBloomFilter( shape );
    }

    public Node(BloomFilter filter) {
        this.wrapped = FrozenBloomFilter.makeInstance(filter);
        this.count = 1;
        this.hash = Integer.hashCode( getHamming() );
        this.log = null;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals( Object other )
    {
        return other instanceof Node ? this.compareTo( (Node)other ) == 0 : false;
    }

    public FrozenBloomFilter getFilter() {
        return wrapped;
    }

    public boolean decrement() {
        this.count--;
        return this.count <= 0;
    }

    public void increment() {
        this.count++;
    }

    public int getCount(Consumer<BloomFilter> func) {
        func.accept( wrapped );
        return this.count;
    }

    public int getHamming() {
        return wrapped.cardinality();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        wrapped.forEachBitMap( (word) -> sb.append( String.format("%016X", word)));

        return String.format( "%s n=%s h=%s l=%s, 0x%s", BitUtils.format( wrapped.getBitMap() ),
                count, getHamming(), getLog(), sb );
    }

    /**
     * The the log2 of this bloom filter. This is the base 2 logarithm of this bloom
     * filter if thie bits in this filter were considers the bits in an unsigned
     * integer.
     *
     * @return the base 2 logarithm
     */
    public Double getLog() {
        if (log == null) {
            log = getApproximateLog(wrapped.getShape().getNumberOfBits());
        }
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
    private double getApproximateLog(int depth) {
        int[] exp = getApproximateLogExponents(depth);
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
    private final int[] getApproximateLogExponents(int depth) {
        int[] exp = new int[depth + 1];

        exp[0] = BitUtils.maxSet(wrapped.getBitMap());
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            exp[i] = BitUtils.maxSetBefore(wrapped.getBitMap(), exp[i - 1]);
            if (exp[i] - exp[0] < -25) {
                exp[i] = -1;
            }
            if (exp[i] == -1) {
                return exp;
            }
        }
        return exp;
    }

    /**
     * Construct a node that searches for the next higher hamming value and the
     * hamming value of this node. If this node is a search node it returns a node
     * with the same hamming and the calculated log.
     *
     * @return a search node.
     */
    public LimitNode lowerLimitNode() {
        return new LowerLimitNode(this);
    }

    /**
     * Construct a node that searches for the next higher hamming value and a log of
     * 0.0.
     *
     * @return a search node.
     */
    public LimitNode upperLimitNode() {
        return new UpperLimitNode(this);
    }

    private abstract class LimitNode extends Node {
        protected int hamming;
        protected LimitNode(Node node) {
            super(empty);
            this.hamming = node.getHamming()+1;
            this.count = 0;
        }
        @Override
        public final int getHamming() {
            return hamming;
        }

    }
    private class LowerLimitNode extends LimitNode {
        public LowerLimitNode(Node node) {
            super(node);
            this.log = node.getLog();
        }
    }

    private class UpperLimitNode extends LimitNode {

        private final double previousLog;
        public UpperLimitNode(Node node) {
            super(node);
            this.log = 0.0;
            this.previousLog = node.getLog();
        }

        @Override
        public LimitNode lowerLimitNode() {
            LimitNode retval = new LowerLimitNode(this);
            retval.hamming--;
            retval.log = previousLog;
            return retval;
        }
    }

    @Override
    public int compareTo(Node other) {
        if (this == other) {
            return 0;
        }
        int result = Integer.compare(this.getHamming(), other.getHamming());
        if (result == 0) {
            result = Double.compare(this.getLog(), other.getLog());
            if (result == 0) {
                result = wrapped.compareTo( other.wrapped );
            }
        }
        return result;
    }

}
