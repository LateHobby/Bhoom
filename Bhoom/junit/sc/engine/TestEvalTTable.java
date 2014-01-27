package sc.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import sc.engine.EvalTTable;

public class TestEvalTTable {

	@Test
	public void test1() {
		EvalTTable tt = new EvalTTable(16);
		Random r = new Random();
		long zkey1 = r.nextLong();
		long zkey2 = r.nextLong();
		
		assertFalse(tt.hasPosition(zkey1));
		tt.storePosition(zkey1, 1, -10, 10, 2, 10, true);
		assertTrue(tt.hasPosition(zkey1));
		assertFalse(tt.hasPosition(zkey2));
		
//		assertEquals(EvalTTable.NO_IEVAL, tt.getEval(zkey1, ));
//		assertEquals(EvalTTable.NO_IEVAL, tt.getEval(1L, -20, 5, 2));
//		assertEquals(EvalTTable.NO_IEVAL, tt.getEval(1L, -8, 8, 3));
//		assertEquals(1, tt.getEval(1L, -16, 16, 2));
	}

}
