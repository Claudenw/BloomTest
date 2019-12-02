package org.xenei.bloompaper;

import java.util.ArrayList;
import java.util.List;

public class Summary {

    public class Element {
        int n;
        String type;
        int population;
        double load;
        double complete;
        double name;
        double feature;

        public Element(Stats stat) {
            this.type = stat.type;
            this.population = stat.population;
            this.n = 0;
        }


        @Override
        public String toString() {
            return String.format("'%s',%s,%s,%s,%s,%s", type, population, load / n,
                    complete / n, name / n, feature / n);
        }

        public boolean add(Stats stat) {
            if (stat.type.equals(type) && stat.population == population) {
                load += stat.load;
                complete += stat.complete;
                name += stat.name;
                feature += stat.feature;
                n++;
                return true;
            }
            return false;

        }
    }

    public static String getHeader() {
        return "'Type', 'Population', 'Avg Load Elapsed', 'Avg Complete Elapsed', 'Avg Name Elapsed', 'Avg Feature Elapsed'";
    }

    private List<Element> table = new ArrayList<Element>();

    public Summary(List<Stats> table) {
        Element el = new Element(table.get(0));
        this.table.add(el);

        for (Stats stat : table) {
            if (!el.add(stat)) {
                el = new Element(stat);
                this.table.add(el);
                el.add(stat);
            }
        }
    }

    public List<Element> getTable() {
        return table;
    }
}
