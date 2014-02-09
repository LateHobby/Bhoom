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

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.EngineStats;
import sc.util.BoardUtils;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;
import sc.util.CSV;
import sc.util.ExternalUCIEngine;
import sc.util.PrintUtils;
import sc.util.SimpleStats;

public class ComparisonTestBase {

	EngineBoard board = new EBitBoard();
	
	
	String fastSuite = "junit/sc/engine/engines/fast.epd";
	
	String suite20 = "junit/sc/engine/engines/Test20.EPD";
	
	String suite100 = "junit/sc/engine/engines/Test100.EPD";
	
	public void setFlags(CTestEngine ce, boolean[] f) {
		ce.setFlags(f[0], f[1], f[2], f[3], f[4], f[5], f[6]);
		
	}
	
	public ComparisonStats compareEngines(CTestEngine hopeFullyImproved, 
			CTestEngine original, int depth, ExternalUCIEngine uci, 
			int percentNodesDropTarget, int percentTimeDropTarget,
			int percentEvalIncreaseTarget, String suite, 
			boolean checkTStat,
			PrintStream resultLog) throws IllegalAccessException, IOException {
		
		String suiteName = "(" + suite.substring(suite.lastIndexOf('/')+1) + ")";
		MultiStats improvedStat = getResultStats(hopeFullyImproved.name() + suiteName, hopeFullyImproved, uci, depth, suite);
		
		MultiStats originalStat = getResultStats(original.name() + suiteName, original, uci, depth, suite);
		
		MultiStats diffStat = compareResults(percentNodesDropTarget, percentTimeDropTarget,
				percentEvalIncreaseTarget, improvedStat, originalStat, checkTStat, resultLog);
		return new ComparisonStats(originalStat, improvedStat, diffStat);
	}
	
	
	public MultiStats getResultStats(String name, CTestEngine ce,
			ExternalUCIEngine uci, int depth, String suite) throws IOException,
			IllegalAccessException {
		List<Result> resultTTable = getResults(ce, uci, depth, suite);
		MultiStats statsTTable = new MultiStats(name, Result.class);
		statsTTable.updateStats(resultTTable);
		return statsTTable;
	}

	public MultiStats compareResults(int percentNodesDropTarget,
			int percentTimeDropTarget, int percentEvalIncreaseTarget,
			MultiStats hopefullyImproved, MultiStats original,
			boolean checkTstat,
			PrintStream resultLog) {
		MultiStats diff = original.difference(hopefullyImproved);
		
		
		
		printAnalysis(percentNodesDropTarget, percentTimeDropTarget,
				percentEvalIncreaseTarget, hopefullyImproved, original, diff, resultLog);
		if (resultLog != System.out) {
			printAnalysis(percentNodesDropTarget, percentTimeDropTarget,
					percentEvalIncreaseTarget, hopefullyImproved, original, diff, System.out);
		}
		
		decreasedAtLeast(percentNodesDropTarget, hopefullyImproved, original, "nodes");
		decreasedAtLeast(percentTimeDropTarget, hopefullyImproved, original, "timeMs");
		increasedAtLeast(percentEvalIncreaseTarget, hopefullyImproved, original, "externalEval");
		if (checkTstat) {
			assertTrue("Nodes tstat too low", diff.stats("nodes").tstat() > 2);
		}
		return diff;
	}

	public void printAnalysis(int percentNodesDropTarget,
			int percentTimeDropTarget, int percentEvalIncreaseTarget, MultiStats hopefullyImproved,
			MultiStats original, MultiStats diff, PrintStream resultLog) {
		diff.printFormatted(resultLog);
		hopefullyImproved.printFormatted(resultLog);
		original.printFormatted(resultLog);
		
		int actualPercentNodeDrop = (int) ( 100 - (100.0 * hopefullyImproved.stats("nodes").mean())/original.stats("nodes").mean());
		int actualPercentTimeDrop = (int) ( 100 - (100.0 * hopefullyImproved.stats("timeMs").mean())/original.stats("timeMs").mean());
		double actualEvalIncrease = hopefullyImproved.stats("externalEval").mean() - original.stats("externalEval").mean();
		int actualPercentEvalIncrease = (int) (100.0 * actualEvalIncrease/Math.abs(original.stats("externalEval").mean()));
		resultLog.printf("Node drop: %d%% (Target:%d%%)  Time drop: %d%% (Target:%d%%) Eval increase: %d%% (Target:%d%%)\n", 
				actualPercentNodeDrop, percentNodesDropTarget, actualPercentTimeDrop, percentTimeDropTarget,
				actualPercentEvalIncrease, percentEvalIncreaseTarget);
		resultLog.println("--------------------------------------------------------");
		resultLog.println();
		
		resultLog.flush();
	}


	public void decreasedAtLeast(int percentDropTarget, MultiStats newS, MultiStats oldS, String name) {
		double newValue = newS.stats(name).mean();
		double oldValue = oldS.stats(name).mean();
		int actualPercentDrop = (int) (100 - (100.0 * newValue)/oldValue);
		String reason = String.format("%s decrease (%d%%) less than target (%d%%)", name, actualPercentDrop, percentDropTarget);
		assertTrue(reason, percentDropTarget <= actualPercentDrop);
	}
	
	public void increasedAtLeast(int percentRiseTarget, MultiStats newS, MultiStats oldS, String name) {
		double newValue = newS.stats(name).mean();
		double oldValue = oldS.stats(name).mean();
		double actualRise = newValue - oldValue;
		int actualPercentRise = (int) (100.0 * actualRise/Math.abs(oldValue));
		String reason = String.format("%s increase (%d%%) less than target (%d%%)", name, actualPercentRise, percentRiseTarget);
		assertTrue(reason, percentRiseTarget <= actualPercentRise);
	}
	
	
	
	public List<Result> getResults(CTestEngine ce, ExternalUCIEngine uci,
			int depth, String suite) throws IOException {
		List<Result> results = new ArrayList<Result>();
		List<EPDTest> tests = EPDTestingUtils.getTestsFromFile(suite);
		for (EPDTest test : tests) {
			Result r = getOneResult(ce, uci, depth, test.fen);
			results.add(r);
			
		}
		
		return results;
	}


	private Result getOneResult(CTestEngine ce, ExternalUCIEngine uci,
			int depth, String fen) {
		Result r = new Result();
		ce.newGame();
		BoardUtils.initializeBoard(board, fen);
		long start = System.currentTimeMillis();
		int move = ce.searchByDepth(board, depth).line[0];
		long timeMs = System.currentTimeMillis() - start;
		EngineStats es = ce.getEngineStats();
		r.nodes = es.getNodes(false) + es.getNodes(true);
		r.timeMs = (int) timeMs;
		r.externalEval = uci.evaluateMove(board, move, depth, 0);
		uci.evaluatePosition(board, depth, 0);
		r.externalNodes = uci.getNodes();
		return r;
	}


	public class ComparisonStats {
		public MultiStats oldStats;
		public MultiStats newStats;
		public MultiStats diffStats;
		public ComparisonStats(MultiStats oldStats, MultiStats newStats,
				MultiStats diffStats) {
			super();
			this.oldStats = oldStats;
			this.newStats = newStats;
			this.diffStats = diffStats;
		}
		
	}

	
	public class MultiStats {
		String name;
		String[] varNames;
		SimpleStats[] stats;
		
		public MultiStats(String name, Class<?> clz) {
			List<String> names = new ArrayList<String>();
			Field[] fields = clz.getDeclaredFields();
			for (Field f : fields) {
				if (Modifier.isPublic(f.getModifiers())) {
					if (f.getType().isAssignableFrom(int.class)) {
						names.add(f.getName());
					}
				}
			}
			String[] namesArr = names.toArray(new String[0]);
			initialize(namesArr);
			setName(name);
		}
		
		public MultiStats(String...names) {
			initialize(names);
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public <T> void updateStats(List<T> results) throws IllegalArgumentException, IllegalAccessException {
			if (results.isEmpty()) {
				return;
			}
			Class<T> clz = (Class<T>) results.get(0).getClass();
			Field[] fields = clz.getDeclaredFields();
			for (T result : results) {
				for (Field f : fields) {
					if (Modifier.isPublic(f.getModifiers())) {
						if (f.getType().isAssignableFrom(int.class)) {
							SimpleStats ss = stats(f.getName());
							if (ss != null) {
								ss.include(f.getDouble(result));
							}
						}
					}
				}
			}
		}
		
		
		public SimpleStats stats(String name) {
			for (int i = 0; i < varNames.length; i++) {
				if (varNames[i].equalsIgnoreCase(name)) {
					return stats[i];
				}
			}
			return null;
		}
		
		// Return this - other
		public MultiStats difference(MultiStats other) {
			if (!Arrays.equals(varNames, other.varNames)) {
				throw new RuntimeException("Varnames don't match");
			}
			MultiStats ms = new MultiStats(varNames);
			ms.setName(name + " - " + other.name);
			for (int i = 0; i < varNames.length; i++) {
				ms.stats[i] = stats[i].difference(other.stats[i]);  
			}
			return ms;
		}
		
		public void printFormatted(PrintStream ps) {
			ps.println(name);
			String[] heading = new String[] { "Variable", "Mean", "SD", "Min", "Max", "Tstat"};
			String[][] twoDim = new String[stats.length + 1][];
			twoDim[0] = heading;
			
			for (int i = 1; i < twoDim.length; i++) {
				String[] row = new String[heading.length];
				row[0] = varNames[i-1];
				row[1] = format(stats[i-1].mean());
				row[2] = format(stats[i-1].sd());
				row[3] = format(stats[i-1].min());
				row[4] = format(stats[i-1].max());
				row[5] = format(stats[i-1].tstat());
				twoDim[i] = row;
			}
			PrintUtils.printFormattedMatrix(ps, twoDim, 2);
		}
		
		NumberFormat df = DecimalFormat.getNumberInstance();
		NumberFormat iff = DecimalFormat.getIntegerInstance();
		
		private String format(double d) {
			df.setMaximumFractionDigits(2);
			if (Math.abs(d) > 100000) {
				return iff.format(d);
			} else {
				return df.format(d);
			}
		}
		
		public void initialize(String... names) {
			varNames = new String[names.length];
			stats = new SimpleStats[names.length];
			for (int i = 0; i < names.length; i++) {
				varNames[i] = names[i];
				stats[i] = new SimpleStats();
			}
		}
		
		
	}
	public class Result {
		public String fen;
		public int nodes;
		public int timeMs;
		public int externalEval;
		public int externalNodes;
	}

}
