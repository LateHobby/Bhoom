package sc.util;

public class ObjectPool<E extends Poolable> {

	
	public static interface Factory<E> {
		E create();
		
		E[] getArray(int size);
	}
	
	Factory<E> factory;
	
	private E[] arr;
	int pos;

	private String name;
	
	public ObjectPool(Factory<E> factory, int capacity, String name) {
		this.name = name;
		this.factory = factory;
		arr = factory.getArray(capacity);
		for (int i = 0; i < capacity; i++) {
			arr[i] = factory.create();
		}
		pos = capacity;
	}
	
	public E allocate() {
		if (pos == 0) {
			reallocate();
		}
		E e = arr[--pos];
		return e;
	}
	
	private void reallocate() {
		System.out.println(name + " Reallocating:" + arr.length);
		E[] newArr = factory.getArray(2 * arr.length);
		for (int i = 0; i < arr.length; i++) {
			newArr[i] = factory.create();
		}
		pos = arr.length;
		arr = newArr;
	}

	public void release(E obj) {
		obj.reset();
		arr[pos++] = obj;
	}

	public void printStats() {
		System.out.println(name + ": Available=" + pos);
		
	}
	
}
