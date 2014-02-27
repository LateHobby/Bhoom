package sc.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class BinaryMaxHeapTest {

	@Test
	public void testPerformance1() {
		int reps = 100;
		int size = 100;
		BinaryMaxHeap<Integer> bm = new BinaryMaxHeap<Integer>(new Integer[size]);
		List<Integer> list = new ArrayList<Integer>(size);
		Random r = new Random();

		for (int i = 0; i < size; i++) {
			list.add(r.nextInt(5 * size));
		}

		long start = System.currentTimeMillis();
		for (int k = 0; k < reps; k++) {
			for (int i = 0; i < size; i++) {
				bm.insert(list.get(i));
			}
			bm.insertsDone();
			int v;
			for (int i = 0; i < size; i++) {
				v = bm.extractMax();
			}
			bm.reset();
		}
		long tbm = System.currentTimeMillis() - start;
		
		List<Integer> sl = new ArrayList<Integer>(size);
		start = System.currentTimeMillis();
		for (int k = 0; k < reps; k++) {
			for (int i = 0; i < size; i++) {
				sl.add(list.get(i));
			}
			Collections.sort(sl);
//			Collections.reverse(sl);
		}
		long tsl = System.currentTimeMillis() - start;
		assertTrue(String.format("tbm=%d  tsl=%d", tbm, tsl), tbm < 2 * tsl/3);
		

	}
}
