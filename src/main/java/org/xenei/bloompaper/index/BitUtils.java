package org.xenei.bloompaper.index;

import java.nio.ByteBuffer;
import java.util.function.BiPredicate;
import java.util.function.LongPredicate;

import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.collections4.bloomfilter.BitMap;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.EnhancedDoubleHasher;

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

    /** Do not instantiate. */
    private BitUtils() {
    }

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
     * Given a set of longs as a bit vector find the highest bit set
     * @param bits the set of longs as a bit vector
     * @return the highest bit set or -1 for none.
     */
    public static int maxSet(long[] bits) {
        for (int longIndex = bits.length - 1; longIndex >= 0; longIndex--) {
            if (bits[longIndex] != 0) {
                for (int bitIndex = Long.SIZE - 1; bitIndex >= 0; bitIndex--) {
                    if ((bits[longIndex] & BitMap.getLongBit(bitIndex)) != 0) {
                        return (Long.SIZE * longIndex) + bitIndex;
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
    public static int maxSetBefore(long[] bits, int before) {
        for (int i = before - 1; i >= 0; i--) {
            if ((bits[BitMap.getLongIndex(i)] & BitMap.getLongBit(i)) != 0) {
                return i;
            }
        }
        return -1;
    }

    public static String format(long[] bits) {
        StringBuilder sb = new StringBuilder();
        for (long l : bits) {
            sb.append(l).append(" ");
        }
        return sb.toString();
    }

    public static String formatHex(long[] bits) {
        StringBuilder sb = new StringBuilder();
        for (long l : bits) {
            sb.append(String.format("0x%h ", l));
        }
        return sb.toString();
    }

    public static boolean isSet(long[] bits, int bitIdx) {
        int longRec = BitMap.getLongIndex(bitIdx);
        return (longRec < bits.length) ? (bits[longRec] & BitMap.getLongBit(bitIdx)) != 0 : false;
    }

    public static class BufferCompare implements LongPredicate {
        public static BiPredicate<Long, Long> exact = (x, y) -> x.equals(y);
        public static BiPredicate<Long, Long> bloom = (x, y) -> ((x.longValue() & y.longValue()) == y.longValue());

        BiPredicate<Long, Long> func;
        long[] bitMap;
        int i;

        public BufferCompare(BloomFilter filter, BiPredicate<Long, Long> func) {
            bitMap = filter.asBitMapArray();
            this.func = func;
        }

        @Override
        public boolean test(long value) {
            return i < bitMap.length && func.test(bitMap[i++], value);
        }

        public boolean matches(BloomFilter other) {
            i = 0;
            return other.forEachBitMap(this) ? i == bitMap.length : false;
        }
    }

    public static class ShardingHasherFactory {

        private ShardingHasherFactory() {
            // do not instantiate
        }

        public static Hasher asHasher(BloomFilter filter) {

            byte[] buf = new byte[ Long.BYTES * BitMap.numberOfBitMaps(filter.getShape().getNumberOfBits())];
            ByteBuffer bb = ByteBuffer.wrap(buf);
            filter.forEachBitMap( x -> {bb.putLong(x);return true;} );
            long[] longs = MurmurHash3.hash128x64( buf );
            return new EnhancedDoubleHasher(longs[0], longs[1]);

        }
    }
}