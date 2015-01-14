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
	
	static final String[] LABELS = { "complete", "high cardinality", "low cardinality" };
	
	public Stats( int density, int limit)
	{
		this.density = density;
		this.limit = limit;
	}
	
	public static String header()
	{
		return "'filter','density','N','load','complete','complete found','low cardinality','low cardinality found','high cardinality','high cardinality found'";
	}
	
	public long value(int pos)
	{
		switch (pos) {
			case 1:
				return  complete;
			case 2:
				return highCard;
			case 3:
				return lowCard;
			default:
				throw new IllegalArgumentException(String.format(
						"%s is not a valid position", pos));
		}	
	}
	
	public long count(int pos)
	{
		switch (pos) {
			case 1:
				return  completeFound;
			case 2:
				return hcFound;
			case 3:
				return lcFound;
			default:
				throw new IllegalArgumentException(String.format(
						"%s is not a valid position", pos));
		}	
	}
	public String toString() {
		return String.format( "'%s',%s,%s,%s,%s,%s,%s,%s,%s,%s", type, density, limit, load, complete,completeFound, highCard,hcFound, lowCard,lcFound);
	}
	
	public void reportLoad(int run ) {
		System.out.println(String.format(
				"%s density=%s N=%s run %s load time %s", type, density, limit,
				run, load));
	}
	
	public void reportCount(int pos, int run) {
		System.out.println(String.format(
			"%s density %s N=%s run %s %s count time %s (%s)", type, density,
			limit, run, LABELS[pos-1], value(pos), count(pos)));
	}
	
	public void registerResult(final int pos,
			final long total, final long found) {
		switch (pos) {
			case 1:
				complete = total;
				completeFound = found;
				break;
			case 2:
				highCard = total;
				hcFound = found;
				break;
			case 3:
				lowCard = total;
				lcFound = found;
				break;
			default:
				throw new IllegalArgumentException(String.format(
						"%s is not a valid position", pos));
		}
	}
}
