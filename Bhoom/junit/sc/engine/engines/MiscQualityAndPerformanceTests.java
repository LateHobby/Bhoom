package sc.engine.engines;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import sc.engine.EngineBoard;
import sc.engine.engines.CTestEngine;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.engine.ttables.ReplaceIfDeeperAlternate;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.CSV;
import sc.util.ExternalUCIEngine;

public class MiscQualityAndPerformanceTests extends ComparisonTestBase {

	static PrintStream resultLog;
	static ExternalUCIEngine uci;
	static {
		try {
			uci = new ExternalUCIEngine(
					"C:\\Program Files (x86)\\BabasChess\\Engines\\toga\\togaII.exe");
			uci.startup();
			uci.send("setoption name MultiPV value 1");
			uci.send("setoption name Ponder value false");
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			resultLog = new PrintStream(new FileOutputStream("testing/tmp/MiscQualityAndPerformanceTestsResults" + timestamp + ".log"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	boolean[] flagsAll = new boolean[]{true, true, true, true, true, true, true};
	
//	@Test
	public void testMvvLvaVsSeeHash20() throws IllegalAccessException, IOException {
		String suite = suite20;
		
		
		CTestEngine mvvLvaEngine = new CTestEngine("MvvLva", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mvvLvaEngine, flagsAll);
		
		CTestEngine seeHashEngine = new CTestEngine("SeeHash", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
		setFlags(seeHashEngine, flagsAll);
		
		ComparisonStats cstat = compareEngines(mvvLvaEngine, seeHashEngine, 7,
				uci, 20, 80, 0, suite, true, resultLog);
		
		assertTrue(cstat.newStats.stats("nodes").mean() < cstat.newStats.stats("externalNodes").mean());
	}
	
//	@Test
	public void testAlwaysReplaceVsReplaceIfDeeperAlternate20() throws IllegalAccessException, IOException {
		String suite = suite20;
		
		CTestEngine mvvLvaEngine = new CTestEngine("AlwaysReplace", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mvvLvaEngine, flagsAll);
		
		CTestEngine seeHashEngine = new CTestEngine("ReplaceIfDeeperAlternate", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new ReplaceIfDeeperAlternate(), new SeeHashSorter());
		setFlags(seeHashEngine, flagsAll);
		
		ComparisonStats cstat = compareEngines(mvvLvaEngine, seeHashEngine, 7,
				uci, 15, 80, 0, suite, true, resultLog);
	}
	
	@Test
	public void testBadEvalIsCaughtFast() throws IllegalAccessException, IOException {
		String suite = fastSuite;
		class BadEval extends SideToMoveEvaluator {

			@Override
			public int evaluate(EngineBoard board) {
				// Flip the evaluation
				return -super.evaluate(board);
			}
			
		}
		
		CTestEngine badEval = new CTestEngine("BadEval", SearchMode.ASP_WIN, new BadEval(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(badEval, flagsAll);
		
		CTestEngine goodEval = new CTestEngine("goodEval", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(goodEval, flagsAll);
		
		
		ComparisonStats cstat = compareEngines(goodEval, badEval, 7,
				uci, -100, -100, 50, suite, false, resultLog);
	}
	
}
