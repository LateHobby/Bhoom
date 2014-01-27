package sc.engine.engines;

import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;

/**
 * Iterative deepening engine
 * 
 * @author Shiva
 * 
 */
public class IDAspWinEngine extends AbstractEngine {

	protected int ASP_WIN;
	protected Continuation[] evalByDepth = new Continuation[100];

	public IDAspWinEngine(String name, Evaluator eval, EvalTT ttable,
			int aspWin, boolean useLateMoveReductions, boolean useNullMoves,
			boolean useHistoryHeuristic, boolean useKillerMoves,
			boolean useFutilityPruning) {
		this(name, eval, ttable, null, aspWin, useLateMoveReductions,
				useNullMoves, useHistoryHeuristic, useKillerMoves,
				useFutilityPruning);
	}

	public IDAspWinEngine(String name, Evaluator eval, EvalTT ttable,
			MoveSorter sorter, int aspWin, boolean useLateMoveReductions,
			boolean useNullMoves, boolean useHistoryHeuristic,
			boolean useKillerMoves, boolean useFutilityPruning) {
		super(name, eval, ttable, sorter, useLateMoveReductions, useNullMoves,
				useHistoryHeuristic, useKillerMoves, useFutilityPruning);
		ASP_WIN = aspWin;
	}

	@Override
	protected Continuation search(EngineBoard board) {
		stats.cumulative.reset();
		System.out.println(name);
		int currentSearchDepth = 1;
		int maxDepthSearched = 0;
		int alpha = Evaluator.MIN_EVAL;
		int beta = Evaluator.MAX_EVAL;
		do {

			stats.perIter.reset();
			long startTime = System.currentTimeMillis();
			listenerStartSearch();
			LocalVars localVars = localVarsPool.allocate();
			listenerEnteredNode(board, alpha, beta, false, 0, EngineListener.NORMAL);
			if (currentSearchDepth == 5) {
				int j = 1;
			}
			int eval = alphaBeta(board, alpha, beta, currentSearchDepth, 0,
					localVars);
			listenerExitNode(eval);
			Continuation e2 = getContinuation(localVars.pv);
			// Continuation e2 = getContinuationFromPVTable(board,
			// currentSearchDepth);
			e2.eval = eval;
			listenerResult(e2);
			localVarsPool.release(localVars);
			// Continuation e2 = evaltt.getContinuation(board,
			// currentSearchDepth);
			if (abandonSearch) {
				break;
			}
			evalByDepth[currentSearchDepth] = e2;
			if ((alpha > Evaluator.MIN_EVAL && eval <= alpha)
					|| (beta < Evaluator.MAX_EVAL && eval >= beta)) {
				alpha = Evaluator.MIN_EVAL;
				beta = Evaluator.MAX_EVAL;
				System.out.println("Repeating depth:" + currentSearchDepth);
			} else {
				String thinking = getThinking(startTime, e2, currentSearchDepth);
				System.out.println(thinking);
				if (thinkingListener != null) {
					thinkingListener.thinkingUpdate(thinking);
				}
				System.out.println(stats.perIter.getStatsString());
				maxDepthSearched = currentSearchDepth;
				currentSearchDepth++;
				alpha = eval - ASP_WIN;
				beta = eval + ASP_WIN;

			}
		} while (currentSearchDepth <= maxDepthAllowed);

		stats.depth(maxDepthSearched);
		return evalByDepth[maxDepthSearched];
	}

//	@Override
	protected Continuation searchSimple(EngineBoard board) {
		stats.cumulative.reset();
		System.out.println(name);
		int currentSearchDepth = maxDepthAllowed;
		int maxDepthSearched = 0;
		int alpha = Evaluator.MIN_EVAL;
		int beta = Evaluator.MAX_EVAL;

		stats.perIter.reset();
		long startTime = System.currentTimeMillis();
		listenerStartSearch();
		LocalVars localVars = localVarsPool.allocate();
		listenerEnteredNode(board, alpha, beta, false, 0, EngineListener.NORMAL);

		int eval = alphaBeta(board, alpha, beta, currentSearchDepth, 0,
				localVars);
		listenerExitNode(eval);
		Continuation e2 = getContinuation(localVars.pv);
		// Continuation e2 = getContinuationFromPVTable(board,
		// currentSearchDepth);
		e2.eval = eval;
		listenerResult(e2);
		localVarsPool.release(localVars);
		// Continuation e2 = evaltt.getContinuation(board, currentSearchDepth);
		String thinking = getThinking(startTime, e2, currentSearchDepth);
		System.out.println(thinking);
		if (thinkingListener != null) {
			thinkingListener.thinkingUpdate(thinking);
		}
		System.out.println(stats.perIter.getStatsString());

		evalByDepth[currentSearchDepth] = e2;

		stats.depth(maxDepthSearched);
		return e2;
	}

}
