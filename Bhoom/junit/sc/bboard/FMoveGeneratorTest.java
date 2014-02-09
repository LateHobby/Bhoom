package sc.bboard;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import sc.util.BoardUtils;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;

public class FMoveGeneratorTest {

	private EBitBoard board = new EBitBoard();
	private EMoveGenerator egen = new EMoveGenerator(board);
	private FMoveGenerator fgen = new FMoveGenerator(board);
	
	@Test
	public void testFAgainstE() throws IOException {
		List<EPDTest> tests = EPDTestingUtils.getTestsFromFile("testing/suites/ECM.EPD");
		for (EPDTest test: tests) {
			testAllMoves(test.fen);
			testCaptures(test.fen);
		}
	}
	
	private void testAllMoves(String fen) {
		BoardUtils.initializeBoard(board, fen);
		int[] earr = new int[128];
		int[] farr = new int[128];
		int numMoves = egen.fillLegalMoves(earr, 0);
		assertTrue(numMoves > 0);
		assertEquals(numMoves, fgen.fillLegalMoves(farr, 0));
		Arrays.sort(earr);
		Arrays.sort(farr);
		assertTrue(Arrays.equals(earr, farr));
	}
	
	private void testCaptures(String fen) {
		BoardUtils.initializeBoard(board, fen);
		int[] earr = new int[128];
		int[] farr = new int[128];
		int numMoves = egen.fillLegalCaptures(earr, 0);
		assertEquals(numMoves, fgen.fillLegalCaptures(farr, 0));
		Arrays.sort(earr);
		Arrays.sort(farr);
		assertTrue(Arrays.equals(earr, farr));
	}
}
