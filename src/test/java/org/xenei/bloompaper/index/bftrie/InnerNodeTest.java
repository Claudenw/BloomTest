package org.xenei.bloompaper.index.bftrie;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.collections4.bloomfilter.Shape;
import org.junit.Test;
import org.xenei.bloompaper.index.BitUtils;

public class InnerNodeTest {

    private long getMask(int idx) {
        return BitUtils.getLongBit(idx - 1);
    }

    @Test
    public void nibbleTest() {

        long zero = 0;

        long one = getMask(1 + 4);

        long two = getMask(2 + 8);

        long three = getMask(1 + 12) | getMask(2 + 12);

        long four = getMask(3 + 16);

        long five = getMask(1 + 20) | getMask(3 + 20);

        long six = getMask(2 + 24) | getMask(3 + 24);

        long seven = getMask(1 + 28) | getMask(2 + 28) | getMask(3 + 28);

        long eight = getMask(4 + 32);

        long nine = getMask(1 + 36) | getMask(4 + 36);

        long a = getMask(2 + 40) | getMask(4 + 40);

        long b = getMask(1 + 44) | getMask(2 + 44) | getMask(4 + 44);

        long c = getMask(3 + 48) | getMask(4 + 48);

        long d = getMask(1 + 52) | getMask(3 + 52) | getMask(4 + 52);

        long e = getMask(2 + 56) | getMask(3 + 56) | getMask(4 + 56);

        long f = getMask(1 + 60) | getMask(2 + 60) | getMask(3 + 60) | getMask(4 + 60);

        long value = zero | one | two | three | four | five | six | seven | eight | nine | a | b | c | d | e | f;

        BFTrie trie = mock(BFTrie.class);
        when(trie.getWidth()).thenReturn(4);

        Shape shape = Shape.fromKM(302, 17);
        InnerNode innerNode = new InnerNode(0, shape, trie);

        for (int i = 0; i < 16; i++) {
            assertEquals("Wrong value from first long ", i, innerNode.getChunk(new long[] { value, value }, i));
            assertEquals("Wrong value from second long", i,
                    innerNode.getChunk(new long[] { value, value }, i + (Long.SIZE / 4)));
        }
    }
}
