package sc.engine.engines;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sc.bboard.OneSidePositionInfo;
import sc.bboard.PositionInfo;
import sc.encodings.Encodings;
import sc.engine.EStats;
import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EngineStats;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.SearchEngine;
import sc.engine.ThinkingListener;
import sc.engine.ttables.AlwaysReplace;
import sc.util.IntStack;
import sc.util.ObjectPool;
import sc.util.ObjectPool.Factory;
import sc.util.Poolable;
import sc.util.PrintUtils;
import sc.util.TTable;
import sc.util.TTable.TTEntry;

abstract public class AbstractEngine implements SearchEngine {

	private static final int MATE_DEPTH = 128;
	private static final int POS_MARGIN = 100; // for futility pruning
	
	//for debugging
	EngineListener listener;
	ThinkingListener thinkingListener;
	
	// switches
	protected boolean useNullMoves;
	protected boolean useLateMoveReduction;
	protected boolean useHistoryHeuristic;
	protected boolean useKillerMoves;
	protected boolean useFutilityPruning;
	
	protected boolean abandonSearch;
	protected int maxDepthAllowed;

	protected TTEntry stored = new TTEntry();
	protected EStats stats = new EStats();
	protected ObjectPool<LocalVars> localVarsPool;
	protected Evaluator evaluator;
	protected EvalTT evaltt;
//	protected EvalTT pvtt = new AlwaysReplace(18);
	protected MoveSorter moveSorter;
	protected String name;
	protected Timer timer = new Timer();
	protected IntStack moveStack = new IntStack(500);

	protected AbstractEngine(String name, Evaluator eval, EvalTT ttable, 
			boolean useLateMoveReductions, boolean useNullMoves, boolean useHistoryHeuristic, boolean useKillerMoves, boolean useFutilityPruning) {
		this(name, eval, ttable, null, useLateMoveReductions, useNullMoves, useHistoryHeuristic, useKillerMoves, useFutilityPruning);
	}

	protected AbstractEngine(String name, Evaluator eval, EvalTT ttable,
			MoveSorter sorter, boolean useLateMoveReductions, boolean useNullMoves,
			boolean useHistoryHeuristic, boolean useKillerMove, boolean useFutilityPruning) {
		this.name = name;
		evaluator = eval;
		moveSorter = sorter;
		evaltt = ttable;
		this.useLateMoveReduction = useLateMoveReductions;
		this.useNullMoves = useNullMoves;
		this.useHistoryHeuristic = useHistoryHeuristic;
		this.useKillerMoves = useKillerMove;
		this.useFutilityPruning = useFutilityPruning;
		
		localVarsPool = new ObjectPool<LocalVars>(new Factory<LocalVars>(

		) {

			@Override
			public LocalVars create() {
				return new LocalVars();
			}

			@Override
			public LocalVars[] getArray(int size) {
				return new LocalVars[size];
			}
		}, 256, "localVarsPool");
	}

	@Override
	public Continuation searchByDepth(EngineBoard board, int searchDepth) {
		moveStack.clear();
		maxDepthAllowed = searchDepth;
		abandonSearch = false;
		return search(board);
	}

	@Override
	public Continuation searchByTime(EngineBoard board, long msTime) {
		moveStack.clear();
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			@Override
			public void run() {
				abandonSearch = true;
				
			}
		};
		
		ses.schedule(command, msTime, TimeUnit.MILLISECONDS);
		maxDepthAllowed = 1000; // infinity, effectively
		return search(board);
	}

	@Override
	public Continuation search(EngineBoard board, int depth, int engineTime,
			int engineInc, int moveTime) {
		System.out.printf("Search: depth=%d engineTime=%d, moveTime=%d\n", depth, engineTime, moveTime);
		abandonSearch = false;
		int timeAllowed = 0;
		if (moveTime > 0) {
			timeAllowed = moveTime;
		} else if (engineTime > 0) {
			int movesToDo = 50 - board.getFullMoveNumber();
			if (movesToDo < 0) {
				movesToDo = 75 - board.getFullMoveNumber();
			}
			timeAllowed = (engineTime + (40 -1) * engineInc)/40;
//			int timeLeft = engineTime + movesToDo * engineInc;
//			timeAllowed = timeLeft / movesToDo;
		}
		System.out.printf("Abandon after: %d\n", timeAllowed);
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			@Override
			public void run() {
				abandonSearch = true;
				
			}
		};
		
		ses.schedule(command, timeAllowed, TimeUnit.MILLISECONDS);
		maxDepthAllowed = (depth <= 0) ? 1000 : depth;
		System.out.printf("MaxDepthAllowed:%d\n", maxDepthAllowed);
		listenerStartSearch();
		return search(board);

	}

	/**
	 * Returns the best continuation found. Respects both the maxDepthAllowed
	 * and the abandonSearch variables.
	 * 
	 * @param board
	 * @return
	 */
	abstract protected Continuation search(EngineBoard board);

	protected int alphaBeta(EngineBoard board, int alpha, int beta,
			int depthLeft, int ply, LocalVars localVars) {
		if (abandonSearch) {
			return beta; // Doesn't matter what is returned, so return cutoff
		}
		boolean quiesce = (depthLeft <= 0);
		stats.addNode(quiesce);
		localVars.alpha = alpha;
		
		if (retrieveEvalCutoff(board, alpha, beta, depthLeft, localVars)) {
			stats.ttHit(quiesce);
			return localVars.eval;
		}
		
		if (isDraw(board)) {
			return -contemptFactor(board);
		}
		
		alpha = localVars.alpha;
		
		if (ply > 0 && noMoveEvalCutoff(board, alpha, beta, depthLeft, ply, quiesce, localVars)) {
//			store(board, alpha, beta, localVars.eval, 0, depthLeft);
			stats.betaCutoff(0, quiesce);
			return localVars.eval;
		}
		alpha = localVars.alpha;

		
		int terminalEval = evaluator.evaluate(board);
		
		if (isFutilityPruned(board, alpha, beta, terminalEval, depthLeft)) {
			localVars.eval = alpha;
			return alpha;
		}
		
		int numMoves = generateMoves(board, alpha, beta, depthLeft, terminalEval, quiesce, localVars);

		if (numMoves == 0) {
			return terminalEval(board, quiesce, terminalEval, depthLeft);
		}

		

		if (moveSorter != null) {
			moveSorter.sortMoves(board, ply, localVars.hashMove, localVars.moves, numMoves);
		}

		int bestScore = Evaluator.MIN_EVAL;
		int bestMove = 0;
		boolean pvFound = false;

		for (int i = 0; i < numMoves; i++) {
			int move = localVars.getMove(i);
			int newDepthLeft = (depthLeft <= 0) ? 0 : depthLeft - 1;
			makeMove(board, move);
			int score;
			if (pvFound && useLateMoveReduction &&
					lateMoveReductionEval(board, alpha, beta, newDepthLeft, ply,
							quiesce, move, localVars)) {
				score = localVars.eval;
			} else {
				LocalVars lv = localVarsPool.allocate();
				listenerEnteredNode(board, alpha, beta, quiesce, move, EngineListener.NORMAL);
				score = -alphaBeta(board, -beta, -alpha, newDepthLeft, ply+1, 
						 lv);
				listenerExitNode(score);
				localVars.currentPV.copy(lv.pv);
				localVarsPool.release(lv);
			}
			undoLastMove(board, move);
			
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				localVars.bestPV.copy(localVars.currentPV);
			} else {
				incrementHistoryHeuristicArray(move, false);
			}
			
			if (score >= beta) {
				stats.betaCutoff(i, quiesce);
				incrementHistoryHeuristicArray(move, true);
				addToKillerMoves(board, ply, move, localVars.hashMove);
				break;
			}
			if (score > alpha) {
				stats.alphaImprovement(i);
				alpha = score;
			}
			pvFound = true;
		}

		store(board, alpha, beta, bestScore, bestMove, depthLeft);
		localVars.pv.addMove(bestMove);
		localVars.pv.addChild(localVars.bestPV);
		localVars.bestMove = bestMove;
		return bestScore;

	}


	

	private boolean isFutilityPruned(EngineBoard board, int alpha, int beta,
			int terminalEval, int depthLeft) {
		if (useFutilityPruning && depthLeft <= 1) {
			int futilityGap = alpha - terminalEval - POS_MARGIN;
			if (futilityGap > 0) { 
				int mvopv = mostValuableOpponentPieceValue(board);
				if (mvopv < futilityGap) {
					listenerFutilityPrune(terminalEval, mvopv);
					// fail low
//					System.out.printf("Fail-low: TEval=%d alpha=%d MVP=%d\n", terminalEval, alpha, mvopv);
					return true;
				}
			}
		}
		return false;
	}

	

	private int mostValuableOpponentPieceValue(EngineBoard board) {
		PositionInfo pinfo = board.getPositionInfo();
		OneSidePositionInfo osp = board.getWhiteToMove() ? pinfo.bConfig : pinfo.wConfig;
		byte mvp = osp.most_valuable_piece;
		return evaluator.pieceWeight(mvp);
	}

	private void addToKillerMoves(EngineBoard board, int ply, int move,
			int hashMove) {
		if (useKillerMoves && moveSorter != null) {
			moveSorter.addToKillerMoves(board, ply, move, hashMove);
		}
		
	}

	private void incrementHistoryHeuristicArray(int move, boolean increment) {
		if (useHistoryHeuristic && moveSorter != null) {
			moveSorter.incrementHistoryHeuristicArray(move, increment);
		}
	}

	protected int generateMoves(EngineBoard board, int alpha, int beta, int depthLeft, int terminalEval, boolean quiesce,
			LocalVars localVars) {
		
		if (quiesce) {
			return board.getMoveGenerator().fillLegalCaptures(localVars.moves,
					0);
		} else {
			return board.getMoveGenerator().fillLegalMoves(localVars.moves, 0);
		}
		
	}

	
	
	/**
	 * Returns true if a no move eval is permissible and exceeds beta. If it is
	 * computable, the null move eval is computed and stored in
	 * localVars.nullMoveEval.
	 * 
	 * For quiesce mode, the no move eval is the stand pat eval. For non-quiesce
	 * mode, it is the null move eval.
	 * 
	 * @param board
	 * @param evaluator
	 * @param alpha
	 * @param beta
	 * @param depthLeft
	 * @param quiesce
	 * @param localVars
	 * @return
	 */
	protected boolean noMoveEvalCutoff(EngineBoard board, int alpha, int beta,
			int depthLeft, int ply, boolean quiesce, LocalVars localVars) {
		localVars.alpha = alpha;
		if (quiesce) {
			if (board.kingInCheck(board.getWhiteToMove())) {
				return false;
			}
			localVars.eval = evaluator.evaluate(board);
			if (localVars.eval > alpha) {
				localVars.alpha = localVars.eval;
			}
			return localVars.eval >= beta;
		} else {
			if (useNullMoves) {
				return nullMoveEvalCutoff(board, alpha, beta, depthLeft, ply, localVars);
			}
		}
		return false;
	}

	/**
	 * Called when no moves are available. Returns the evaluator value if in
	 * quiesce mode and either a mate or a stalemate value if not in quiesce
	 * mode.
	 * 
	 * @param board
	 * @param quiesce
	 * @param depthLeft
	 * @param localVars
	 * @return
	 */
	private int terminalEval(EngineBoard board, boolean quiesce, int terminalEval, int depthLeft) {
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		int eval;
		if (quiesce) {
			eval = terminalEval;
		} else {
			if (inCheck) { // checkmate
				eval = -Evaluator.MATE_BOUND;
			} else { // stalemate
				eval = 0;
			}
		}
		// fake alpha and beta bracketing value to simulate TType.EXACT
		store(board, eval - 1, eval + 1, eval, 0, depthLeft);
		return eval;
	}

	/**
	 * Returns true if the retrieved value indicates that there is no need
	 * to search the node. This happens if the value of the node is either
	 * <pre>
	 * 1) Exactly known
	 * 2) Lowerbounded above beta
	 * 3) Upperbounded below alpha
	 * </pre>
	 * @param board
	 * @param alpha
	 * @param beta
	 * @param depthLeft
	 * @param lv
	 * @return
	 */
	private boolean retrieveEvalCutoff(EngineBoard board, int alpha, int beta,
			int depthLeft, LocalVars lv) {
		lv.alpha = alpha;
		lv.hashMove = 0;
		if (evaltt == null) {
			return false;
		}
		boolean rv = false;
		if (evaltt.retrieveFromTT(board, stored)) {// set the values of stored
			lv.hashMove = stored.move;
			// if the stored result has depth less than depthleft all bets are off
			if (depthLeft <= stored.depthLeft) { 
				if (stored.type == TTable.EXACT) {
					rv = true;
				}
				if (stored.type == TTable.LOWERBOUND) {
					if (stored.eval > alpha) {
						lv.alpha = stored.eval;
					}
					rv = (stored.eval >= beta);
				}
				if (stored.type == TTable.UPPERBOUND) {
					rv = (stored.eval <= alpha);
				}
				
				if (rv) {
					lv.eval = stored.eval;
					lv.bestMove = stored.move;
					lv.pv.length = 1;
					lv.pv.line[0] = lv.bestMove;
					listenerTTHit(stored.type);
				}
			}
		}
		return rv;
	}

	private void store(EngineBoard board, int alpha, int beta, int eval,
			int move, int depthLeft) {
		if (move == 0) {
//			System.out.println("Not storing zero move");
			return;
		}
		if (evaltt != null) {
			stored.type = TTable.EXACT;
			if (eval <= alpha) {
				stored.type = TTable.UPPERBOUND;
			}
			if (eval >= beta) {
				stored.type = TTable.LOWERBOUND;
			}
			stored.eval = eval;
			stored.move = move;
			stored.depthLeft = depthLeft;
			evaltt.storeToTT(board, stored);
		}
	}

	/**
	 * Returns true if a late move reduction eval is permissible and computable.
	 * If computable, it is computed and stored in localVars.lmrEval
	 * 
	 * @param board
	 * @param alpha
	 * @param beta
	 * @param newDepthLeft
	 * @param quiesce
	 * @param localVars
	 * @return
	 */
	protected boolean lateMoveReductionEval(EngineBoard board, int alpha,
			int beta, int newDepthLeft, int ply, boolean quiesce, int move, LocalVars localVars) {
		
		if (board.kingInCheck(board.getWhiteToMove())) {
			return false;
		}
		if (newDepthLeft < 2) {
			return false;
		}
		LocalVars lv = localVarsPool.allocate();
		listenerEnteredNode(board, beta-1, beta, quiesce, move, EngineListener.LMR_ZW);
		int score = -alphaBeta(board, -beta, -beta+1, newDepthLeft-1, ply+1, 
				 lv);
		listenerExitNode(score);
		localVars.currentPV.copy(lv.pv);
		if (score > alpha) { // do full search
			listenerEnteredNode(board, alpha, beta, quiesce, move, EngineListener.LMR_FULL);
			score = -alphaBeta(board, -beta, -alpha, newDepthLeft, ply+1, 
					 lv);
			listenerExitNode(score);
			localVars.currentPV.copy(lv.pv);
		} else {
//			System.out.println("lmr success");
		}
		localVarsPool.release(lv);
		localVars.eval = score;
		return true;
	}

	/**
	 * Returns true if a null move eval is permissible and the eval is greater
	 * than beta. If computable, the null move eval is stored in
	 * localVars.nullMoveEval.
	 * 
	 * @param board
	 * @param alpha
	 * @param beta
	 * @param depthLeft
	 * @param localVars
	 * @return
	 */
	protected boolean nullMoveEvalCutoff(EngineBoard board, int alpha,
			int beta, int depthLeft, int ply, LocalVars localVars) {
		if (lastMoveWasNull()) { // don't allow the same side to make two consecutive null moves
			return false;
		}
		if (board.kingInCheck(board.getWhiteToMove())) {
			return false;
		}
		if (depthLeft < 2) {
			return false;
		}
//		int staticEval = evaluator.evaluate(board);
//		if (staticEval < 300) {
//			return false;
//		}
		int R = 2;
		stats.tryNull();
		makeMove(board, 0);
		LocalVars lv = localVarsPool.allocate();
		listenerEnteredNode(board, beta-1, beta, false, 0, EngineListener.NULLMOVE_ZW);
		int score = -alphaBeta(board, -beta, -beta+1, depthLeft-R, ply+1,  lv);
		listenerExitNode(score);
		localVarsPool.release(lv);
		undoLastMove(board, 0);
		
		if (score >= beta) {
			localVars.eval = score;
			stats.nullCutoff();
			return true;
		}
//		if (score >= beta) {
//			lv = localVarsPool.allocate();
//			listenerEnteredNode(board, alpha, beta, false, 0, EngineListener.NULLMOVE_QUIESCENT);
//			score = -alphaBeta(board, -beta, -alpha, 0, ply+1, lv);
//			listenerExitNode(score);
//			localVarsPool.release(lv);
//			localVars.eval = score;
//			stats.nullCutoff();
//			return true;
//		}
//		if (score > localVars.alpha) {
//			localVars.alpha = score;
//		}
		return false;
	}

	protected boolean lastMoveWasNull() {
		if (moveStack.size() >= 2) {
			return moveStack.peek(-1) == 0;
		}
		return false;
	}

	protected void makeMove(EngineBoard board, int move) {
		if (move != 0) {
			board.makeMove(move, false);
		} else {
			board.makeNullMove();
		}
		moveStack.push(move);
	}
	
	protected void undoLastMove(EngineBoard board, int move) {
		if (move != 0) {
			board.undoLastMove();
		} else {
			board.undoNullMove();
		}
		moveStack.pop();
	}
	
	private boolean isDraw(EngineBoard board) {
		if (board.getHalfMoveClock() >= 50) {
			return true;
		}
		if (board.drawByRepetition()) {
			return true;
		}
		return false;
	}
	
	private int contemptFactor(EngineBoard board) {
		if (board.getFullMoveNumber() < 30) {
			return 50;
		} else if (board.getFullMoveNumber() < 50) {
			return 25;
		} else {
			return 0;
		}
	}
	
	// Taken from mediocre, basically
	protected String getThinking(long startTime, Continuation e2, int depth) {
		StringBuilder sb = new StringBuilder();
		int eval = e2.eval;
		int length = 0;
		while (e2.line[length] != 0) {
			sb.append(PrintUtils.notation(e2.line[length]));
			sb.append(" ");
			length++;
		}
		
		long nodesSearched = stats.perIter.getNodes(false) + stats.perIter.getNodes(true);
		long splitTime = (System.currentTimeMillis() - startTime);
		long nps;
		if ((splitTime / 1000) < 1) {
			nps = nodesSearched;
		} else {
			Double decimalTime = new Double(nodesSearched / (splitTime / 1000D));
			nps = decimalTime.intValue();
		}
		// Send the info to the uci interface
		int mateInN = 0;
		if (eval >= Evaluator.MATE_BOUND) {
			mateInN = length;
		} else if (eval <= -Evaluator.MATE_BOUND) {
			mateInN = -length;
		}
		if (mateInN != 0) {
			return "info score mate " + mateInN + " depth " + depth + " nodes "
					+ nodesSearched + " nps " + nps + " time " + splitTime
					+ " pv " + sb.toString();
		}
		return "info score cp " + eval + " depth " + depth + " nodes "
				+ nodesSearched + " nps " + nps + " time " + splitTime + " pv "
				+ sb.toString();
	}

	
	@Override
	public EngineStats getEngineStats() {
		return stats.cumulative;
	}

	protected Continuation getContinuation(PV pv) {
		Continuation c = new Continuation();
		for (int i = 0; i < pv.length; i++) {
			c.line[i] = pv.line[i];
		}
		return c;
	}

//	protected Continuation getContinuationFromPVTable(EngineBoard board, int depth) {
//		return pvtt.getContinuation(board, depth);
//	}
	
	protected void listenerExitNode(int score) {
		if (listener != null) {
			listener.exitNode(score);
		}
		
	}

	protected void listenerEnteredNode(EngineBoard board, int alpha, int beta, boolean quiesce, int move, int flags) {
		if (listener != null) {
			if (board.getWhiteToMove()) {
				flags |= EngineListener.WHITE_TO_MOVE;
			}
			listener.enteredNode(alpha, beta, quiesce, move, flags);
		}
		
	}

	protected void listenerResult(Continuation pv) {
		if (listener != null) {
			listener.searchResult(pv);
		}
	}
	
	protected void listenerStartSearch() {
		if (listener != null) {
			listener.startSearch();
		}
	}
	
	protected void listenerTTHit(int type) {
		if (listener != null) {
			switch (type) {
			case TTable.EXACT: listener.ttableHit(EngineListener.TT_EXACT);
			break;
			case TTable.LOWERBOUND: listener.ttableHit(EngineListener.TT_LOWERBOUND);
			break;
			case TTable.UPPERBOUND: listener.ttableHit(EngineListener.TT_UPPERBOUND);
			break;
			default:
				throw new RuntimeException("Unknown type:" + type);
			}
		}
	}
	
	private void listenerFutilityPrune(int terminalEval, int mvp) {
		if (listener != null) {
			listener.futilityPrune(terminalEval, mvp);
		}
		
	}
	
	@Override
	public String name() {
		return this.name;
	}

	@Override
	public void setEvaluator(Evaluator eval) {
		this.evaluator = eval;

	}
	
	@Override
	public Evaluator getEvaluator() {
		return evaluator;
	}

	
	@Override
	public void setThinkingListener(ThinkingListener listener) {
		thinkingListener = listener;
		
	}

	@Override
	public void setListener(EngineListener listener) {
		this.listener = listener;

	}

	@Override
	public void setDefaultListener() {
		this.listener = null;

	}


	protected class LocalVars implements Poolable {
		public int alpha;
		public int eval;
		public int hashMove;
		// Temporary
		public int dummyStoredEval;
		
		int[] moves = new int[128];
		public PV pv = new PV();
		PV bestPV = new PV();
		PV currentPV = new PV();
		public int bestMove;
		
		@Override
		public void reset() {
			pv.length = bestPV.length = currentPV.length = 0;
			bestMove = hashMove = 0;
			dummyStoredEval = Integer.MIN_VALUE;
		}

		public int getMove(int i) {
			return moves[i];
		}

	}

	protected class PV extends Continuation {
		public int length;
		
		void addChild(PV childPV) {
			for (int i = length; i < length + childPV.length; i++) {
				line[i] = childPV.line[i - length];
			}
			length += childPV.length;
		}
		public void addMove(int move) {
			line[length++] = move;
			
		}
		void copy(PV other) {
			for (int i = 0; i < other.length; i++) {
				line[i] = other.line[i];
			}
			length = other.length;
		}
	}

}
