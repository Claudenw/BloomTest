package org.xenei.bloompaper.index.hamming;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.index.BitUtils;

public class Node implements Comparable<Node> {
    protected static BloomFilter empty;
    private Shape shape;
    private long[] bitMap;
    protected int count;
    protected Double log;
    protected int hamming;

    static void setEmpty( Shape shape ) {
        empty = new SimpleBloomFilter( shape );
    }

    public Node(BloomFilter filter) {
        this.bitMap = BloomFilter.asBitMapArray(filter);
        this.shape = filter.getShape();
        this.hamming = filter.cardinality();
        this.count = 1;
        reset();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode( bitMap );
    }

    @Override
    public boolean equals( Object other )
    {
        return other instanceof Node ? this.compareTo( (Node)other ) == 0 : false;
    }

    public BloomFilter getFilter() {
        return new SimpleBloomFilter( shape, BitMapProducer.fromLongArray(bitMap));
    }

    public boolean decrement() {
        this.count--;
        return this.count <= 0;
    }

    public void increment() {
        this.count++;
    }

//    public void merge(Node otherNode) {
//        this.filter.merge(otherNode.filter);
//        this.count += otherNode.count;
//        reset();
//    }

    private void reset() {
        this.log = null;
    }

    public int getCount() {
        return this.count;
    }

    public int getHamming() {
        return hamming;
    }

    @Override
    public String toString() {
        return String.format( "%s n=%s h=%s l=%s", BitUtils.format( bitMap ),
                count, getHamming(), getLog());
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
            log = getApproximateLog(shape.getNumberOfBits());
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

        exp[0] = BitUtils.maxSet(bitMap);
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            exp[i] = BitUtils.maxSetBefore(bitMap, exp[i - 1]);
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
    public Node lowerLimitNode() {
        return new LowerLimitNode(this);
    }

    /**
     * Construct a node that searches for the next higher hamming value and a log of
     * 0.0.
     *
     * @return a search node.
     */
    public Node upperLimitNode() {
        return new UpperLimitNode(this);
    }

    private class LowerLimitNode extends Node {

        public LowerLimitNode(Node node) {
            super(empty);
            this.hamming = node.getHamming()+1;
            this.log = node.getLog();
            this.count = 0;
        }
    }

    private class UpperLimitNode extends Node {

        private final double previousLog;

        public UpperLimitNode(Node node) {
            super(empty);
            this.hamming = node.getHamming() + 1;
            this.log = 0.0;
            this.previousLog = node.getLog();
            this.count = 0;
        }

        @Override
        public Node lowerLimitNode() {
            Node retval = new LowerLimitNode(this);
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
                int limit = this.bitMap.length>other.bitMap.length? other.bitMap.length:this.bitMap.length;
                int i=0;
                while (i<limit && result == 0) {
                    result = Long.compare( other.bitMap[i], this.bitMap[i]);
                }
                if (result == 0) {
                    result = Integer.compare( this.bitMap.length, other.bitMap.length);
                }
            }
        }
        return result;
    }

}
