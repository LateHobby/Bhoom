package sc.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.TreeMap;

import sc.engine.EngineStats;
import sc.testing.TestingUtils.SuiteResult;
import sc.testing.TestingUtils.TestResult;
import sc.util.PrintUtils;
import sc.util.SimpleStats;

public class CompareBenchmarks {

	static NumberFormat df = DecimalFormat.getNumberInstance();
	static NumberFormat iff = DecimalFormat.getIntegerInstance();
	
	public static void printComparison(PrintStream stream, File fileOld, File fileNew) throws FileNotFoundException, ClassNotFoundException, IOException {
		SuiteResult oldr = readResult(fileOld);
		SuiteResult newr = readResult(fileNew);
		printComparison(stream, oldr, newr);
	}
	
	public static void printIndividual(PrintStream stream, File fileOld) throws FileNotFoundException, ClassNotFoundException, IOException {
		SuiteResult oldr = readResult(fileOld);
		printIndividual(stream, oldr);
	}
	
	private static SuiteResult readResult(File file) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		return (SuiteResult) ois.readObject();
	}
	
	private static void printComparison(PrintStream stream, SuiteResult oldr, SuiteResult newr) {
		stream.print(oldr.engineName + "[" + oldr.engineSetting + "]");
		stream.print("  vs  ");
		stream.println(newr.engineName + "[" + newr.engineSetting + "]");
		
		String[] heading = new String[] { "Variable", "Mean", "SD", "Min", "Max", "Tstat"};
		String[] params = new String[] { "Nodes", "Time", "TTHitsN", "TTHitsQ", "AvgBN", "AvgBQ", "Depth" };
		SimpleStats[] sa = new SimpleStats[params.length];
		
		for (int i = 0; i < sa.length; i++) {
			sa[i] = new SimpleStats();
		}
		
		Map<String, TestResult> oldMap = getResultMap(oldr);
		Map<String, TestResult> newMap = getResultMap(newr);
		
		int count = 0;
		for (TestResult oldt: oldMap.values()) {
			TestResult newt = newMap.get(oldt.testName);
			if (newt == null) continue;
			count++;
			EngineStats olds = oldt.stats;
			EngineStats news = newt.stats;
			
			sa[0].include( olds.getNodes(false) + olds.getNodes(true) - (news.getNodes(false) + news.getNodes(true)));
			sa[1].include(oldt.timeMs - newt.timeMs);
			sa[2].include(olds.getTTHitPercentage(false) - news.getTTHitPercentage(false));
			sa[3].include(olds.getTTHitPercentage(true) - news.getTTHitPercentage(true));
			sa[4].include(olds.getAvgBetaCutoffIndex(false) - news.getAvgBetaCutoffIndex(false));
			sa[5].include(olds.getAvgBetaCutoffIndex(true) - news.getAvgBetaCutoffIndex(true));
			sa[6].include(olds.getDepth() - news.getDepth());
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
		
		stream.println("Number of tests:" + count);
	}
	
	private static void printIndividual(PrintStream stream, SuiteResult oldr) {
		stream.println(oldr.engineName + "[" + oldr.engineSetting + "]");
		
		
		String[] heading = new String[] { "Variable", "Mean", "SD", "Min", "Max", "Tstat"};
		String[] params = new String[] { "Nodes", "Time", "TTHitsN", "TTHitsQ", "AvgBN", "AvgBQ", "Depth" };
		SimpleStats[] sa = new SimpleStats[params.length];
		
		for (int i = 0; i < sa.length; i++) {
			sa[i] = new SimpleStats();
		}
		
		Map<String, TestResult> oldMap = getResultMap(oldr);
		
		int count = 0;
		for (TestResult oldt: oldMap.values()) {
			count++;
			EngineStats olds = oldt.stats;
			
			sa[0].include( olds.getNodes(false) + olds.getNodes(true) );
			sa[1].include(oldt.timeMs);
			sa[2].include(olds.getTTHitPercentage(false));
			sa[3].include(olds.getTTHitPercentage(true));
			sa[4].include(olds.getAvgBetaCutoffIndex(false));
			sa[5].include(olds.getAvgBetaCutoffIndex(true));
			sa[6].include(olds.getDepth());
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

	private static Map<String, TestResult> getResultMap(SuiteResult oldr) {
		TreeMap<String, TestResult> map = new TreeMap<String, TestResult>();
		for (TestResult tr : oldr.results) {
			map.put(tr.testName, tr);
		}
		return map;
	}

	private static String format(double d) {
		df.setMaximumFractionDigits(2);
		if (Math.abs(d) > 100000) {
			return iff.format(d);
		} else {
			return df.format(d);
		}
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException {
//		SuiteResult sr = readResult(new File("testing/benchmarks/ECM-NFullQuiescence-1Min.ser"));
//		System.out.println(sr.fileName);
//		System.out.println(sr.engineName);
//		System.out.println(sr.engineSetting);
//		for (TestResult tr : sr.results) {
//			System.out.println(tr);
//		}
//		SuiteResult oldr = readResult(new File("testing/benchmarks/SMALL-IDAsp-100Sec.ser"));
		SuiteResult oldr = readResult(new File("testing/benchmarks/SMALL-IDAsp-SeeWithHash-100Sec.ser"));
		SuiteResult newr = readResult(new File("testing/benchmarks/SMALL-IDAsp-NullMoves-SeeWithHash-100Sec.ser"));
//		SuiteResult oldr = readResult(new File("testing/benchmarks/SMALL-MTDf-100Sec.ser"));
//		SuiteResult newr = readResult(new File("testing/benchmarks/SMALL-MTDf-SeeWithHash-100Sec.ser"));
		printComparison(System.out, oldr, newr);
		
	}

}
