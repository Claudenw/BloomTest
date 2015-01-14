package test;

public class Stats {
	String type;
	int density;
	int limit;
	long load;
	long complete;
	long completeFound;
	long highCard;
	long hcFound;
	long lowCard;
	long lcFound;
	
	public Stats( int density, int limit)
	{
		this.density = density;
		this.limit = limit;
	}
	
	public static String header()
	{
		return "'filter','density','N','load','complete','complete found','low cardinality','low cardinality found','high cardinality','high cardinality found'";
	}
	
	public String toString() {
		return String.format( "'%s',%s,%s,%s,%s,%s,%s,%s,%s,%s", type, density, limit, load, complete,completeFound, highCard,hcFound, lowCard,lcFound);
	}
}
