package org.xenei.bloompaper;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntConsumer;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;

public class InstrumentedBloomFilter extends BloomFilter {

    private Integer hamming;
    private Double log;

    /**
     * The bitset that defines this BloomFilter.
     */
    private BitSet bitSet;

    /**
     * Constructs a InstrumentedBloomFilter from a hasher and a shape.
     *
     * @param hasher the Hasher to use.
     * @param shape the desired shape of the filter.
     */
    public InstrumentedBloomFilter(Hasher hasher, Shape shape) {
        this(shape);
        verifyHasher(hasher);
        hasher.getBits(shape).forEachRemaining((IntConsumer) bitSet::set);
        reset();
    }

    /**
     * Constructs an empty InstrumentedBloomFilter.
     *
     * @param shape the desired shape of the filter.
     */
    public InstrumentedBloomFilter(Shape shape) {
        super(shape);
        this.bitSet = new BitSet();
        reset();
    }

    @Override
    public long[] getBits() {
        return bitSet.toLongArray();
    }

    @Override
    public StaticHasher getHasher() {
        return new StaticHasher(bitSet.stream().iterator(), getShape());
    }

    @Override
    public void merge(BloomFilter other) {
        verifyShape(other);
        bitSet.or(BitSet.valueOf(other.getBits()));
        reset();
    }

    @Override
    public boolean contains(Hasher hasher) {
        verifyHasher(hasher);
        OfInt iter = hasher.getBits(getShape());
        while (iter.hasNext()) {
            if (!bitSet.get(iter.nextInt())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hammingValue() {
        if (hamming == null)
        {
            hamming = bitSet.cardinality();
        }
        return hamming;
    }

    @Override
    public String toString() {
        return bitSet.toString();
    }

    /**
     * Merge another InstrumentedBloomFilter into this one. <p> This method takes advantage of
     * internal structures of InstrumentedBloomFilter. </p>
     *
     * @param other the other InstrumentedBloomFilter.
     * @see #merge(BloomFilter)
     */
    public void merge(InstrumentedBloomFilter other) {
        verifyShape(other);
        bitSet.or(other.bitSet);
        reset();
    }

    @Override
    public void merge(Hasher hasher) {
        verifyHasher(hasher);
        hasher.getBits(getShape()).forEachRemaining((IntConsumer) bitSet::set);
        reset();
    }

    /**
     * Calculates the andCardinality with another InstrumentedBloomFilter. <p> This method takes
     * advantage of internal structures of InstrumentedBloomFilter. </p>
     *
     * @param other the other InstrumentedBloomFilter.
     * @return the cardinality of the result of {@code ( this AND other )}.
     * @see #andCardinality(BloomFilter)
     */
    public int andCardinality(InstrumentedBloomFilter other) {
        verifyShape(other);
        BitSet result = (BitSet) bitSet.clone();
        result.and(other.bitSet);
        return result.cardinality();
    }

    /**
     * Calculates the orCardinality with another InstrumentedBloomFilter. <p> This method takes
     * advantage of internal structures of InstrumentedBloomFilter. </p>
     *
     * @param other the other InstrumentedBloomFilter.
     * @return the cardinality of the result of {@code ( this OR other )}.
     * @see #orCardinality(BloomFilter)
     */
    public int orCardinality(InstrumentedBloomFilter other) {
        verifyShape(other);
        BitSet result = (BitSet) bitSet.clone();
        result.or(other.bitSet);
        return result.cardinality();
    }

    /**
     * Calculates the xorCardinality with another InstrumentedBloomFilter. <p> This method takes
     * advantage of internal structures of InstrumentedBloomFilter. </p>
     *
     * @param other the other InstrumentedBloomFilter.
     * @return the cardinality of the result of {@code( this XOR other )}
     * @see #xorCardinality(BloomFilter)
     */
    public int xorCardinality(InstrumentedBloomFilter other) {
        verifyShape(other);
        BitSet result = (BitSet) bitSet.clone();
        result.xor(other.bitSet);
        return result.cardinality();
    }

    private void reset() {
        hamming = null;
        log = null;
    }



    /**
     * The the log(2) of this bloom filter. This is the base 2 logarithm of this
     * bloom filter if thie bits in this filter were considers the bits in an
     * unsigned integer.
     *
     * @return the base 2 logarithm
     */
    public double getLog() {
        if (log == null)
        {
            log = getApproximateLog( getShape().getNumberOfBits());
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
     * @see AbstractBloomFilter.getApproximateLog()
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

        exp[0] = bitSet.length() - 1;
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            exp[i] = bitSet.previousSetBit(exp[i - 1] - 1);
            if (exp[i] - exp[0] < -25) {
                exp[i] = -1;
            }
            if (exp[i] == -1) {
                return exp;
            }
        }
        return exp;
    }
}
