package org.xenei.bloompaper.hamming;

import java.util.Comparator;

public class DoubleLongCompare implements Comparator<DoubleLong> {

    @Override
    public int compare(DoubleLong o1, DoubleLong o2) {
        ByteInfoCompare bic = new ByteInfoCompare();
        int retval = Integer.compare(o1.getHammingWeight(), o2.getHammingWeight());
        if (retval == 0)
        {
            for (int i =0;i<DoubleLong.BYTES;i++)
            {
                retval = bic.compare(o1.getByte(i), o2.getByte(i) );
                if (retval != 0)
                {
                    return retval;
                }
            }
        }
        return retval;
    }




}
