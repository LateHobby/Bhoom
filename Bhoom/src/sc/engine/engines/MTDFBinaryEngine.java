package sc.engine.engines;

import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.MTDFEngine.Mode;


public class MTDFBinaryEngine extends MTDFEngine {

	public MTDFBinaryEngine(String name, Evaluator eval, EvalTT ttable,
			MoveSorter sorter, boolean useLateMoveReductions,
			boolean useNullMoves, boolean useHistoryHeuristic,
			boolean useKillerMoves, boolean useFutilityPruning) {
		super(name, eval, ttable, sorter, useLateMoveReductions, useNullMoves,
				useHistoryHeuristic, useKillerMoves, useFutilityPruning);
		
	}

	@Override
	public Continuation mtd(EngineBoard board, int f, int d) {
		return mtdGeneric(board, f, d, Mode.BINARY);
	}
	
//	@Override
//	public Continuation mtd(EngineBoard board, int f, int d) {
//		Continuation e2 = null;
//		int g = f;
//		int lbound = Evaluator.MIN_EVAL;
//		int ubound = Evaluator.MAX_EVAL;
//		do {
//			System.out.printf("Init: lbound=%d ubound=%d\n", lbound, ubound);
//			listenerStartSearch();
//			int beta = (ubound - lbound > 5) ? (lbound + ubound)/2 : (g == lbound) ? g+1 : g;
//			LocalVars lv = localVarsPool.allocate();
//			listenerEnteredNode(board, beta-1, beta, false, 0, EngineListener.NORMAL);
//			g = alphaBeta(board, beta-1, beta, d, 0, lv);
//			listenerExitNode(g);
//			if (abandonSearch) {
//				break;
//			}
//			e2 = getContinuation(lv.pv);
//			e2.eval = g;
//			
//			listenerResult(e2);
//			localVarsPool.release(lv);
//			if (g < beta) {
//				ubound = g;
//			} else {
//				lbound = g;
//			}
////			System.out.printf("Term: lbound=%d ubound=%d\n", lbound, ubound);
//		} while (lbound < ubound);
//		return e2;
//	}

}
