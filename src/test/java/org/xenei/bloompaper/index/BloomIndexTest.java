package org.xenei.bloompaper.index;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.SimpleHasher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public  class BloomIndexTest {
    private static int HASHER_COUNT = 20;
    private static int INSERT_LIMIT = HASHER_COUNT+1;
    private final Shape shape = Shape.Factory.fromNP( 3 , 1.0 / 100000);
    private final Hasher[] hasher = new Hasher[HASHER_COUNT];
    private final int[] matches = new int[HASHER_COUNT];
    private String name;
    private Constructor<? extends BloomIndex> constructor;
    private BloomIndex underTest;


    public BloomIndexTest( String name, Constructor<? extends BloomIndex> constructor ) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.name = name;
        this.constructor = constructor;
        underTest = constructor.newInstance(INSERT_LIMIT, shape);
        for (int i=0;i<HASHER_COUNT;i++)
        {
            hasher[i] = new SimpleHasher(1,i+1);
        }
        for (int i=0;i<HASHER_COUNT;i++) {
            BloomFilter bf = new SimpleBloomFilter( shape, hasher[i] );
            for (int j=0;j<HASHER_COUNT;j++) {
                if (bf.contains( hasher[j] )) {
                    matches[j]++;
                }
            }
        }
    }

    @Parameters(name="{0}")
    public static Collection tests() throws NoSuchMethodException, SecurityException {
        org.xenei.bloompaper.Test.init();
        Object[][] objs = new Object[org.xenei.bloompaper.Test.constructors.size()][2];
        int i=0;
        for (Entry<String, Constructor<? extends BloomIndex>> entry : org.xenei.bloompaper.Test.constructors.entrySet())
        {
            objs[i++] = new Object[] { entry.getKey(), entry.getValue() };
        }
        return Arrays.asList( objs );
    }

    @Test
    public void testDelete() {
        assertEquals( 0, underTest.count() );
        underTest.add( new SimpleBloomFilter( shape, hasher[0] ) );
        underTest.delete( new SimpleBloomFilter( shape, hasher[0] )  );
        assertEquals( 0, underTest.count() );

        for (int i=0;i<HASHER_COUNT;i++) {
            underTest.add( new SimpleBloomFilter( shape, hasher[i] ));
        }
        assertEquals( HASHER_COUNT, underTest.count() );
        for (int i=0;i<HASHER_COUNT;i++) {
            underTest.delete( new SimpleBloomFilter( shape, hasher[i] ));
        }
        assertEquals( 0, underTest.count() );
    }

    @Test
    public void testAdd() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertEquals( 0, underTest.count() );
        underTest.add( new SimpleBloomFilter( shape, hasher[0] ) );
        assertEquals( 1, underTest.count() );

        for (int i=0;i<HASHER_COUNT;i++) {
            underTest.add( new SimpleBloomFilter( shape, hasher[i] ));
        }
        assertEquals( HASHER_COUNT+1, underTest.count() );
    }


    @Test
    public void testCount() {
        List<BloomFilter> capture = new ArrayList<BloomFilter>();
        underTest.setFilterCapture(capture);
        assertEquals( 0, underTest.count() );
        underTest.add( new SimpleBloomFilter( shape, hasher[0] ) );
        assertEquals( 1, underTest.count() );

        for (int i=1;i<hasher.length;i++) {
            underTest.add( new SimpleBloomFilter( shape, hasher[i] ));
        }

        Collection<BloomFilter> collection = new ArrayList<BloomFilter>();
        underTest.setFilterCapture(collection);
        for (int i=0;i<hasher.length;i++) {
            //System.out.println( String.format( "%s count %s : %s",  name, i, underTest.count( new SimpleBloomFilter( shape, hasher[i] ))));
            collection.clear();
            int actual = underTest.count( new SimpleBloomFilter( shape, hasher[i] ));
            if (matches[i] != actual) {
                System.out.println( String.format( "%s: %s ", name, i));
                for (BloomFilter bf : collection)
                {
                    System.out.print( String.format( "%10s", " "));
                    bf.forEachBitMap( (word) -> System.out.print( String.format("%016X", word)));
                    System.out.println();
                }
            }
           assertEquals( "error at position "+i, matches[i], actual);
        }

    }


}
