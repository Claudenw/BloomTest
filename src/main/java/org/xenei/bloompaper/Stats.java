package org.xenei.bloompaper;

public class Stats {
    String type;
    int population;
    long load;
    long complete;
    long completeFound;
    long name;
    long nameFound;
    long feature;
    long featureFound;

    public Stats( int population)
    {
        this.population = population;
    }

    public static String getHeader() {
        return "'Type', 'Population', 'Load Elapsed', 'Complete Elapsed', 'Complete Found', 'Name Elapsed', 'Name Found', 'Feature Elapsed', 'Feature Found'";
    }
    @Override
    public String toString() {
        return String.format( "'%s',%s,%s,%s,%s,%s,%s,%s,%s", type, population, load, complete,completeFound, name,nameFound, feature,featureFound);
    }
}
