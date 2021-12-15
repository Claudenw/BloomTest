package org.xenei.bloompaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.Stats.Phase;
import org.xenei.bloompaper.Stats.Type;
import org.xenei.bloompaper.index.FrozenBloomFilter;

public class StatsSerdeTest {

    private Shape shape = new Shape(12, 12);

    private Stats.Serde serde = new Stats.Serde();

    public StatsSerdeTest() {

    }

    @Test
    public void testRoundTrip() throws IOException {
        Stats expected = new Stats("testing", 1000, 5);
        BloomFilter target = FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape));
        List<BloomFilter> found = new ArrayList<BloomFilter>();
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        expected.addFoundFilters(Type.COMPLETE, target, found);
        expected.setLoad(40000);
        expected.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        expected.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream oos = new DataOutputStream(baos)) {
            serde.writeStats(oos, expected);
        }

        Stats actual = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (DataInputStream ois = new DataInputStream(bais)) {
            actual = serde.readStats(ois);
        }
        assertNotNull(actual);

        assertEquals("testing", actual.getName());
        assertEquals(5, actual.getRun());
        assertEquals(1000, actual.getPopulation());
        assertEquals(40000 * Stats.TIME_SCALE, expected.getLoad(), Stats.TIME_SCALE);
        assertEquals(40000 * Stats.TIME_SCALE, actual.getLoad(), Stats.TIME_SCALE);
        assertEquals(500, actual.getCount(Phase.Delete, Type.COMPLETE));
        assertEquals(700, actual.getCount(Phase.Query, Type.COMPLETE));
        assertEquals(60000 * Stats.TIME_SCALE, expected.getElapsed(Phase.Delete, Type.COMPLETE), Stats.TIME_SCALE);
        assertEquals(70000 * Stats.TIME_SCALE, expected.getElapsed(Phase.Query, Type.COMPLETE), Stats.TIME_SCALE);
        assertEquals(0.0, expected.getElapsed(Phase.Delete, Type.HIGHCARD), Stats.TIME_SCALE);
        assertEquals(0.0, expected.getElapsed(Phase.Query, Type.HIGHCARD), Stats.TIME_SCALE);
        assertEquals(0, actual.getCount(Phase.Query, Type.HIGHCARD));
        assertEquals(0, actual.getCount(Phase.Delete, Type.HIGHCARD));
        Map<FrozenBloomFilter, Set<FrozenBloomFilter>> m = actual.getFound(Type.COMPLETE);
        assertNotNull(m.get(target));
        Set<FrozenBloomFilter> s = m.get(target);
        assertEquals(found.size(), s.size());
        for (int i = 0; i < found.size(); i++) {
            assertTrue("Missing filter as position " + i, s.contains(found.get(i)));
        }
        assertNotNull(actual.getFound(Type.HIGHCARD));
        assertTrue(actual.getFound(Type.HIGHCARD).isEmpty());

    }

}
