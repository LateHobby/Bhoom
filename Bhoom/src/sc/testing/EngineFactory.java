package sc.testing;

import sc.engine.SearchEngine;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.engines.CEngine;
import sc.engine.engines.CTestEngine;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.ObjectPool.Factory;

public class EngineFactory implements Factory<SearchEngine>{

	private SearchMode mode;
	int aspWin = 200;
	private  boolean useTTable= true;
	private  boolean useMoveSorter= true;
	
	private  boolean useNullMoves= true;
	private  boolean useLateMoveReduction= true;
	private  boolean useHistoryHeuristic= true;
	private  boolean useKillerMoves= true;
	private  boolean useFutilityPruning= true;
	private boolean test;

	public EngineFactory(SearchMode mode) {
		this.test = false;
		this.mode = mode;
	}
	
	public EngineFactory(SearchMode mode, boolean useTTable, boolean useMoveSorter, boolean useNullMoves,
			boolean useLateMoveReduction, boolean useHistoryHeuristic,
			boolean useKillerMoves, boolean useFutilityPruning) {
		this.test = true;
		this.mode = mode;
		this.useTTable = useTTable;
		this.useMoveSorter = useMoveSorter;
		this.useNullMoves = useNullMoves;
		this.useLateMoveReduction = useLateMoveReduction;
		this.useHistoryHeuristic = useHistoryHeuristic;
		this.useKillerMoves = useKillerMoves;
		this.useFutilityPruning = useFutilityPruning;
	}
	@Override
	public SearchEngine create() {
		if (test) {
			return createTestEngine();
		} else {
			return createEngine();
		}
//		
	}

	private SearchEngine createEngine() {
		CEngine engine = null;
		String name = getName();
		if (mode == SearchMode.ASP_WIN) {
			engine = new CEngine(name, mode, aspWin, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		} else if (mode == SearchMode.MTDF || mode == SearchMode.BIN_MTDF || mode == SearchMode.HYBRID_MTDF) {
			engine = new CEngine(name, mode, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		} 
		if (engine == null) {
			throw new RuntimeException("Unknown engine class: " + mode);
		}
		return engine;
	}

	public SearchEngine createTestEngine() {
		CTestEngine engine = null;
		String name = getName();
		if (mode == SearchMode.ASP_WIN) {
			engine = new CTestEngine(name, mode, aspWin, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		} else if (mode == SearchMode.MTDF || mode == SearchMode.BIN_MTDF || mode == SearchMode.HYBRID_MTDF) {
			engine = new CTestEngine(name, mode, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		} 
		if (engine == null) {
			throw new RuntimeException("Unknown engine class: " + mode);
		}
		engine.setFlags(useTTable, useMoveSorter, useNullMoves,
				useLateMoveReduction, useHistoryHeuristic,
				useKillerMoves, useFutilityPruning);
		return engine;
	}
	
	public String getName() {
		String name = mode.name();
		
		name += "-" + (useTTable? "" : "no");
		name += "TT";
		name += "-" + (useMoveSorter? "" : "no");
		name += "MS";
		name += "-" + (useNullMoves? "" : "no");
		name += "NM";
		name += "-" + (useLateMoveReduction? "" : "no");
		name += "LMR";
		name += "-" + (useHistoryHeuristic? "" : "no");
		name += "HH";
		name += "-" + (useKillerMoves? "" : "no");
		name += "KM";
		name += "-" + (useFutilityPruning? "" : "no");
		name += "FP";
		return name;
		
	}

	@Override
	public SearchEngine[] getArray(int size) {
		// TODO Auto-generated method stub
		return null;
	}
}

