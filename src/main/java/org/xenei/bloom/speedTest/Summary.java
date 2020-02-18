package org.xenei.bloom.speedTest;

import java.util.ArrayList;
import java.util.List;

public class Summary {

	public class Element {
		int n;
		String type;
		int population;
		double load;
	    double intChk;
	    double longChk;
	    double filterChk;
	    double hasherChk;

		public Element(Stats stat) {
			this.type = stat.type;
			this.population = stat.population;
			this.n = 0;
		}


		@Override
        public String toString() {
			return String.format("'%s',%s,%s,%s,%s,%s,%s", type, population, load/n,
					intChk/n, longChk/n, hasherChk/n, filterChk/n);
		}

		public boolean add(Stats stat) {
			if (stat.type.equals(type) && stat.population == population) {
				load += stat.load;
				intChk += stat.intChk;
				longChk += stat.longChk;
                hasherChk += stat.hasherChk;
				filterChk += stat.filterChk;
				n++;
				return true;
			}
			return false;

		}
	}

	public static String getHeader() {
        return "'Type', 'Population', 'Avg Load Elapsed', 'Avg Int Elapsed', 'Avg Long Elapsed', 'Avg Hasher Elapsed', 'Avg Filter Elapsed'";
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
