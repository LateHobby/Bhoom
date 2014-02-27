package sc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import sc.util.TTable.ProbeResult;

public class TestLPTTable {

	ProbeResult pr = new ProbeResult();
	@Test
	public void test1() {
		int numBits = 20;
		LPTTable lpt = new LPTTable(numBits, 2);
		int size = 10000;
		long[] keys = new long[size];
		long[] values = new long[size];
		Random r = new Random();
		for (int i = 0; i < size; i++) {
			keys[i] = r.nextLong();
			values[i] = r.nextLong();
		}
		for (int i = 0; i < size; i++) {
			lpt.store(keys[i], values[i]);
		}
		int index = 0;
		for (int i = 0; i < size; i++) {
			if (lpt.contains(keys[i], pr)) {
				assertEquals(values[i], lpt.get(keys[i]));
				index++;
			}
		}
		assertTrue("Index=" + index, index > Math.min(size, lpt.TABLE_SIZE/2)/2);
		lpt.printOccupancy();
	}
	
	@Test
	public void test2() {
		long key = -1946699405907152930L;
		LPTTable table = new LPTTable(20, 2);
		
		table.store(key, -100);
		if (table.contains(key, pr)) {
			assertEquals(-100, table.get(key));
		}
	}
}
