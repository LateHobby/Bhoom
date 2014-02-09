package sc.engine.engines;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.EngineStats;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;
import sc.util.ExternalUCIEngine;
import sc.util.PrintUtils;
import sc.util.SimpleStats;

public class AspWinFeaturesTest extends ComparisonTestBase {
	private static final int TTABLE_SHORT_NODE_DROP_TARGET_PERCENTAGE = 68;
	private static final int TTABLE_SHORT_TIME_DROP_TARGET_PERCENTAGE = 15;
	
	private static final int TTABLE_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 60;
	private static final int TTABLE_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = 60;

	private static final int MOVE_SORTING_SHORT_NODE_DROP_TARGET_PERCENTAGE = 95;
	private static final int MOVE_SORTING_SHORT_TIME_DROP_TARGET_PERCENTAGE = 90;
	
	private static final int MOVE_SORTING_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 95;
	private static final int MOVE_SORTING_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = 95;
	
	private static final int NULL_MOVES_SHORT_NODE_DROP_TARGET_PERCENTAGE = 25;
	private static final int NULL_MOVES_SHORT_TIME_DROP_TARGET_PERCENTAGE = 20;
	
	private static final int NULL_MOVES_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 25;
	private static final int NULL_MOVES_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = 20;
	
	private static final int KILLER_MOVES_SHORT_NODE_DROP_TARGET_PERCENTAGE = 12;
	private static final int KILLER_MOVES_SHORT_TIME_DROP_TARGET_PERCENTAGE = 3;
	
	private static final int KILLER_MOVES_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 5;
	private static final int KILLER_MOVES_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = 3;
	
	private static final int HISTORY_H_SHORT_NODE_DROP_TARGET_PERCENTAGE = 8;
	private static final int HISTORY_H_SHORT_TIME_DROP_TARGET_PERCENTAGE = -10;
	
	private static final int HISTORY_H_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 8;
	private static final int HISTORY_H_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = -5;

	private static final int FUTILITY_PRUNING_SHORT_NODE_DROP_TARGET_PERCENTAGE = 30;
	private static final int FUTILITY_PRUNING_SHORT_TIME_DROP_TARGET_PERCENTAGE = 17;
	
	private static final int FUTILITY_PRUNING_MEDIUM_NODE_DROP_TARGET_PERCENTAGE = 40;
	private static final int FUTILITY_PRUNING_MEDIUM_TIME_DROP_TARGET_PERCENTAGE = 15;
	
	
	private static final int EVAL_INCREASE_PERCENTAGE = 0;
	
	
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
			resultLog = new PrintStream(new FileOutputStream("testing/tmp/AspWinFeaturesTestResults" + timestamp + ".log"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	
	CTestEngine aspWinEngine = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
	boolean[] flagsAll = new boolean[]{true, true, true, true, true, true, true};
	
	private int TTABLE = 0;
	private int MOVESORTER = 1;
	private int NULLMOVES = 2;
	private int LMR = 3;
	private int HISTORYH = 4;
	private int KILLERMOVES = 5;
	private int FUTILITYPRUNING = 6;
	
	private int TEST_DEPTH = 8;
	
	
	public AspWinFeaturesTest() throws IOException {
	}
	
	@Test
	public void evalTableWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
		testCommon("TTable (short)", TTABLE, aspWinEngine, uci, TEST_DEPTH, suite, 
				TTABLE_SHORT_NODE_DROP_TARGET_PERCENTAGE, TTABLE_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );

	}
	
	@Test
	public void evalTableWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
		testCommon("TTable (medium)", TTABLE, aspWinEngine, uci, TEST_DEPTH, suite, 
				TTABLE_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, TTABLE_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true );
	}
	
	@Test
	public void moveSorterWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
//		testCommon("Move sorting", MOVESORTER, aspWinEngine, uci, TEST_DEPTH, suite, 0.2, 0.1, 1.0);
		testCommon("Move sorting (short)", MOVESORTER, aspWinEngine, uci, TEST_DEPTH, suite,
				MOVE_SORTING_SHORT_NODE_DROP_TARGET_PERCENTAGE, MOVE_SORTING_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );
	}
	
	@Test
	public void moveSorterWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
//		testCommon("Move sorting", MOVESORTER, aspWinEngine, uci, TEST_DEPTH, suite, 0.1, 0.1, 1.0);
		testCommon("Move sorting (medium)", MOVESORTER, aspWinEngine, uci, TEST_DEPTH, suite,
				MOVE_SORTING_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, MOVE_SORTING_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true );
	}
	
	@Test
	public void nullMovesWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
		testCommon("Null moves (short)", NULLMOVES, aspWinEngine, uci, TEST_DEPTH, suite,
				NULL_MOVES_SHORT_NODE_DROP_TARGET_PERCENTAGE, NULL_MOVES_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );
	}
	
	@Test
	public void nullMovesWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
		testCommon("Null moves (medium)", MOVESORTER, aspWinEngine, uci, TEST_DEPTH, suite,
				NULL_MOVES_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, NULL_MOVES_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true );
	}

	@Test
	public void killerMovesWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
//		testCommon("Killer moves", KILLERMOVES, aspWinEngine, uci, TEST_DEPTH, suite, 0.4, 0.6, 1.0);
		testCommon("Killer moves (short)", KILLERMOVES, aspWinEngine, uci, TEST_DEPTH, suite,
				KILLER_MOVES_SHORT_NODE_DROP_TARGET_PERCENTAGE, KILLER_MOVES_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );
	}
	
	@Test
	public void killerMovesWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
//		testCommon("Killer moves", KILLERMOVES, aspWinEngine, uci, TEST_DEPTH, suite, 0.1, 0.1, 1.0);
		testCommon("Killer moves (medium)", KILLERMOVES, aspWinEngine, uci, TEST_DEPTH, suite,
				KILLER_MOVES_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, KILLER_MOVES_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true );
	}
	
	@Test
	public void historyHeuristicWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
//		testCommon("History heuristic", HISTORYH, aspWinEngine, uci, TEST_DEPTH, suite, 0.4, 0.6, 1.0);
		testCommon("History heuristic (short)", HISTORYH, aspWinEngine, uci, TEST_DEPTH, suite,
				HISTORY_H_SHORT_NODE_DROP_TARGET_PERCENTAGE, HISTORY_H_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );
	}
	
	@Test
	public void historyHeuristicWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
		testCommon("History heuristic (medium)", HISTORYH, aspWinEngine, uci, TEST_DEPTH, suite,
				HISTORY_H_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, HISTORY_H_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true);
	}
	
	@Test
	public void futilityPruningWorksShort() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/fast.epd";
		testCommon("Futility pruning (short)", FUTILITYPRUNING, aspWinEngine, uci, TEST_DEPTH, suite,
				FUTILITY_PRUNING_SHORT_NODE_DROP_TARGET_PERCENTAGE, FUTILITY_PRUNING_SHORT_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, false );
	}
	
	@Test
	public void futilityPruningWorksMedium() throws IllegalArgumentException, IllegalAccessException, IOException {
		String suite = "junit/sc/engine/engines/Test20.EPD";
//		testCommon("Futility pruning", FUTILITYPRUNING, aspWinEngine, uci, TEST_DEPTH, suite, 0.6, 0.9, 1.0);
		testCommon("Futility pruning (medium)", FUTILITYPRUNING, aspWinEngine, uci, TEST_DEPTH, suite, 
				FUTILITY_PRUNING_MEDIUM_NODE_DROP_TARGET_PERCENTAGE, FUTILITY_PRUNING_MEDIUM_TIME_DROP_TARGET_PERCENTAGE, 
				EVAL_INCREASE_PERCENTAGE, true);
	}
	
	
	public void testCommon(String featureName, int indexToChange, CTestEngine ce, ExternalUCIEngine uci, int depth,
			String suite, int percentNodesDropTarget, int percentTimeDropTarget,
			int percentEvalIncreaseTarget, boolean checkTstat)
			throws IOException, IllegalAccessException {
		
		boolean[] withoutFeature = Arrays.copyOf(flagsAll, flagsAll.length);
		withoutFeature[indexToChange] = false;
		
		MultiStats withStat = getResultStats("With " + featureName, ce, uci, depth, suite,
				flagsAll);
		
		MultiStats withoutStat = getResultStats("Without " + featureName, ce, uci, depth, suite,
				withoutFeature);
		
		compareResults(percentNodesDropTarget, percentTimeDropTarget,
				percentEvalIncreaseTarget, withStat, withoutStat, checkTstat, resultLog);
	}

	private MultiStats getResultStats(String name, CTestEngine ce, ExternalUCIEngine uci,
			int depth, String suite, boolean[] flags) throws IOException,
			IllegalAccessException {
		setFlags(ce, flags);
		return getResultStats(name, ce, uci, depth, suite);
	}

	
	
}
