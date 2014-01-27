package sc.util;



public class LongHashTable<E extends ListNode> {

	protected ObjectPool<E> pool;
	protected E[] table;
	protected int TABLE_SIZE;
	protected long TABLE_MASK;
	protected int NUM_BITS;
	
	private int numItemsStored;
	private int maxChainLength;
	private int numSlotsUsed;
	
	private int maxChainAllowed;
	
	public LongHashTable(ObjectPool<E> pool, int numBits, int maxChain) {
		this.pool = pool;
		this.maxChainAllowed = maxChain;
		NUM_BITS = numBits;
		TABLE_SIZE = 1 << NUM_BITS;
		TABLE_MASK = BitManipulation.long_msb_masks[NUM_BITS];
		table = pool.factory.getArray(TABLE_SIZE);
	}
	
	 @SuppressWarnings("unchecked")
	public void store(long key, E val) {
		val.key = key;
		int index = (int) (key & TABLE_MASK);
		if (table[index] == null) {
			table[index] = val;
			val.next = null;
			numItemsStored++;
			numSlotsUsed++;
			if (maxChainLength == 0) {
				maxChainLength = 1;
			}
		} else {
			E node = table[index];
			E previous = null;
			int count = 0;
			while (node != null) {
				if (node.key == key) { // replace and raise to front
					break;
				}
				count++;
				previous = node;
				node = (E) node.next;
			}
			if (node != null) {  // key found; replace
				if (previous != null) {
					previous.next = node.next;
					val.next = table[index];
				} else { // node is the first element
					val.next = node.next;
				}
				table[index] = val;
				node.next = null;
				pool.release(node);
			} else { // key not found; insert in front
				val.next = table[index];
				table[index] = val;
				numItemsStored++;
				if (count+1 > maxChainLength) {
					if (count +1 > maxChainAllowed) { // remove an item from the back
						removeLastElement(table[index]);
					} else {
						maxChainLength = count+1;
					}
				}
			}
		}
			
	}

	private void removeLastElement(E node) {
		E previous = null;
		while (node.next != null) {
			previous = node;
			node = (E) node.next;
		}
		previous.next = null;
		pool.release(node);
	}
	
	@SuppressWarnings("unchecked")
	public E get(long key) {
		int index = (int) (key & TABLE_MASK);
		if (table[index] == null) {
			return null;
		} else {
			E node = table[index];
			while (node != null) {
				if (node.key == key) {
					return node;
				}
				node = (E) node.next;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public void promote(long key, E toPromote) {
		int index = (int) (key & TABLE_MASK);
		E node = table[index];
		if (node != toPromote) {
			while (node.next != toPromote) {
				node = (E) node.next;
			}
			// node.next is toPromote
			node.next = toPromote.next;
			toPromote.next = table[index];
			table[index] = toPromote;
		}
	}
	
	public void printStats() {
		System.out.println("LongHashTable: Entries=" + numItemsStored + " Max chain=" + maxChainLength + " Num slots=" + numSlotsUsed + " (" + (numSlotsUsed * 100)/ TABLE_SIZE + "%)");
	}
	

}
