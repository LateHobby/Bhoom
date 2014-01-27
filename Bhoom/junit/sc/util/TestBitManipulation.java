package sc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sc.util.BitManipulation;

public class TestBitManipulation {

	@Test
	public void testIsSet() {
		long zero = 0L;
		long ones = ~zero;
		for (int i = 0; i < 64; i++) {
			assertTrue("ones bit " + i, BitManipulation.isSet(i, ones));
			assertFalse("zeros bit " + i, BitManipulation.isSet(i, zero));
		}
	}
	
	@Test
	public void testSet() {
		long zero = 0L;
		for (int i = 0; i < 64; i++) {
			long val = BitManipulation.set(i, zero);
			assertTrue("bit " + i, BitManipulation.isSet(i, val));
		}
	}

	@Test
	public void testClear() {
		long zero = 0L;
		long ones = ~zero;
		for (int i = 0; i < 64; i++) {
			long val = BitManipulation.clear(i, ones);
			assertFalse("bit " + i, BitManipulation.isSet(i, val));
		}
	}
	
	@Test
	public void testAllLongs() {
		int[] bits = new int[]{0, 1, 2};
		long[] longs = BitManipulation.allLongsForBits(bits);
		
		assertEquals(8, longs.length);
		
		for (long l = 0; l < 8; l++) {
			assertEquals(l, longs[(int)l]);
		}
	}
}
