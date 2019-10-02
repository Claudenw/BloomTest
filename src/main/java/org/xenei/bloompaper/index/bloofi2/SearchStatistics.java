package org.xenei.bloompaper.index.bloofi2;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author adina
 */
public class SearchStatistics {

    /** Number of BloomFilters checked for matches */
    public int nbBFChecks;

    public SearchStatistics() {
        nbBFChecks = 0;
    }

    /**
     * Reset the statistics to 0
     */
    public void clear() {
        nbBFChecks = 0;
    }
}

