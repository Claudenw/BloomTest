package org.xenei.bloompaper;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.xenei.bloompaper.index.FrozenBloomFilter;

/**
 * Verifies the data from the indexes are correct.
 *
 */
public class Verifier {

    /**
     * The stream to write the results to.
     */
    private PrintStream out;

    /**
     * Constructor.
     * @param out the output stream to write the results to.
     */
    public Verifier(PrintStream out) {
        this.out = out;
    }

    /**
     * Generates a list of stats for each total returned.  There should only be one
     * total for each population, phase, and type.
     * for specific population, phase and type.
     *
     */
    private class MapGen implements Consumer<Stats> {
        /**
         * the report of values.
         */
        Map<Long, List<Stats>> report = new TreeMap<Long, List<Stats>>();
        /**
         * The population that the report is generated for.
         */
        int population;
        /**
         * The phase the report is generated for.
         */
        Stats.Phase phase;
        /**
         * The type the phase is generated for.
         */
        Stats.Type type;
        /**
         * The directory where the data files are stored.
         */
        File dir;

        /**
         * Constructor.
         * @param dir the directory where the data files are stored.
         * @param population the population to process
         * @param phase the phase to process
         * @param type the type to process.
         */
        MapGen(File dir, int population, Stats.Phase phase, Stats.Type type) {
            this.population = population;
            this.phase = phase;
            this.type = type;
            this.dir = dir;
        }

        @Override
        public void accept(Stats s) {
            if (s.getPopulation() == population) {
                try {
                    s.loadFilterMaps(dir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                long idx = s.getCount(phase, type)-s.getFalsePositives(phase,type);
                List<Stats> lst = report.get(idx);
                if (lst == null) {
                    lst = new ArrayList<Stats>();
                    report.put(idx, lst);
                }
                lst.add(s);
            }
        }

        public Map<Long, List<Stats>> getReport() {
            return report;
        }
    }

    /**
     * Returns false if any error was found.
     * @param table the table to check.
     * @return false if any error was found.
     * @throws IOException
     */
    public boolean verify(final Table table) throws IOException {
        boolean result = true;
        for (Stats.Phase phase : Stats.Phase.values()) {
            for (Stats.Type type : Stats.Type.values()) {

                for (int population : Test.POPULATIONS) {
                    MapGen mapGen = new MapGen(table.getDir(), population, phase, type);
                    table.forEachStat(mapGen);

                    Map<Long, List<Stats>> report = mapGen.getReport();

                    if (!report.isEmpty()) {
                        if (report.size() == 1) {
                            display(String.format("%s %s %s - OK", phase, type, population));
                        } else {
                            result = false;
                            err(String.format("%s %s %s - ERROR", phase, type, population));
                            for (Map.Entry<Long, List<Stats>> e : report.entrySet()) {
                                err(String.format("count = %s", e.getKey()));
                                for (Stats s : e.getValue()) {
                                    err(String.format("     %s (run %s)", s.getName(), s.getRun()));
                                }
                            }

                            for (Long x : report.keySet()) {
                                for (Long y : report.keySet()) {
                                    if (x != y) {
                                        compare(phase, type, report.get(x).get(0), report.get(y).get(0));
                                    }
                                }

                            }

                            display("");
                        }
                    } else {
                        display(String.format("No data for %s %s %s", phase, type, population));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Compare Bloom filters returned by the stats for the specific phase and type.
     * Writes output to System.out.
     * @param phase the phase to compare
     * @param type the type to compare.
     * @param statsX the first stats object.
     * @param statsY the second stats object.
     */
    private void compare(Stats.Phase phase, Stats.Type type, Stats statsX, Stats statsY) {
        Map<FrozenBloomFilter, Set<FrozenBloomFilter>> result = new HashMap<FrozenBloomFilter, Set<FrozenBloomFilter>>();
        result.putAll(statsX.getFound(type));
        for (Map.Entry<FrozenBloomFilter, Set<FrozenBloomFilter>> e : statsY.getFound(type).entrySet()) {
            Set<FrozenBloomFilter> xExtra = new HashSet<FrozenBloomFilter>(result.get(e.getKey()));
            Set<FrozenBloomFilter> yExtra = new HashSet<FrozenBloomFilter>(e.getValue());
            yExtra.removeAll(xExtra);
            xExtra.removeAll(e.getValue());
            if (!(yExtra.isEmpty() && xExtra.isEmpty())) {

                System.out.println("Difference with " + e.getKey());
                if (!yExtra.isEmpty()) {

                    System.out.println("  Only in " + statsY.displayString(phase, type));
                    for (FrozenBloomFilter bf : yExtra) {
                        System.out.println("    " + bf.toString());
                    }
                }
                if (!xExtra.isEmpty()) {

                    System.out.println("  Only in " + statsX.displayString(phase, type));
                    for (FrozenBloomFilter bf : xExtra) {
                        System.out.println("    " + bf.toString());
                    }
                }

            }
        }
    }

    /**
     * Prints an error string on output as well as on System.out
     * @param s the string to print
     */
    private void err(String s) {
        System.out.println(s);
        if (out != null) {
            out.println(s);
        }
    }

    /**
     * Prints a string on output as well as on System.out
     * @param s the string to print
     */
    private void display(String s) {
        System.out.println(s);
        if (out != null) {
            out.println(s);
        }
    }
}
