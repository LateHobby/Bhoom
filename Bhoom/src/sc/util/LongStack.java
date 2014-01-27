package sc.util;

public class LongStack {

	protected long[] arr;
	protected int pos = 0;
	
	public LongStack(int maxLength) {
		arr = new long[maxLength];
	}
	
	public void push(long val) {
		arr[pos++] = val;
	}
	
	public long pop() {
		return arr[--pos];
	}

	// special method required for draw detection
	public int getCount(long val) {
		int count = 0;
		for (int i = 0; i < pos; i++) {
			if (arr[i] == val) {
				count++;
			}
		}
		return count;
	}
	public void clear() {
		pos = 0;
		
	}
}
