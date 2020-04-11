package org.xenei.bloompaper;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.xenei.bloompaper.index.BitUtils;
import org.xenei.bloompaper.index.NumericBloomFilter;

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
//
//                        for (Long x : report.keySet()) {
//                            for (Long y : report.keySet()) {
//                                compare(phase, type, report.get(x).get(0), report.get(y).get(0));
//                            }
//
//                        }

                        display("");
                    }
                }
            }
        }
    }

    private void compare(Stats.Phase phase, Stats.Type type, Stats statsX, Stats statsY) {
        Map<NumericBloomFilter, Set<NumericBloomFilter>> result = new HashMap<NumericBloomFilter, Set<NumericBloomFilter>>();
        result.putAll(statsX.getFound( type));
        for (Map.Entry<NumericBloomFilter, Set<NumericBloomFilter>> e : statsY.getFound(type).entrySet()) {
            Set<NumericBloomFilter> xExtra = new HashSet<NumericBloomFilter>(result.get(e.getKey()));
            Set<NumericBloomFilter> yExtra = new HashSet<NumericBloomFilter>(e.getValue());
            yExtra.removeAll(xExtra);
            xExtra.removeAll(e.getValue());
            if (!(yExtra.isEmpty() && xExtra.isEmpty())) {

                System.out.println("Error in " + BitUtils.format(e.getKey().getBits()));
                if (!yExtra.isEmpty()) {

                    System.out.println("Extra " + statsY.displayString(phase,type));
                    for (BloomFilter bf : yExtra) {
                        System.out.println(BitUtils.format(bf.getBits()));
                    }
                }
                    if (!xExtra.isEmpty()) {

                        System.out.println("Extra " + statsX.displayString(phase,type));
                        for (BloomFilter bf : yExtra) {
                            System.out.println(BitUtils.format(bf.getBits()));
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
