package org.xenei.bloompaper.hamming;

import java.util.Comparator;


public class ByteInfoCompare implements Comparator<ByteInfo>{

    @Override
    public int compare(ByteInfo arg0, ByteInfo arg1) {
        int i = Integer.compare(arg0.getHammingWeight(), arg1.getHammingWeight());
        if (i == 0)
        {
            return Integer.compare( arg0.getVal(), arg1.getVal());
        }
        return i;
    }

}
