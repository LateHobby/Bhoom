package sc.engine.engines;

import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.engines.AbstractEngine.LocalVars;

public class MTDFEngine extends AbstractEngine {

	protected enum Mode {CLASSIC, BINARY, HYBRID};
	
	private Continuation[] evalByDepth = new Continuation[100];
	
	public MTDFEngine(String name, Evaluator eval, EvalTT ttable,
			MoveSorter sorter, boolean useLateMoveReductions, boolean useNullMoves, boolean useHistoryHeuristic, 
			boolean useKillerMoves,  boolean useFutilityPruning) {
		super(name, eval, ttable, sorter, useLateMoveReductions, useNullMoves, useHistoryHeuristic, 
				useKillerMoves, useFutilityPruning);
	}

	@Override
	protected Continuation search(EngineBoard board) {
		stats.cumulative.reset();
		System.out.println(name);
		int maxDepthSearched = 0;
		int currentDepth = 1;
		int guess = -Evaluator.MATE_BOUND;
		do {
			stats.perIter.reset();
			long startTime = System.currentTimeMillis();
			Continuation e2 = mtd(board, guess, currentDepth);
			if (abandonSearch) {
				break;
			} else {
				String thinking = getThinking(startTime, e2, currentDepth);
				System.out.println(thinking);
				if (thinkingListener != null) {
					thinkingListener.thinkingUpdate(thinking);
				}
				System.out.println(stats.perIter.getStatsString());
				guess = e2.eval;
				maxDepthSearched = currentDepth;
				evalByDepth[maxDepthSearched] = e2;
			}
			currentDepth++;
		} while (currentDepth <= maxDepthAllowed);
		
		stats.depth(maxDepthSearched);
		return evalByDepth[maxDepthSearched];

	}



	public Continuation mtd(EngineBoard board, int f, int d) {
		return mtdGeneric(board, f, d, Mode.CLASSIC);
	}


	public Continuation mtdGeneric(EngineBoard board, int f, int d, Mode mode) {
		Continuation e2 = null;
		int g = f;
		int lbound = Evaluator.MIN_EVAL;
		int ubound = Evaluator.MAX_EVAL;
		int iterationCount = 0;
		do {
			System.out.printf("Init: lbound=%d ubound=%d\n", lbound, ubound);
			listenerStartSearch();
			int beta = getNextBeta(g, lbound, ubound, iterationCount, mode);
			
			LocalVars lv = localVarsPool.allocate();
			listenerEnteredNode(board, beta-1, beta, false, 0, EngineListener.NORMAL);
			g = alphaBeta(board, beta-1, beta, d, 0, lv);
			listenerExitNode(g);
			if (abandonSearch) {
				break;
			}
			e2 = getContinuation(lv.pv);
			e2.eval = g;
			
			listenerResult(e2);
			localVarsPool.release(lv);
			if (g < beta) {
				ubound = g;
			} else {
				lbound = g;
			}
			iterationCount++;
//			System.out.printf("Term: lbound=%d ubound=%d\n", lbound, ubound);
		} while (lbound < ubound);
		return e2;
	}

	protected int getNextBeta(int g, int lbound, int ubound, int iterationCount,
			Mode mode) {
		switch (mode) {
		case CLASSIC: return (g == lbound) ? g+1 : g;
		case BINARY: return (ubound - lbound > 5) ? 
				(lbound + ubound)/2 : (g == lbound) ? g+1 : g;
		case HYBRID: return (ubound - lbound > 5 && 
				(iterationCount > 0 && (iterationCount % 5) == 0)) ? 
						(lbound + ubound)/2 : (g == lbound) ? g+1 : g;
			default: throw new RuntimeException("Unknown mode:" + mode);
		}
	}







}
