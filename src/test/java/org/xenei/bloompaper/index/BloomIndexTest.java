package org.xenei.bloompaper.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.SimpleBloomFilter;
import org.apache.commons.collections4.bloomfilter.hasher.Hasher;
import org.apache.commons.collections4.bloomfilter.hasher.SimpleHasher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xenei.bloompaper.index.BitUtils.BufferCompare;

@RunWith(Parameterized.class)
public class BloomIndexTest {
    private static int HASHER_COUNT = 200;
    private static int INSERT_LIMIT = HASHER_COUNT + 1;
    private final Shape shape = Shape.Factory.fromNP(3, 1.0 / 100000);
    private final Hasher[] hasher = new Hasher[HASHER_COUNT];
    private final int[] matches = new int[HASHER_COUNT];
    private final int[] duplicates = new int[HASHER_COUNT];
    private final Constructor<? extends BloomIndex> constructor;
    private String name;
    private BloomIndex underTest;

    public BloomIndexTest(String name, Constructor<? extends BloomIndex> constructor) {
        this.name = name;
        this.constructor = constructor;

        // setup the hashers
        for (int i = 0; i < HASHER_COUNT; i++) {
            hasher[i] = new SimpleHasher(1, i + 1);
        }

        // calculate the hasher collisions
        for (int i = 0; i < HASHER_COUNT; i++) {
            BloomFilter bf = new SimpleBloomFilter(shape, hasher[i]);
            BitUtils.BufferCompare comp = new BitUtils.BufferCompare(bf, BitUtils.BufferCompare.exact);
            for (int j = 0; j < HASHER_COUNT; j++) {
                BloomFilter test = new SimpleBloomFilter(shape, hasher[j]);
                if (bf.contains(test)) {
                    matches[j]++;
                }
                if (comp.matches(test)) {
                    duplicates[j]++;
                }
            }
        }

    }

    @Before
    public void setup()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        underTest = constructor.newInstance(INSERT_LIMIT, shape);
    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = "{0}")
    public static Collection tests() throws NoSuchMethodException, SecurityException {
        org.xenei.bloompaper.Test.init();
        Object[][] objs = new Object[org.xenei.bloompaper.Test.constructors.size()][2];
        int i = 0;
        for (Entry<String, Constructor<? extends BloomIndex>> entry : org.xenei.bloompaper.Test.constructors
                .entrySet()) {
            objs[i++] = new Object[] { entry.getKey(), entry.getValue() };
        }
        return Arrays.asList(objs);
    }

    @Test
    public void testDelete() {
        assertEquals(0, underTest.count());
        underTest.add(new SimpleBloomFilter(shape, hasher[0]));
        underTest.delete(new SimpleBloomFilter(shape, hasher[0]));
        assertEquals(0, underTest.count());

        for (int i = 0; i < HASHER_COUNT; i++) {
            underTest.add(new SimpleBloomFilter(shape, hasher[i]));
        }
        assertEquals(HASHER_COUNT, underTest.count());
        for (int i = 0; i < HASHER_COUNT; i++) {
            underTest.delete(new SimpleBloomFilter(shape, hasher[i]));
        }
        assertEquals(0, underTest.count());
    }

    @Test
    public void testDeleteMultiples() {

        for (int i = 0; i < HASHER_COUNT; i++) {
            underTest.add(new SimpleBloomFilter(shape, hasher[i]));
        }

        int expected = HASHER_COUNT;
        assertEquals(expected, underTest.count());

        // find the index with the highest duplicate count
        int max = -1;
        int idx = -1;

        for (int i = 0; i < HASHER_COUNT; i++) {
            if (duplicates[i] > max) {
                idx = i;
                max = duplicates[i];
            }
        }

        BloomFilter bf = new SimpleBloomFilter(shape, hasher[idx]);
        underTest.delete(bf);
        assertEquals(expected - 1, underTest.count());
        assertEquals(matches[idx] - 1, underTest.count(bf));

    }

    @Test
    public void testAdd()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertEquals(0, underTest.count());
        underTest.add(new SimpleBloomFilter(shape, hasher[0]));
        assertEquals(1, underTest.count());

        for (int i = 0; i < HASHER_COUNT; i++) {
            underTest.add(new SimpleBloomFilter(shape, hasher[i]));
        }
        assertEquals(HASHER_COUNT + 1, underTest.count());
    }

    @Test
    public void testCount() {
        assertEquals(0, underTest.count());
        underTest.add(new SimpleBloomFilter(shape, hasher[0]));
        assertEquals(1, underTest.count());

        for (int i = 1; i < hasher.length; i++) {
            underTest.add(new SimpleBloomFilter(shape, hasher[i]));
        }

        Collection<BloomFilter> collection = new ArrayList<BloomFilter>();
        // underTest.setFilterCapture(collection);
        for (int i = 0; i < hasher.length; i++) {
            // System.out.println( String.format( "%s count %s : %s", name, i,
            // underTest.count( new SimpleBloomFilter( shape, hasher[i] ))));
            collection.clear();
            int actual = underTest.count(new SimpleBloomFilter(shape, hasher[i]));
            if (matches[i] != actual) {
                System.out.println(String.format("%s: %s ", name, i));
                for (BloomFilter bf : collection) {
                    System.out.print(String.format("%10s", " "));
                    bf.forEachBitMap((word) -> {
                        System.out.print(String.format("%016X", word));
                        return true;
                    });
                    System.out.println();
                }
            }
            assertEquals("error at position " + i, matches[i], actual);
        }

    }

    @Test
    public void testRetrieval() {

        for (int i = 1; i < hasher.length; i++) {
            underTest.add(new SimpleBloomFilter(shape, hasher[i]));
        }

        Collection<BloomFilter> collection = new ArrayList<BloomFilter>();

        BloomFilter bf = new SimpleBloomFilter(shape, hasher[0]);
        underTest.search(collection::add, bf);

        BufferCompare buffComp = new BufferCompare(bf, BufferCompare.exact);
        boolean found = false;
        for (BloomFilter bf2 : collection) {
            found |= buffComp.matches(bf2);
        }
        assertTrue(found);
    }

}
