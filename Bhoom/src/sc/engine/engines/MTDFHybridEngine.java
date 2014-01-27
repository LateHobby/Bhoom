package sc.engine.engines;

import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.MTDFEngine.Mode;

/** Sets the next beta according to the binary rule once every five iterations. */
public class MTDFHybridEngine extends MTDFEngine {

	public MTDFHybridEngine(String name, Evaluator eval, EvalTT ttable,
			MoveSorter sorter, boolean useLateMoveReductions,
			boolean useNullMoves, boolean useHistoryHeuristic,
			boolean useKillerMoves, boolean useFutilityPruning) {
		super(name, eval, ttable, sorter, useLateMoveReductions, useNullMoves,
				useHistoryHeuristic, useKillerMoves, useFutilityPruning);
	}

	@Override
	public Continuation mtd(EngineBoard board, int f, int d) {
		return mtdGeneric(board, f, d, Mode.HYBRID);
	}
}
