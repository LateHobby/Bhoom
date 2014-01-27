package sc.testing;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.EngineStats;
import sc.engine.SearchEngine;
import sc.testing.TestingUtils.EPDTest;
import sc.testing.TestingUtils.EngineSetting;
import sc.testing.TestingUtils.TestResult;
import sc.util.BoardUtils;
import sc.util.ObjectPool.Factory;
import sc.util.PrintUtils;
import sc.util.SimpleStats;

public class CompareAgainstExternal {

	private ExternalUCIEngine uci;
	private Factory<SearchEngine> ef;
	private EngineSetting setting;

	NumberFormat df = DecimalFormat.getNumberInstance();
	NumberFormat iff = DecimalFormat.getIntegerInstance();

	public CompareAgainstExternal(Factory<SearchEngine> ef, EngineSetting setting, ExternalUCIEngine uci) throws IOException {
		this.ef = ef;
		this.setting = setting;
		this.uci = uci;
		uci.startup();
	}
	
	public void runSuite(File suiteFile, PrintStream stream) throws Exception {
		EngineBoard board = new EBitBoard();
		List<EPDTest> tests = TestingUtils.getTestsFromFile(suiteFile);
		List<ScoredResult> sr = new ArrayList<ScoredResult>();
		int index = 0;
		for (EPDTest test : tests) {
			TestResult result = TestingUtils.getTestResult(ef, setting, board, test);
			BoardUtils.initializeBoard(board, test.fen);
			uci.getBestMove(board, setting.depth, setting.timeMs);
			int uciEval = uci.getEval();
			int uciMove = uci.getMove();
			
			int move = result.moves[0];
			board.makeMove(move, false);
			uci.evaluatePosition(board, setting.depth, setting.timeMs);
			int uciMoveEval = uci.getEval();
			ScoredResult sres = new ScoredResult(result, uciMoveEval - uciEval, move == uciMove);
			sr.add(sres);
			printIntermediateResults(stream, index, sr);
			index++;
		}
	}
	
	private void printIntermediateResults(PrintStream stream, int index,
			List<ScoredResult> sr) {
		if (index > 9 && index % 10 == 0) {
			printResults(stream, sr);
		}
		
	}

	private void printResults(PrintStream stream, List<ScoredResult> sr) {

		String[] heading = new String[] { "Variable", "Mean", "SD", "Min", "Max", "Tstat"};
		String[] params = new String[] { "Score", "Nodes", "Depth", "Match" };
		SimpleStats[] sa = new SimpleStats[params.length];
		
		for (int i = 0; i < sa.length; i++) {
			sa[i] = new SimpleStats();
		}
		
		
		
		int count = 0;
		for (ScoredResult sres : sr) {
			count++;
			EngineStats olds = sres.result.stats;
			
			sa[0].include(sres.score);
			sa[1].include( olds.getNodes(false) + olds.getNodes(true) );
			sa[2].include(olds.getDepth());
			sa[3].include(sres.match ? 1 : 0);
		}
		
		
		String[][] twoDim = new String[sa.length + 1][];
		twoDim[0] = heading;
		
		for (int i = 1; i < twoDim.length; i++) {
			String[] row = new String[heading.length];
			row[0] = params[i-1];
			row[1] = format(sa[i-1].mean());
			row[2] = format(sa[i-1].sd());
			row[3] = format(sa[i-1].min());
			row[4] = format(sa[i-1].max());
			row[5] = format(sa[i-1].tstat());
			twoDim[i] = row;
		}
		PrintUtils.printFormattedMatrix(stream, twoDim, 2);

		
	}

	private String format(double d) {
		df.setMaximumFractionDigits(2);
		if (Math.abs(d) > 100000) {
			return iff.format(d);
		} else {
			return df.format(d);
		}
	}

	private class ScoredResult {
		public ScoredResult(TestResult result, int score, boolean match) {
			this.result = result;
			this.score = score;
			this.match = match;
		}
		TestResult result;
		int score;
		boolean match;
	}
	
	
	
	
}
