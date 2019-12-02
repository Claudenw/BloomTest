package org.xenei.bloompaper.hamming;

public class DoubleLong  {
    public static final int BYTES = 16;
    public static final int WIDTH = 16*8;
    protected ByteInfo[] val;
    private int hammingWeight;
    private int hash;
    private long[] parts;

    protected DoubleLong( long a, long b )
    {
        val = new ByteInfo[BYTES];
        int idx = 0;
        parts = new long[2];
        parts[0] = a;
        parts[1] = b;
        hash = Long.valueOf(a).hashCode()^Long.valueOf(b).hashCode();
        hammingWeight = 0;
        for (int i=0;i<2;i++)
        {
            int x = 0;
            for (int j=0;j<64;j+=8)
            {
                x = (int) (0xFF & (parts[i] >> j));
                ByteInfo bi = ByteInfo.BYTE_INFO[(byte) x];
                hammingWeight += bi.getHammingWeight();
                val[idx++]= bi;
            }
        }

    }

    public int length() {
        return BYTES;
    }

    public ByteInfo getByte( int i)
    {
        return val[i];
    }

    public NibbleInfo getNibble( int i )
    {
        return (val[ i/2]).getNibbles()[ i % 2];
    }

    public int getHammingWeight()
    {
        return hammingWeight;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof DoubleLong)
        {
            DoubleLong dl = (DoubleLong)o;
            return parts[0] == dl.parts[0] && parts[1] == dl.parts[1];
        }
        return false;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<BYTES;i++)
        {
            sb.append( val[i].getHexPattern());
        }
        return sb.toString();
    }

}
