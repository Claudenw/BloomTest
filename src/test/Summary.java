package test;

import java.util.ArrayList;
import java.util.List;

public class Summary {

	public class Element {
		int n;
		String type;
		int limit;
		int density;
		double load;
		double complete;
		double highCard;
		double lowCard;

		public Element(Stats stat) {
			this.density = stat.density;
			this.type = stat.type;
			this.limit = stat.limit;
			this.n = 0;
		}
		
		public String toString() {
			return String.format("'%s',%s,%s,%s,%s,%s,%s",  type, density, limit, load / n,
					complete / n, highCard / n, lowCard / n);
		}

		public boolean add(Stats stat) {
			if (stat.type.equals(type) && stat.limit == limit) {
				load += stat.load;
				complete += stat.complete;
				highCard += stat.highCard;
				lowCard += stat.lowCard;
				n++;
				return true;
			}
			return false;

		}
	}

	private List<Element> table = new ArrayList<Element>();
	
	public static String header() {return "'name','density','N','load','complete','high cardinality','low cardinality'";}

	
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
