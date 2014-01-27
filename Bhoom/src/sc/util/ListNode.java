package sc.util;

public class ListNode implements Poolable {

	public long key;
	public ListNode next;

	@Override
	public void reset() {
		if (next != null) {
			next.reset();
		}
		
	}
	
}
