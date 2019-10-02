package org.xenei.bloompaper;

public class Stats {
	String type;
	int limit;
	long load;
	long complete;
	long completeFound;
	long name;
	long nameFound;
	long feature;
	long featureFound;

	public Stats( int limit)
	{
		this.limit = limit;
	}

	public static String getHeader() {
	    return "type, limit, load, complete, completeFound, name, nameFound, feature, featureFound";
	}
	@Override
    public String toString() {
		return String.format( "'%s',%s,%s,%s,%s,%s,%s,%s,%s", type, limit, load, complete,completeFound, name,nameFound, feature,featureFound);
	}
}
