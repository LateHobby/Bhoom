package sc.util;

public class IntStack {

	protected int[] arr;
	protected int pos = 0;
	
	public IntStack(int maxLength) {
		arr = new int[maxLength];
	}
	
	public void push(int val) {
		arr[pos++] = val;
	}
	
	public int pop() {
		return arr[--pos];
	}

	public void clear() {
		pos = 0;
	}
	
	public int size() {
		return pos;
	}
	
	public int peek(int offset) {
		if (offset > 0) {
			throw new RuntimeException("peek() called with positive argument");
		}
		return arr[pos-1-offset];
	}
}
