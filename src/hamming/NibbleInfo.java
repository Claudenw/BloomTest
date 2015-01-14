package hamming;


public class NibbleInfo {
	
	public static NibbleInfo[] NIBBLE_INFO = { 
		new NibbleInfo( 0,  "0000", "0", 0 ),
		new NibbleInfo( 1,  "0001", "1", 1 ),		
		new NibbleInfo( 2,  "0010", "2", 1 ),
		new NibbleInfo( 3,  "0011", "3", 2 ),
		new NibbleInfo( 4,  "0100", "4", 1 ),
		new NibbleInfo( 5,  "0101", "5", 2 ),
		new NibbleInfo( 6,  "0110", "6", 2 ),
		new NibbleInfo( 7,  "0111", "7", 3 ),
		new NibbleInfo( 8,  "1000", "8", 1 ),
		new NibbleInfo( 9,  "1001", "9", 2 ),
		new NibbleInfo( 10, "1010", "A", 2 ),
		new NibbleInfo( 11, "1011", "B", 3 ),
		new NibbleInfo( 12, "1100", "C", 2 ),
		new NibbleInfo( 13, "1101", "D", 3 ),
		new NibbleInfo( 14, "1110", "E", 3 ),
		new NibbleInfo( 15, "1111", "F", 4 ) };


	private int val;
	private String pattern;
	private String hexPattern;
	private int hammingWeight;
	
	private NibbleInfo( int val, String pattern, String hexPattern, int size )
	{
		this.val = val;
		this.pattern = pattern;
		this.hexPattern = hexPattern;
		this.hammingWeight = size;
	}

	public String getPattern() {
		return pattern;
	}

	public String getHexPattern() {
		return hexPattern;
	}
	public int getHammingWeight() {
		return hammingWeight;
	}
	
	public int getVal() {
		return val;
	}
	
	public String toString()
	{
		return hexPattern;
	}
}


