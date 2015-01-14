package test;

import java.util.ArrayList;
import java.util.List;

public class Summary {

	public class Element {
		int n;
		String type;
		int limit;
		double load;
		double complete;
		double name;
		double feature;

		public Element(Stats stat) {
			this.type = stat.type;
			this.limit = stat.limit;
			this.n = 0;
		}

		public String toString() {
			return String.format("'%s',%s,%s,%s,%s,%s", type, limit, load / n,
					complete / n, name / n, feature / n);
		}

		public boolean add(Stats stat) {
			if (stat.type.equals(type) && stat.limit == limit) {
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
