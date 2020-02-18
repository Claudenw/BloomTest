package org.xenei.bloom.speedTest;

public class Stats {
	String type;
	int population;
	long load;
	long intChk;
	long longChk;
	long filterChk;
	long hasherChk;

	public Stats( int population)
	{
		this.population = population;
	}

	public static String getHeader() {
	    return "'Type', 'Population', 'Load Elapsed', 'Int Elapsed', 'Long Elapsed', 'Hasher Elapsed', 'Filter Elapsed'";
	}
	@Override
    public String toString() {
		return String.format( "'%s',%s,%s,%s,%s,%s,%s", type, population, load, intChk, longChk, hasherChk, filterChk);
	}
}
