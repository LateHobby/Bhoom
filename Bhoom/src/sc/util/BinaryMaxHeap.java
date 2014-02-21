package sc.util;

import java.util.*;

/**
 * This implementation of a Binary Heap is specifically for move sorting. In move
 * sorting, there are no deletes and all the inserts happen in the beginning.
 * The objective is to achieve constant time per insert() and logarithmic time
 * per extractMax(). Inserts are done by just adding to the array. After all the inserts are done,
 * insertsDone() should be called, after which extractMax() will always return the largest element.
 *
 * Created by Shiva on 2/21/14.
 */
public  class BinaryMaxHeap<T extends Comparable<T>> {

    private T[] array;
    private int pos;
    private boolean isHeap;

    public BinaryMaxHeap(T[] array) {
        this.array = array;
        this.pos = 0;
    }

    /**
     * Adds an element to the heap. O(1) time.
     * @param t
     */
    public void insert(T t) {
        array[pos] = t;
        pos++;
        isHeap = false;
    }

    /**
     * Should be called after all the inserts are done.
     */
    public void insertsDone() {
        buildMaxHeap(array, pos);
        isHeap = true;
    }

    public T extractMax() {
        if (!isHeap) {
            throw new RuntimeException("Heap is not built yet");
        }
        if (pos == 0) {
            return null;
        }
        pos--;
        T rv = array[0];
        array[0] = array[pos];
        if (pos > 1) {
            maxHeapify(array, 0, pos);
        }
        return rv;

    }
    private  void buildMaxHeap(T[] arr, int length)
    {
        for( int i = length/2; i >= 0; i-- )
            maxHeapify(arr, i, length);
    }
    private  void maxHeapify(T[] arr, int node, int length)
    {
        int left = 2 * node + 1;
        int right = 2 * node + 2;
        int largest = node;

        if( left < length && arr[ left ].compareTo(arr[ largest ]) > 0 )
            largest = left;
        if( right < length && arr[ right ].compareTo(arr[ largest ]) > 0 )
            largest = right;
        if( largest != node )
        {
            T temp = arr[ node ];
            arr[ node ] = arr[ largest ];
            arr[ largest ] = temp;
            maxHeapify(arr, largest, length);
        }
    }


    public static void main(String[] args) {
        int size = 200;
        BinaryMaxHeap<Integer> bm = new BinaryMaxHeap<Integer>(new Integer[size]);
        List<Integer> list = new ArrayList<Integer>();
        Random r = new Random();
        for (int i = 0; i < size; i++) {
            int ri = r.nextInt();
            bm.insert(ri);
            list.add(ri);
        }
        List<Integer> l2 = new ArrayList<Integer>();
        bm.insertsDone();
        for (int i = 0; i < size; i++) {
            l2.add(bm.extractMax());
        }
        if (!(bm.extractMax() == null)) throw new AssertionError();

        Collections.sort(list);
        Collections.reverse(list);
        if (!Arrays.deepEquals(list.toArray(), l2.toArray())) throw new AssertionError("Lists not equal");
        System.out.println("Done");
    }
}
