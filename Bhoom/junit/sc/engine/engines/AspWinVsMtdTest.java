package sc.engine.engines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.ExternalUCIEngine;
//
// Status at last run...
//
//AspWin(fast.epd) - Mtd(fast.epd)
//Variable        Mean          SD          Min      Max  Tstat
//   nodes  -6,590,483  21,596,244  -71,704,581  161,265  -1.01
//  timeMs   -5,987.45   19,411.33      -64,510       38  -1.02
//externalEval       -3.18       27.92          -49       59  -0.38
//externalNodes      840.18   16,528.33      -32,430   41,080   0.17
//Mtd(fast.epd)
//Variable       Mean          SD     Min         Max  Tstat
//   nodes  6,631,199  21,588,323  36,349  71,721,802   1.02
//  timeMs   6,074.27   19,389.39      18      64,529   1.04
//externalEval     369.82      437.84    -516       1,019    2.8
//externalNodes  49,445.45   42,965.85   2,659     123,573   3.82
//AspWin(fast.epd)
//Variable       Mean         SD    Min      Max  Tstat
//   nodes  40,715.91  92,605.32  3,319  318,484   1.46
//  timeMs      86.82     209.64      9      718   1.37
//externalEval     366.64     432.61   -516    1,019   2.81
//externalNodes  50,285.64   50,397.6  2,659  164,653   3.31
//Node drop: -16186% (Target:0%)  Time drop: -6896% (Target:-500%) Eval increase: 0% (Target:0%)
//--------------------------------------------------------
//
//AspWin(fast.epd) - Mtd-Binary(fast.epd)
//Variable       Mean         SD      Min      Max  Tstat
//   nodes  -5,919.09     38,765  -35,971  106,376  -0.51
//  timeMs       4.45     101.55      -56      306   0.15
//externalEval         -1      22.45      -45       52  -0.15
//externalNodes     423.09  17,586.05  -37,496   41,080   0.08
//Mtd-Binary(fast.epd)
//Variable       Mean         SD    Min      Max  Tstat
//   nodes     46,635  56,303.15  8,773  212,108   2.75
//  timeMs      77.73     111.01     12      406   2.32
//externalEval     367.64     433.36   -516    1,019   2.81
//externalNodes  49,862.55  43,424.06  1,589  123,573   3.81
//AspWin(fast.epd)
//Variable       Mean         SD    Min      Max  Tstat
//   nodes  40,715.91  92,605.32  3,319  318,484   1.46
//  timeMs      82.18     209.21      5      712    1.3
//externalEval     366.64     432.61   -516    1,019   2.81
//externalNodes  50,285.64   50,397.6  2,659  164,653   3.31
//Node drop: -14% (Target:0%)  Time drop: 5% (Target:-500%) Eval increase: 0% (Target:0%)
//--------------------------------------------------------
//
//AspWin(Test20.EPD) - Mtd-Binary(Test20.EPD)
//Variable       Mean         SD       Min      Max  Tstat
//   nodes  -9,479.55  65,179.67  -142,910  106,405  -0.65
//  timeMs      11.35     164.62      -243      386   0.31
//externalEval       1.25         84      -218      203   0.07
//externalNodes    6,453.9  45,914.31   -59,335  192,720   0.63
//Mtd-Binary(Test20.EPD)
//Variable     Mean         SD     Min      Max  Tstat
//   nodes  167,218  93,438.33  76,698  441,821      8
//  timeMs      385      232.7     121    1,057    7.4
//externalEval    202.2     202.48     -99      810   4.47
//externalNodes  132,123  93,786.16  26,649  400,454    6.3
//AspWin(Test20.EPD)
//Variable     Mean       SD     Min      Max  Tstat
//   nodes  157,739  103,706  39,664  383,083    6.8
//  timeMs   396.35   284.32      90    1,113   6.23
//externalEval   203.45   204.27     -48      810   4.45
//externalNodes  138,577  106,756  26,649  400,619   5.81
//Node drop: -6% (Target:0%)  Time drop: 2% (Target:10%) Eval increase: 0% (Target:0%)
//--------------------------------------------------------
//
//AspWin(Test20.EPD) - Mtd(Test20.EPD)
//Variable      Mean         SD       Min      Max  Tstat
//   nodes  -221,264    256,954  -897,040  107,335  -3.85
//  timeMs    -601.1     880.91    -3,732      387  -3.05
//externalEval    -13.65      72.76      -218      154  -0.84
//externalNodes  7,113.55  50,221.45   -67,593  192,720   0.63
//Mtd(Test20.EPD)
//Variable     Mean         SD      Min        Max  Tstat
//   nodes  379,003    289,724  104,409  1,125,814   5.85
//  timeMs   992.15     956.17      247      4,002   4.64
//externalEval    217.1     187.34      -48        810   5.18
//externalNodes  131,463  93,849.07   26,649    400,619   6.26
//AspWin(Test20.EPD)
//Variable     Mean       SD     Min      Max  Tstat
//   nodes  157,739  103,706  39,664  383,083    6.8
//  timeMs   391.05   276.78      93    1,075   6.32
//externalEval   203.45   204.27     -48      810   4.45
//externalNodes  138,577  106,756  26,649  400,619   5.81
//Node drop: -140% (Target:0%)  Time drop: -153% (Target:10%) Eval increase: 6% (Target:0%)
//--------------------------------------------------------
//

public class AspWinVsMtdTest extends ComparisonTestBase {

	private static final int DEPTH = 7;
	
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
			resultLog = new PrintStream(new FileOutputStream("testing/tmp/AspWinVsMtdTestResults" + timestamp + ".log"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	boolean[] flagsAll = new boolean[]{true, true, true, true, true, true, true};
	
	@Test
	public void aspWinVsMtdFast() throws IllegalAccessException, IOException {
		
		CTestEngine aspWin = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(aspWin, flagsAll);
		
		CTestEngine mtd = new CTestEngine("Mtd", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mtd, flagsAll);
		
		
		compareEngines(mtd, aspWin, DEPTH, uci, 0, -500, 0, fastSuite, false, resultLog);
	}
	
	@Test
	public void aspWinVsMtd20() throws IllegalAccessException, IOException {
		
		CTestEngine aspWin = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(aspWin, flagsAll);
		
		CTestEngine mtd = new CTestEngine("Mtd", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mtd, flagsAll);
		
		
		compareEngines(mtd, aspWin, DEPTH, uci, 0, 10, 0, suite20, true, resultLog);
	}
	
	@Test
	public void aspWinVsMtdBinaryFast() throws IllegalAccessException, IOException {
		
		CTestEngine aspWin = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(aspWin, flagsAll);
		
		CTestEngine mtd = new CTestEngine("Mtd-Binary", SearchMode.BIN_MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mtd, flagsAll);
		
		
		compareEngines(mtd, aspWin, DEPTH, uci, 0, -500, 0, fastSuite, false, resultLog);
	}
	
	@Test
	public void aspWinVsMtdBinary20() throws IllegalAccessException, IOException {
		
		CTestEngine aspWin = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(aspWin, flagsAll);
		
		CTestEngine mtd = new CTestEngine("Mtd-Binary", SearchMode.BIN_MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		setFlags(mtd, flagsAll);
		
		
		compareEngines(mtd, aspWin, DEPTH, uci, 0, 10, 0, suite20, true, resultLog);
	}

}
