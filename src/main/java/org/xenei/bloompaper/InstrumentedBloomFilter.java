package org.xenei.bloompaper;

import java.util.BitSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntConsumer;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilter;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.StaticHasher;

public class InstrumentedBloomFilter extends AbstractBloomFilter {

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
    public InstrumentedBloomFilter(Hasher hasher, BloomFilter.Shape shape) {
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
    public InstrumentedBloomFilter(BloomFilter.Shape shape) {
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
    public int cardinality() {
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

    @Override
    public void merge(BloomFilter other) {
        verifyShape(other);
        if (other instanceof InstrumentedBloomFilter)
        {
            bitSet.or(((InstrumentedBloomFilter)other).bitSet);
        } else {
            bitSet.or(BitSet.valueOf(other.getBits()));
        }
        reset();
    }

    @Override
    public void merge(Hasher hasher) {
        verifyHasher(hasher);
        hasher.getBits(getShape()).forEachRemaining((IntConsumer) bitSet::set);
        reset();
    }

    @Override
    public int andCardinality(BloomFilter other) {
        if (other instanceof InstrumentedBloomFilter) {
            verifyShape(other);
            BitSet result = (BitSet) bitSet.clone();
            result.and(((InstrumentedBloomFilter)other).bitSet);
            return result.cardinality();
        }
        return super.andCardinality(other);
    }

    @Override
    public int orCardinality(BloomFilter other) {
        if (other instanceof InstrumentedBloomFilter)
        {
            verifyShape(other);
            BitSet result = (BitSet) bitSet.clone();
            result.or(((InstrumentedBloomFilter)other).bitSet);
            return result.cardinality();
        }
        return super.orCardinality(other);
    }

    @Override
    public int xorCardinality(BloomFilter other) {
        if (other instanceof InstrumentedBloomFilter)
        {
            verifyShape(other);
            BitSet result = (BitSet) bitSet.clone();
            result.xor(((InstrumentedBloomFilter)other).bitSet);
            return result.cardinality();
        }
        return super.xorCardinality(other);
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
