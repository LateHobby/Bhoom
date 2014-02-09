package sc.engine.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import sc.IntermediateTests;
import sc.SlowTests;
import sc.bboard.EBitBoard;
import sc.engine.Evaluator;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;
import sc.util.PrintUtils;

public class SimpleAlphaBetaTest {

	String debugFen = null;
	PrintWriter pw;
	File log = new File("testing/tmp/SimpleAlphaBetaTestFailures.log");
	boolean[] flags = new boolean[]{true, true, true, false, true, true, false};
	
	// Fails
//	boolean[] flags = new boolean[]{false, false, false, false, false,false, true};
//	boolean[] flags = new boolean[]{false, true, false, false, true, true, true};
	
	// Passes
//	boolean[] flags = new boolean[]{false, false, false, false, false,false, false};
//	boolean[] flags = new boolean[]{false, true, false, false, true, true, false};
	
	public SimpleAlphaBetaTest() {
		try {
			pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void aspWinVsSimpleAlphaBetaFast() {
		aspWinVsSimpleAlphaBeta(4, "junit/sc/engine/engines/fast.epd");
	}
	
	@Test 
	public void mtdVsAspWinFast() {
		CTestEngine mtdEngine = new CTestEngine("MTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdEngine, 4, "junit/sc/engine/engines/fast.epd");
	}
	@Test 
	public void mtdBinVsAspWinFast() {
		CTestEngine mtdBinaryEngine = new CTestEngine("BinaryMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdBinaryEngine, 4, "junit/sc/engine/engines/fast.epd");
	}
	@Test 
	public void mtdHybridVsAspWinFast() {
		CTestEngine mtdHybridEngine = new CTestEngine("HybridMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdHybridEngine, 4, "junit/sc/engine/engines/fast.epd");
	}
	
	
	
	@Test
	@Category({SlowTests.class, IntermediateTests.class})
	public void aspWinVsSimpleAlphaBetaIntermediate() {
		aspWinVsSimpleAlphaBeta(4, "junit/sc/engine/engines/Test100.EPD");
	}
	
	@Test 
	@Category({SlowTests.class, IntermediateTests.class})
	public void mtdVsAspWinIntermediate() {
		CTestEngine mtdEngine = new CTestEngine("MTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdEngine, 4, "junit/sc/engine/engines/Test100.EPD");
	}
	@Test 
	@Category({SlowTests.class, IntermediateTests.class})
	public void mtdBinVsAspWinIntermediate() {
		CTestEngine mtdBinaryEngine = new CTestEngine("BinaryMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdBinaryEngine, 4, "junit/sc/engine/engines/Test100.EPD");
	}
	@Test 
	@Category({SlowTests.class, IntermediateTests.class})
	public void mtdHybridVsAspWinIntermediate() {
		CTestEngine mtdHybridEngine = new CTestEngine("HybridMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdHybridEngine, 4, "junit/sc/engine/engines/Test100.EPD");
	}
	
	
	@Test
	@Category({SlowTests.class})
	public void aspWinVsSimpleAlphaBetaSlow() {
		aspWinVsSimpleAlphaBeta(4, "junit/sc/engine/engines/ECM.EPD");
	}
	
	@Test 
	@Category({SlowTests.class})
	public void mtdVsAspWinSlow() {
		CTestEngine mtdEngine = new CTestEngine("MTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdEngine, 4, "junit/sc/engine/engines/ECM.EPD");
	}
	@Test 
	@Category({SlowTests.class})
	public void mtdBinVsAspWinSlow() {
		CTestEngine mtdBinaryEngine = new CTestEngine("BinaryMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdBinaryEngine, 4, "junit/sc/engine/engines/ECM.EPD");
	}
	@Test 
	@Category({SlowTests.class})
	public void mtdHybridVsAspWinSlow() {
		CTestEngine mtdHybridEngine = new CTestEngine("HybridMTDF", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		engineVsAspWin(mtdHybridEngine, 4, "junit/sc/engine/engines/ECM.EPD");
	}
	
	private void aspWinVsSimpleAlphaBeta(int depth, String testFile) {
		
		List<EPDTest> tests;
		try {
			tests = EPDTestingUtils.getTestsFromFile(testFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Evaluator eval = new SideToMoveEvaluator();
		CTestEngine aspWinEngine = new CTestEngine("AspWin", SearchMode.ASP_WIN, eval, new AlwaysReplace(), new MvvLvaHashSorter());
		checkAgainstSimpleAlphaBeta(aspWinEngine, depth, tests);
		
		CTestEngine mtdEngine = new CTestEngine("MTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new MvvLvaHashSorter());
		CTestEngine mtdBinaryEngine = new CTestEngine("BinaryMTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new MvvLvaHashSorter());
		CTestEngine mtdHybridEngine = new CTestEngine("HybridMTDF", SearchMode.MTDF, eval, new AlwaysReplace(), new MvvLvaHashSorter());
		CTestEngine[] ea = new CTestEngine[] {mtdEngine, mtdBinaryEngine, mtdHybridEngine};
		for (CTestEngine ce : ea) {
//			checkAgainstReferenceEngine(aspWinEngine, ce, depth, tests);
		}
		
	}
	
	private void engineVsAspWin(CTestEngine ce, int depth, String testFile) {
		
		List<EPDTest> tests;
		try {
			tests = EPDTestingUtils.getTestsFromFile(testFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Evaluator eval = new SideToMoveEvaluator();
		CTestEngine aspWinEngine = new CTestEngine("AspWin", SearchMode.ASP_WIN, eval, new AlwaysReplace(), new MvvLvaHashSorter());
		aspWinEngine.setFlags(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
		
		checkAgainstReferenceEngine(aspWinEngine, ce, depth, tests);
		
	}
	private void checkAgainstSimpleAlphaBeta(CTestEngine ce, int depth, List<EPDTest> tests) {
		EBitBoard board = new EBitBoard();
		

		ce.setFlags(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
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
				System.out.println("Failures written to " + log);
				System.out.println("Run TestDebugger to debug...");
				if (debugFen == null) {
					fail();	
				}
			}
		}
	}

	 
	private boolean checkAgainstReferenceEngine(CTestEngine reference, CTestEngine ce, int depth, List<EPDTest> tests) {
		EBitBoard board = new EBitBoard();

		ce.setFlags(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
		for (EPDTest test: tests) {
			BoardUtils.initializeBoard(board, test.fen);
			if (test.fen.equals(debugFen)) {
				TestDebugger tdb = new TestDebugger();
				tdb.visualCheck(reference, ce, depth, debugFen);
				return false;
			}
			Continuation c = reference.searchByDepth(board, depth);
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
				System.out.println("Failures written to " + log);
				System.out.println("Run TestDebugger to debug...");
				if (debugFen == null) {
					fail();
				}
			}
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

	public static void main(String[] args) {
		SimpleAlphaBetaTest t = new SimpleAlphaBetaTest();
		t.debugFen = "3r2k1/1p5p/6p1/p2q1p2/P1Q5/1P5P/1P6/5RK1 w - - 0 0";
		t.mtdVsAspWinFast();
	}
}
