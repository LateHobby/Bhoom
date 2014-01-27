package sc.engine.ttables;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import sc.util.TTable.TTEntry;

public class TestAlwaysReplace {
	
	@Test
	public void testCorrectness() {
		AbstractEvalTT ar = new AlwaysReplace(8);
		long key = 1L;
		TTEntry entry = new TTEntry();
		ar.storeToTT(key, entry);
		TTEntry returnValue = new TTEntry();
		assertTrue(ar.retrieveFromTT(key, returnValue ));
		assertFalse(ar.retrieveFromTT(2, returnValue ));
	}
	
	@Test
	public void testCorrectness2() {
		AbstractEvalTT ar = new AlwaysReplace(8);
		Random r = new Random();
		int size = 10000;
		long[] keys = new long[size];
		TTEntry[] values = new TTEntry[size];
		for (int i = 0; i < size; i++) {
			keys[i] = r.nextLong();
			values[i] = new TTEntry();
			// need non-negative values
			values[i].eval = r.nextInt(Short.MAX_VALUE);
			values[i].move = r.nextInt(Short.MAX_VALUE);
			ar.storeToTT(keys[i], values[i]);
		}
		TTEntry buffer = new TTEntry();
		for (int i = 0; i < size; i++) {
			if (ar.retrieveFromTT(keys[i], buffer)) {
				assertEquals(values[i].eval, buffer.eval);
				assertEquals(values[i].move, buffer.move);
			}
		}
	}

	
}
