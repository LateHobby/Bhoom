package sc.engine.engines;

import java.util.ArrayList;
import java.util.List;

public class EngineParameters {

	List<Parameter> params = new ArrayList<Parameter>();
	
	public void addParameter(String name, int lowerBound, int upperBound) {
		Parameter p = new Parameter(name, lowerBound, upperBound);
		params.add(p);
	}
	
	
	public String[] getParameterNames() {
		String[] sa = new String[params.size()];
		for (int i = 0; i < sa.length; i++) {
			sa[i] = params.get(i).name;
		}
		return sa;
	}
	
	public int getNumParameters() {
		return params.size();
	}
	
	public double[][] getParameterBounds() {
		double[][] pb = new double[params.size()][2];
		for (int i = 0; i < params.size(); i++) {
			pb[i][0] = params.get(i).lower;
			pb[i][1] = params.get(i).upper;
		}
		return pb;
	}
	
	private class Parameter {
		String name;
		int lower;
		int upper;
		public Parameter(String name, int lower, int upper) {
			super();
			this.name = name;
			this.lower = lower;
			this.upper = upper;
		}
		
	}
}
