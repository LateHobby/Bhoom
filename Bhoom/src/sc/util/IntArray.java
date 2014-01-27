package sc.util;

public class IntArray extends IntStack {

	public IntArray(int maxLength) {
		super(maxLength);
	}

	public int get(int index) {
		return arr[index];
	}
	
	public void set(int index, int value) {
		arr[index] = value;
	}
	
	public int size() {
		return pos;
	}
}
