package sc.util;

public class ObjStack<E> {

	protected Object[] arr;
	protected int pos = 0;
	
	public ObjStack(int maxLength) {
		arr = new Object[maxLength];
	}
	
	public void push(E val) {
		arr[pos++] = val;
	}
	
	public E pop() {
		return (E) arr[--pos];
	}

	public void clear() {
		pos = 0;
		
	}
}
