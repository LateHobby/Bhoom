package sc.util;

public class SimpleStats {

	double[] arr = new double[1000];
	int pos = 0;
	double sum = 0;
	double min = Double.POSITIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;
	
	public void include(double d) {
		arr[pos++] = d;
		sum += d;
		if (d < min) {
			min = d;
		}
		if (d > max) {
			max = d;
		}
	}
	
	public double mean() {
		return sum/pos;
	}
	
	public double var() {
		double avg = mean();
		double sum = 0;
		for (int i = 0; i < pos; i++) {
			double d = arr[i] - avg;
			sum += (d * d);
		}
		return sum/(pos-1);
	}
	
	public double sd() {
		return Math.sqrt(var());
	}
	
	public double min() {
		return min;
	}
	
	public double max() {
		return max;
	}
	
	public double tstat() {
		double sd = sd();
		double se = sd/Math.sqrt(pos);
		double avg = mean();
		return avg/se;
	}
	
	public SimpleStats difference(SimpleStats other) {
		if (other.pos != pos) {
			throw new RuntimeException("Unequal point count");
		}
		SimpleStats s = new SimpleStats();
		for (int i = 0; i < pos; i++) {
			s.include(arr[i] - other.arr[i]);
		}
		return s;
	}
}
