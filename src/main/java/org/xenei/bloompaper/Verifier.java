package org.xenei.bloompaper;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.xenei.bloompaper.index.FrozenBloomFilter;

public class Verifier {

    private PrintStream o;

    public Verifier(PrintStream o) {
        this.o = o;
    }

    public void verify(final List<Stats> table) {

        for (Stats.Phase phase : Stats.Phase.values()) {
            for (Stats.Type type : Stats.Type.values()) {

                for (int population : Test.POPULATIONS) {
                    Map<Long, List<Stats>> report = table.stream().filter(s -> s.getPopulation() == population)
                            .collect(Collectors.groupingBy(s -> s.getCount(phase, type)));

                    if (report.size() == 1) {
                        display(String.format("%s %s %s - OK", phase, type, population));
                    } else {

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
                }
            }
        }
    }

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

    private void err(String s) {
        System.out.println(s);
        if (o != null) {
            o.println(s);
        }
    }

    private void display(String s) {
        System.out.println(s);
        if (o != null) {
            o.println(s);
        }
    }
}
