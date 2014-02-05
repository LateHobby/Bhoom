package sc.engine.engines;

import static org.junit.Assert.assertEquals;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.engine.Evaluator;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.testing.TestingUtils;
import sc.testing.TestingUtils.EPDTest;
import sc.util.BoardUtils;
import sc.util.PrintUtils;
import sc.visualdebug.SearchTreeBuilder;
import sc.visualdebug.SearchTreePanel;

public class CorrectnessTests {

	PrintWriter pw;
	public CorrectnessTests() {
		File log = new File("testing/tmp/correctness.log");
		try {
			pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log))));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
//	@Test
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

	@Test
	public void testEngineSettings() {
		List<EPDTest> tests;
		try {
//			tests = TestingUtils.getTestsFromFile("testing/suites/Test20.EPD");
			tests = TestingUtils.getTestsFromFile("junit/sc/engine/engines/fast.epd");
//			tests = TestingUtils.getTestsFromFile("testing/suites/Test100.EPD");
			
			checkAgainstSimpleAlphaBeta(4, tests);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void checkAgainstSimpleAlphaBeta(int depth, List<EPDTest> tests) {
		
		Evaluator eval = new SideToMoveEvaluator();
		CTestEngine aspWinEngine = new CTestEngine("AspWin", SearchMode.ASP_WIN, eval, new AlwaysReplace(), new SeeHashSorter());
		CTestEngine mtdEngine = new CTestEngine("MTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new SeeHashSorter());
		CTestEngine mtdBinaryEngine = new CTestEngine("BinaryMTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new SeeHashSorter());
		CTestEngine mtdHybridEngine = new CTestEngine("HybridMTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new SeeHashSorter());
		CTestEngine[] ea = new CTestEngine[] {aspWinEngine, mtdEngine, mtdBinaryEngine, mtdHybridEngine};
		for (CTestEngine ce : ea) {
			checkAgainstSimpleAlphaBeta(ce, depth, tests);
		}
		
	}
	
	private void checkAgainstSimpleAlphaBeta(CTestEngine ce, int depth, List<EPDTest> tests) {
		boolean[] flags = new boolean[]{false, false, false, false, false, false, false};
		EBitBoard board = new EBitBoard();
		for (int i = -1; i < flags.length; i++) {
			if (i >= 0) {
				flags[i] = true;
			}
			ce.setFlags(flags[0], flags[1], flags[2], false, flags[4], flags[5], flags[6]);
			for (EPDTest test: tests) {
				BoardUtils.initializeBoard(board, test.fen);
				Continuation c = ce.simpleAlphaBeta(board, depth, 0);
				if (!checkAgainstReference(ce, depth, test.fen, c, false)) {
					boolean internalSearchFailed = false;
					if (ce.mode == SearchMode.ASP_WIN) {
						internalSearchFailed = checkAspWinSearch(ce, depth, test.fen, c, false);
					} else {
						internalSearchFailed = checkMtdSearch(ce, depth, test.fen, c, false);
					}
					System.out.println(ce.mode + " failed on " + test.fen );
					System.out.println(Arrays.toString(flags));
					if (internalSearchFailed) {
						System.out.println("Internal search also failed!");
					}
					pw.println(ce.mode + " failed on " + test.fen );
					pw.println(Arrays.toString(flags));
					pw.flush();
//					visualCheck(ce, depth, test.fen);
//					return;
//					fail();
				}
			}
		}
	}
	
//	private void compareAlphaBetaToSimpleAlphaBeta(String fen) {
//		CTestEngine ce = new CTestEngine("test", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
//		ce.setFlags(false, false, false, false, false, false, false);
//		int depth = 4;
//		checkAgainstSimpleAlphaBeta(ce, depth, fen);
//	}

	public boolean checkAgainstReference(CTestEngine ce, int depth,
			String fen, Continuation reference, boolean failOnAssertion) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		
		Continuation c = ce.searchByDepth(bb, depth);
		
		System.out.println(fen);
		System.out.printf("%s  Found: %s[%d] Expected: %s[%d]\n", 
				ce.name(),
				PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(reference.line[0]), reference.eval);
		// Make sure that the engine returns the same as the reference.
		if (failOnAssertion) {
			assertEquals(reference.eval, c.eval);
			assertEquals(reference.line[0], c.line[0]);
		}  else {
			return (reference.eval == c.eval);
		}
		return true;
	}

	// Test the basic search routine
	private boolean checkAspWinSearch(CTestEngine ce, int depth, String fen, Continuation reference, boolean failOnAssertion) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		// Check that the basic search works
		Continuation c = ce.aspWinSearch(bb, depth, Evaluator.MIN_EVAL);
		if (failOnAssertion) {
			assertEquals(reference.eval, c.eval);
			assertEquals(reference.line[0], c.line[0]);
		} 
		boolean basicSearchWorks = (reference.eval == c.eval /*&& reference.line[0] == c.line[0]*/);
		if (!basicSearchWorks) {
			System.out.printf("Basic search failed: Found: %s[%d] Expected: %s[%d]\n",
					PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(reference.line[0]), reference.eval);
			return false;
		} else {
			// Check that the aspiration window works
			c = ce.aspWinSearch(bb, depth, reference.eval);
			if (failOnAssertion) {
				assertEquals(reference.eval, c.eval);
				assertEquals(reference.line[0], c.line[0]);
			} 
			boolean aspWinSearchWorks = (reference.eval == c.eval /*&& reference.line[0] == c.line[0]*/);
			if (!aspWinSearchWorks) {
				System.out.printf("Asp win search failed: Found: %s[%d] Expected: %s[%d]\n",
						PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(reference.line[0]), reference.eval);
				return false;
			}
		}
		return true;
	}
	
	// Test the basic search routine
	private boolean checkMtdSearch(CTestEngine ce, int depth, String fen, Continuation reference, boolean failOnAssertion) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		// Check that the basic search works
		Continuation c = ce.mtdSearch(bb, depth, Evaluator.MIN_EVAL, ce.mode);
		try {
			assertEquals(reference.eval, c.eval);
			assertEquals(reference.line[0], c.line[0]);
		} catch (Throwable t) {
			if (failOnAssertion) {
				throw t;
			} else {
				return false;
			}
		}
		return true;
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

	
	private void nonVisualCheck(CTestEngine ce, int depth, String fen) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		Continuation c = ce.simpleAlphaBeta(bb, depth, 0);
		
		Continuation c2 = ce.aspWinSearch(bb, depth, Evaluator.MIN_EVAL);
		
		System.out.println(fen);
		System.out.printf("Found: %s[%d] Expected: %s[%d]\n", 
				PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(c2.line[0]), c2.eval);
//		assertEquals(c2.eval, c.eval);
	}
	
	private void visualCheck(CTestEngine ce, int depth, String fen) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		ce.listen = true;
		SearchTreeBuilder stb = new SearchTreeBuilder();
		stb.fen = fen;
		ce.setListener(stb);
		Continuation reference = ce.simpleAlphaBeta(bb, depth, 0);
		display(stb, "SimpleAlphaBeta");
		
		SearchTreeBuilder stb2 = new SearchTreeBuilder();
		stb2.fen = fen;
		ce.setListener(stb2);
		Continuation c2 = ce.aspWinSearch(bb, depth, Evaluator.MIN_EVAL);
		display(stb2, "CEngine");
		
		System.out.println(fen);
		System.out.printf("Found: %s[%d] Expected: %s[%d]\n", 
				PrintUtils.notation(c2.line[0]), c2.eval,
				PrintUtils.notation(reference.line[0]),reference.eval);
//		assertEquals(c2.eval, c.eval);
	}

	private void display(SearchTreeBuilder stb, String title) {
		SearchTreePanel stp = new SearchTreePanel();
		stp.setBuilder(stb);
		JFrame fr = new JFrame(title);
		fr.setLayout(new BorderLayout());
		fr.add(new JScrollPane(stp), BorderLayout.CENTER);
		fr.add(stp.boardEvalPanel, BorderLayout.EAST);
		stp.evalPanel.setEvaluator(new SideToMoveEvaluator());
		fr.setVisible(true);
		
	}
	
	
	public static void main(String[] args) throws IOException {
		CorrectnessTests ct = new CorrectnessTests();

//		ct.testEngineSettings();

//		ct.testAgainstMinimaxVisual("8/k1b5/P4p2/1Pp2p1p/K1P2P1P/8/3B4/8 w - - 0 0");
		
//		CTestEngine ce = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
//		boolean[] flags = new boolean[]{true, true, true, false, true, true, true};
//		ce.setFlags(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
//		String fen = "6k1/2p5/2n1p2p/4P1p1/r4nP1/1p1pKP2/1B6/3N4 b - - 0 39";
////		ct.visualCheck(ce, 4, fen);
//		ct.nonVisualCheck(ce, 4, fen);
		
		EBitBoard board = new EBitBoard();
		CTestEngine ce = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		boolean[] flags = new boolean[]{true, true, true, false, true, true, true};
		List<EPDTest> tests = TestingUtils.getTestsFromFile("testing/suites/Test100.EPD");
		for (EPDTest test: tests) {
			BoardUtils.initializeBoard(board, test.fen);
			ce.search(board, 7, 0, 0, 0, 0, 0);
		}
		// bad positions from game (try with depth 4)
		//r1bq1rk1/1p1n1p1p/1n4p1/2p5/1Q2P2N/p1P3P1/P4PBP/1RBR2K1 w - - 0 19
		//r1bqkb1r/4pppp/2pp1n2/p7/3QP3/2N4P/PPP2PP1/R1B1KB1R w KQkq - 0 11
		//r1bqk2r/pp1n1pp1/2pbp2p/8/2pP2n1/2N1PN2/PPQB1P1P/1R2KB1R w Kkq - 0 10
	}
}
