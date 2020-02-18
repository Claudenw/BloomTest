package org.xenei.bloompaper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xenei.bloom.speedTest.SortedList;

public class SortedListTest {

    @Test
    public void insertTest() {
        SortedList<Integer> lst = new SortedList<Integer>();

        lst.add( Integer.valueOf( 5 ));
        lst.add( Integer.valueOf( 4 ));
        lst.add( Integer.valueOf( 6 ));
        lst.add( Integer.valueOf( 5 ));

        assertEquals( Integer.valueOf(4), lst.get(0));
        assertEquals( Integer.valueOf(5), lst.get(1));
        assertEquals( Integer.valueOf(5), lst.get(2));
        assertEquals( Integer.valueOf(6), lst.get(3));

    }

    @Test
    public void searchTest() {
        SortedList<Integer> lst = new SortedList<Integer>();

        lst.add( Integer.valueOf( 5 ));
        lst.add( Integer.valueOf( 4 ));
        lst.add( Integer.valueOf( 6 ));
        lst.add( Integer.valueOf( 5 ));

        int pos = lst.getSearchPoint( Integer.valueOf(5));
        assertEquals( 1, pos );

        lst.add( Integer.valueOf( 1 ));
        pos = lst.getSearchPoint( Integer.valueOf(5));
        assertEquals( 2, pos );


    }
}
