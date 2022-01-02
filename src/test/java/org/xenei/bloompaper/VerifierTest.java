package org.xenei.bloompaper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.xenei.bloompaper.Stats.Phase;
import org.xenei.bloompaper.Stats.Type;
import org.xenei.bloompaper.index.FrozenBloomFilter;
import org.apache.commons.io.FileUtils;

public class VerifierTest {

    private Shape shape = new Shape(12, 12);

    private File dir;

    public VerifierTest() {
    }

    @Before
    public void setup() throws IOException {
        dir = Files.createTempDirectory("verifyTest").toFile();
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(dir);
    }

    @Test
    public void testDifferent() throws FileNotFoundException, IOException {

        BloomFilter target = FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape));
        List<BloomFilter> found = new ArrayList<BloomFilter>();
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));

        Stats one = new Stats("usage","testing", 1000, 5);
        one.addFoundFilters(Type.COMPLETE, target, found);
        one.setLoad(40000);
        one.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        one.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        Stats two = new Stats("usage","testing2", 1000, 5);
        two.addFoundFilters(Type.COMPLETE, target, found);
        two.setLoad(40000);
        two.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        two.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        Stats three = new Stats("usage","testing3", 1000, 5);
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        three.addFoundFilters(Type.COMPLETE, target, found);
        three.setLoad(40000);
        three.registerResult(Phase.Delete, Type.COMPLETE, 60000, 400);
        three.registerResult(Phase.Query, Type.COMPLETE, 70000, 800);

        Table table = new Table(dir);
        table.add("testing", Arrays.asList(one));
        table.add("testing2", Arrays.asList(two));
        table.add("testing3", Arrays.asList(three));

        Verifier verify = new Verifier(null);
        assertFalse(verify.verify(table));
    }

    @Test
    public void testSame() throws FileNotFoundException, IOException {

        BloomFilter target = FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape));
        List<BloomFilter> found = new ArrayList<BloomFilter>();
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));
        found.add(FrozenBloomFilter.makeInstance(new TestingBloomFilter(shape)));

        Stats one = new Stats("usage","testing", 1000, 5);
        one.addFoundFilters(Type.COMPLETE, target, found);
        one.setLoad(40000);
        one.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        one.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        Stats two = new Stats("usage","testing2", 1000, 5);
        two.addFoundFilters(Type.COMPLETE, target, found);
        two.setLoad(40000);
        two.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        two.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        Stats three = new Stats("usage","testing3", 1000, 5);
        three.addFoundFilters(Type.COMPLETE, target, found);
        three.setLoad(40000);
        three.registerResult(Phase.Delete, Type.COMPLETE, 60000, 500);
        three.registerResult(Phase.Query, Type.COMPLETE, 70000, 700);

        Table table = new Table(dir);
        table.add("testing", Arrays.asList(one));
        table.add("testing2", Arrays.asList(two));
        table.add("testing3", Arrays.asList(three));

        Verifier verify = new Verifier(null);
        assertTrue(verify.verify(table));
    }

}
