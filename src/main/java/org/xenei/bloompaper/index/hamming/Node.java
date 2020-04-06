package org.xenei.bloompaper.index.hamming;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.BloomFilterIndexer;

public class Node {
    private final BloomFilter filter;
    private int count;
    private Double log;
    private Integer hamming;

    public Node( BloomFilter filter )
    {
        this.filter = filter;
        this.count = 0;
        reset();
    }

    public BloomFilter getFilter() {
        return filter;
    }

    public void decrement() {
        this.count--;
        if (this.count < 0)
        {
            this.count = 0;
        }
    }

    public void merge( Node otherNode ) {
        this.filter.merge( otherNode.filter );
        this.count += otherNode.count;
        reset();
    }

    private void reset() {
        this.log = -1.0;
        this.hamming = -1;
    }

    public int getCount() {
        return this.count;
    }

    public Integer getHamming() {
        if (hamming == null) {
            hamming = filter.cardinality();
        }
        return hamming;
    }

    /**
     * The the log2 of this bloom filter. This is the base 2 logarithm of this
     * bloom filter if thie bits in this filter were considers the bits in an
     * unsigned integer.
     *
     * @return the base 2 logarithm
     */
    public Double getLog() {
        if (log == null)
        {
            log = getApproximateLog( filter.getShape().getNumberOfBits());
        }
        return log;
    }

    /**
     * Get the approximate log for this filter. If the bloom filter is considered as
     * an unsigned number what is the approximate base 2 log of that value. The
     * depth argument indicates how many extra bits are to be considered in the log
     * calculation. At least one bit must be considered. If there are no bits on
     * then the log value is 0.
     * @param depth the number of bits to consider.
     * @return the approximate log.
     */
    private double getApproximateLog(int depth) {
        int[] exp = getApproximateLogExponents( depth);
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

        long[] bits = filter.getBits();

        exp[0] = BloomFilterIndexer.maxSet( bits );
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            exp[i] = BloomFilterIndexer.maxSetBefore( bits, exp[i - 1]);
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
     * Construct a node that searches for the next higher hamming value.
     * @return a search node.
     */
    public Node lowerLimitNode() {
        Node searchNode = new Node( filter );
        searchNode.hamming = getHamming()+1;
        searchNode.log = getLog();
        return searchNode;
    }

    /**
     * Construct a node that searches for the next higher hamming value.
     * @return a search node.
     */
    public Node upperLimitNode() {
        Node searchNode = new Node( filter );
        searchNode.hamming = getHamming()+1;
        searchNode.log = Double.valueOf(0.0);
        return searchNode;
    }

    public interface NodeComparator  {

        public static Comparator<Node> COMPLETE = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int result = SEARCH.compare( o1,  o2 );
                if (result == 0) {
                    result = Arrays.compare(o1.filter.getBits(), o2.filter.getBits());
                }
                return result;
            }
        };

        public static Comparator<Node> SEARCH = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int result = Integer.compare( o1.getHamming(), o2.getHamming() );
                if (result == 0)
                {
                    result = Double.compare(o1.getLog(), o2.getLog() );
                }
                return result;
            }
        };



    }


}
