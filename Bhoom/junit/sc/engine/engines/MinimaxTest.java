package sc.engine.engines;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import sc.SlowTests;
import sc.bboard.EBitBoard;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.PrintUtils;

public class MinimaxTest {

	@Test
	@Category(SlowTests.class)
	public void simpleAlphaBetaAndMinimax() {
		// This test is to be run only if you suspect that simpleAlphaBeta is not
		// returning the right result. Running the minimax search takes time.
//	    Identical evaluation
		compareSimpleAlphaBetaToMinimax("3r2k1/1p5p/6p1/p2q1p2/P1Q5/1P5P/1P6/5RK1 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("R7/P4k2/8/8/8/8/r7/6K1 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("8/6pp/3q1p2/3n1k2/1P6/3NQ2P/5PP1/6K1 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("2r5/1r6/4pNpk/3pP1qp/8/2P1QP2/5PK1/R7 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("8/k1b5/P4p2/1Pp2p1p/K1P2P1P/8/3B4/8 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("1k6/5RP1/1P6/1K6/6r1/8/8/8 w - - 0 0");
		compareSimpleAlphaBetaToMinimax("6k1/5ppp/1q6/2b5/8/2R1pPP1/1P2Q2P/7K w - - 0 0");
		compareSimpleAlphaBetaToMinimax("8/p7/1ppk1n2/5ppp/P1PP4/2P1K1P1/5N1P/8 b - - 0 0");
		compareSimpleAlphaBetaToMinimax("8/p3k1p1/4r3/2ppNpp1/PP1P4/2P3KP/5P2/8 b - - 0 0");
		compareSimpleAlphaBetaToMinimax("8/k7/p7/3Qp2P/n1P5/3KP3/1q6/8 b - - 0 0");
		
// Long incomplete tests below this line		
//		testAgainstMinimax("r6k/p1Q4p/2p1b1rq/4p3/B3P3/4P3/PPP3P1/4RRK1 b - - 0 0");
//		testAgainstMinimax("2k5/pppr4/4R3/4Q3/2pp2q1/8/PPP2PPP/6K1 w - - 0 0");
//		testAgainstMinimax("r2k4/1pp2rpp/pn1b1p2/3n4/8/P4NB1/1PP3PP/2KRR3 w - - 0 0");
//		testAgainstMinimax("5r1k/4R3/p1pp4/3p1bQ1/3q1P2/7P/P2B2P1/7K w - - 0 0");
	}

	
	private void compareSimpleAlphaBetaToMinimax(String fen) {
		CTestEngine ce = new CTestEngine("test", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
		ce.setFlags(false, false, false, false, false, false, false);
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		int depth = 4;
		Continuation c = ce.simpleAlphaBeta(bb, depth, 0);
		Continuation c2 = ce.minimaxSearch(bb, depth, 0);
		
		System.out.println(fen);
		System.out.printf("simpleAlphaBeta: %s[%d] minimax: %s[%d]\n", 
				PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(c2.line[0]), c2.eval);
		// Make sure that simpleAlphaBeta returns the same as minimax, which is the reference.
		assertEquals(c2.eval, c.eval);
		assertEquals(c2.line[0], c.line[0]);
	}

}
