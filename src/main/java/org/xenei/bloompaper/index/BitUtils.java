package org.xenei.bloompaper.index;

import java.util.Arrays;

import org.apache.commons.collections4.bloomfilter.BloomFilter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Contains functions to convert {@code int} indices into Bloom filter bit positions.
 */
public final class BitUtils {
    /** A bit shift to apply to an integer to divided by 64 (2^6). */
    private static final int DIVIDE_BY_64 = 6;

    /** Do not instantiate. */
    private BitUtils() {}

    /**
     * Check the index is positive.
     *
     * @param bitIndex the bit index
     * @throws IndexOutOfBoundsException if the index is not positive
     */
    public static void checkPositive(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("Negative bitIndex: " + bitIndex);
        }
    }

    /**
     * Gets the filter index for the specified bit index assuming the filter is using 64-bit longs
     * to store bits starting at index 0.
     *
     * <p>The index is assumed to be positive. For a positive index the result will match
     * {@code bitIndex / 64}.
     *
     * <p>The divide is performed using bit shifts. If the input is negative the behaviour
     * is not defined.
     *
     * @param bitIndex the bit index (assumed to be positive)
     * @return the filter index
     * @see #checkPositive(int)
     */
    public static int getLongIndex(int bitIndex) {
        // An integer divide by 64 is equivalent to a shift of 6 bits if the integer is positive.
        // We do not explicitly check for a negative here. Instead we use a
        // a signed shift. Any negative index will produce a negative value
        // by sign-extension and if used as an index into an array it will throw an exception.
        return bitIndex >> DIVIDE_BY_64;
    }

    /**
     * Gets the filter bit mask for the specified bit index assuming the filter is using 64-bit
     * longs to store bits starting at index 0. The returned value is a {@code long} with only
     * 1 bit set.
     *
     * <p>The index is assumed to be positive. For a positive index the result will match
     * {@code 1L << (bitIndex % 64)}.
     *
     * <p>If the input is negative the behaviour is not defined.
     *
     * @param bitIndex the bit index (assumed to be positive)
     * @return the filter bit
     * @see #checkPositive(int)
     */
    public static long getLongBit(int bitIndex) {
        // Bit shifts only use the first 6 bits. Thus it is not necessary to mask this
        // using 0x3f (63) or compute bitIndex % 64.
        // Note: If the index is negative the shift will be (64 - (bitIndex & 0x3f)) and
        // this will identify an incorrect bit.
        return 1L << bitIndex;
    }

    /**
     * Given a set of longs as a bit vector find the highest bit set
     * @param bits the set of longs as a bit vector
     * @return the highest bit set or -1 for none.
     */
    public static int maxSet( long[] bits ) {
        for (int longIndex=bits.length-1;longIndex>=0;longIndex--)
        {
            if ( bits[longIndex] > 0)
            {
                for (int bitIndex=Long.SIZE-1;bitIndex>=0;bitIndex--) {
                    if ((bits[longIndex] &  getLongBit( bitIndex )) > 0)
                    {
                        return (Long.SIZE*longIndex)+bitIndex;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Given a set of longs as a bit vector find the highest bit set lower than the bit specified.
     * @param bits the set of longs as a bit vector
     * @param before the index of the maximum position+1
     * @return the highest bit set or -1 for none.
     */
    public static int maxSetBefore( long[] bits, int before ) {
        int bitLimit = (before % 64)-1;
        int limit = getLongIndex( before );
        if (bitLimit<0)
        {
            limit--;
        }
        for (int longIndex=limit;longIndex>=0;longIndex--)
        {
            if ( bits[longIndex] > 0)
            {
                for (int bitIndex=bitLimit;bitIndex>=0;bitIndex--) {
                    if ((bits[longIndex] &  getLongBit( bitIndex )) > 0)
                    {
                        return (Long.SIZE*longIndex)+bitIndex;
                    }
                }
                bitLimit=Long.SIZE-1;
            }
        }
        return -1;
    }

    public static String format( long[] bits )
    {
        StringBuilder sb = new StringBuilder();
        for (long l : bits)
        {
            sb.append( l ).append(" ");
        }
        return sb.toString();
    }

    public static boolean chkBreak( BloomFilter filter, long... values )
    {
        long[] bits = filter.getBits();
        return Arrays.compare( bits,  values) == 0;
    }

    public static boolean isSet( long[] bits, int bitIdx )
    {
        int longRec = getLongIndex(bitIdx);
        return (longRec < bits.length) ? (bits[longRec] & getLongBit(bitIdx)) > 0 : false;
    }
}