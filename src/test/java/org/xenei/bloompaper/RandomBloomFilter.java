package org.xenei.bloompaper;

import java.util.BitSet;
import java.util.Random;
import java.util.function.LongConsumer;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.xenei.bloompaper.index.BitUtils;


public class RandomBloomFilter extends SimpleBloomFilter {

    private static Random random = new Random();

    private static BitMapProducer randomBitMapProducer(Shape shape) {
        return new BitMapProducer() {
            int len = (int) Math.ceil( shape.getNumberOfBits() * 1.0 / Long.SIZE );

        @Override
        public void forEachBitMap(LongConsumer consumer) {
            long mask = 0;
            for ( int i=0;i<len;i++)
            {
                long word = random.nextLong();
                mask |= BitUtils.getLongBit(i);
                if (i == len-1) {
                    word &= mask;
                }
                consumer.accept(word);
            }
        }
    };
    }
    public RandomBloomFilter( Shape shape)
    {
        super(shape, randomBitMapProducer(shape) );
    }


    @Override
    public boolean mergeInPlace(BloomFilter other) {
        throw new IllegalStateException( new OperationNotSupportedException() );
    }
    @Override
    public boolean mergeInPlace(Hasher hasher) {
        throw new IllegalStateException( new OperationNotSupportedException() );
    }

}
