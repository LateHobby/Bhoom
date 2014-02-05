package sc.engine.engines;

import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;

public class CEngine extends AbstractEngine {

	public static final boolean listen = "true".equals(System.getProperty("Bhoom.debug"));
	
	// switches
	public static final boolean useTTable= "true".equals(System.getProperty("ttable"));
	public static final boolean useMoveSorter= "true".equals(System.getProperty("moveSorter"));
	
	public static final boolean useNullMoves= "true".equals(System.getProperty("nullMoves"));
	public static final boolean useLateMoveReduction= "true".equals(System.getProperty("lmr"));
	public static final boolean useHistoryHeuristic= "true".equals(System.getProperty("historyHeuristic"));
	public static final boolean useKillerMoves= "true".equals(System.getProperty("killerMoves"));
	public static final boolean useFutilityPruning= "true".equals(System.getProperty("futilityPruning"));

	public CEngine(String name, SearchMode mode, int aspWin, Evaluator eval, EvalTT ttable,
			MoveSorter sorter) {
		super(name, mode, aspWin, eval, ttable, sorter);
	}
	
	public CEngine(String name, SearchMode mode, Evaluator eval, EvalTT ttable,
			MoveSorter sorter) {
		super(name, mode, eval, ttable, sorter);
	}

	@Override
	public boolean useTTable() {
		return useTTable;
	}

	@Override
	public boolean useMoveSorter() {
		return useMoveSorter;
	}

	@Override
	public boolean useNullMoves() {
		return useNullMoves;
	}

	@Override
	public boolean useLateMoveReduction() {
		return useLateMoveReduction;
	}

	@Override
	public boolean useHistoryHeuristic() {
		return useHistoryHeuristic;
	}

	@Override
	public boolean useKillerMoves() {
		return useKillerMoves;
	}

	@Override
	public boolean useFutilityPruning() {
		return useFutilityPruning;
	}

	@Override
	protected Continuation search(EngineBoard board) {
		// TODO Auto-generated method stub
		return null;
	}

}
