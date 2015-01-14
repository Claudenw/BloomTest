package hamming;


public class ByteInfo {
		private byte val;
		private int hammingWeight;
		
		public static ByteInfo[] BYTE_INFO = new ByteInfo[256];
		
		static {
			for (int i=0;i<256;i++)
			{
				BYTE_INFO[i] = new ByteInfo((byte)i);
			}	
		}
		
		private ByteInfo( byte val)
		{
			this.val = val;
			int x = 0x0F & (val >> 4);
			hammingWeight = NibbleInfo.NIBBLE_INFO[x].getHammingWeight();
			x = 0x0F & val;
			hammingWeight += NibbleInfo.NIBBLE_INFO[x].getHammingWeight();
		}

		public String getPattern() {
			int x = 0x0F & (val >> 4);
			StringBuilder retval = new StringBuilder(NibbleInfo.NIBBLE_INFO[x].getPattern());
			x = 0x0F & val;
			retval.append( NibbleInfo.NIBBLE_INFO[x].getPattern() );
			return retval.toString();
		}
		
		public NibbleInfo[] getNibbles() {
			int x = 0x0F & (val >> 4);
			NibbleInfo ni1 = NibbleInfo.NIBBLE_INFO[x];
			x = 0x0F & val;
			NibbleInfo ni2 = NibbleInfo.NIBBLE_INFO[x];
			return new NibbleInfo[] { ni1, ni2 };
		}

		public String getHexPattern() {
			int x = 0x0F & (val >> 4);
			StringBuilder retval = new StringBuilder(NibbleInfo.NIBBLE_INFO[x].getHexPattern());
			x = 0x0F & val;
			retval.append( NibbleInfo.NIBBLE_INFO[x].getHexPattern() );
			return retval.toString();
		}
		public int getHammingWeight() {
			return hammingWeight;
		}
		
		public int getVal() {
			return 0xFF & val;
		}

		@Override
		public int hashCode() {
			return val;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof ByteInfo)
			{
				return val == ((ByteInfo)o).val;
			}
			return false;
		}
		
		@Override
		public String toString()
		{
			return getHexPattern();
		}		
	
}
