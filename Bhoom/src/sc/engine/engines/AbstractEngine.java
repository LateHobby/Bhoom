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
import sc.engine.SearchEngine.Continuation;
import sc.util.IntStack;
import sc.util.ObjectPool;
import sc.util.ObjectPool.Factory;
import sc.util.BoardUtils;
import sc.util.Poolable;
import sc.util.PrintUtils;
import sc.util.TTable;
import sc.util.TTable.TTEntry;

abstract public class AbstractEngine implements SearchEngine {

	public static long measuredTime;
	
	// This should be final - temporarily making it non-final for testing
	public static boolean listen = false;
	
	private static final int MATE_DEPTH = 128;
	private static final int DELTAPOS_1_UPPERBOUND = 300; // for futility pruning
	private static final int DELTAPOS_2_LOWERBOUND = -300;
	private static final int NULLMOVE_REDUCTION = 2;


	public enum SearchMode {ASP_WIN, MTDF, BIN_MTDF, HYBRID_MTDF};
	
	//for debugging
	EngineListener listener;
	ThinkingListener thinkingListener;
	
	protected EvalTT evaltt;
	protected MoveSorter moveSorter;

	
	protected SearchMode mode;
	
	protected int ASP_WIN = 200; // default value
	protected Continuation[] evalByDepth = new Continuation[500];
	
	protected boolean abandonSearch;
	protected int maxDepthAllowed;

	protected TTEntry stored = new TTEntry();
	protected EStats stats = new EStats();
	protected ObjectPool<LocalVars> localVarsPool;
	protected Evaluator evaluator;
	protected String name;
	protected Timer timer = new Timer();
	protected IntStack moveStack = new IntStack(500);
	protected int timeAllowed;
	protected long thisMoveStart;
	protected boolean searchByDepthOnly;
	
	protected AbstractEngine(String name, SearchMode mode, int aspWin,  Evaluator eval, EvalTT ttable, MoveSorter sorter) {
		this(name, mode, eval, ttable, sorter);
		ASP_WIN = aspWin;
	}

	protected AbstractEngine(String name, SearchMode mode, Evaluator eval, EvalTT ttable, MoveSorter sorter) { 
		this.name = name;
		evaluator = eval;
		moveSorter = sorter;
		evaltt = ttable;
		this.mode = mode;
		
		localVarsPool = new ObjectPool<LocalVars>(
				new Factory<LocalVars>() {

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

	protected String getFlagString() {
		return String.format("%s : nullMoves:%s lmr:%s hh:%s km:%s fp:%s", name,
				fb(useNullMoves()), fb(useLateMoveReduction()), fb(useHistoryHeuristic()),
				fb(useKillerMoves()), fb(useFutilityPruning()));
	}
	
	private String fb(boolean b) {
		return b ? "True" : "False";
	}

	abstract public boolean useTTable();

	abstract public boolean useMoveSorter();
	
	abstract public boolean useNullMoves();

	abstract public boolean useLateMoveReduction();

	abstract public boolean useHistoryHeuristic();

	abstract public boolean useKillerMoves();

	abstract public boolean useFutilityPruning();

	@Override
	public void newGame() {
		// reset engine's stored values
		evaltt.reset();
		moveSorter.reset();
		evaluator.reset();
	}

	@Override
	public Continuation searchByDepth(EngineBoard board, int searchDepth) {
		moveStack.clear();
		maxDepthAllowed = searchDepth;
		searchByDepthOnly = true;
		abandonSearch = false;
		return search(board);
	}

	@Override
	public Continuation searchByTime(EngineBoard board, long msTime) {
		moveStack.clear();
		searchByDepthOnly = false;
		abandonSearch = false;
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
			int engineInc, int moveTime, int oppTime, int oppInc) {
		System.out.printf("Search: depth=%d engineTime=%d, moveTime=%d\n", depth, engineTime, moveTime);
		abandonSearch = false;
		timeAllowed = 0;
		if (moveTime > 0) {
			timeAllowed = moveTime;
		} else if (engineTime > 0) {
			timeAllowed = calculateTimeAllowed(board, engineTime, engineInc, oppTime, oppInc);
		}
		maxDepthAllowed = (depth <= 0) ? 1000 : depth;
		thisMoveStart = System.currentTimeMillis();
		System.out.printf("MaxDepthAllowed:%d TimeForMove:%d\n", maxDepthAllowed, timeAllowed);
		traceStartSearch();
		if (timeAllowed > 0) {
			return searchByTime(board, timeAllowed);
		} else {
			return searchByDepth(board, maxDepthAllowed);
		}
		
	}

	protected int calculateTimeAllowed(EngineBoard board, int engineTime,
			int engineInc, int oppTime, int oppInc) {
		int idealTimeAllowed = (engineTime + (40 -1) * engineInc)/50;
		return idealTimeAllowed;
//		int movesMade = board.getFullMoveNumber();
//		int movesLeft = movesMade < 35 ? 50 - movesMade : 
//			movesMade < 50 ? 70 - movesMade : 90 - movesMade;
//		int proposedTime = (engineTime + (movesLeft-1) * engineInc)/movesLeft;
//		
//		boolean lagging = board.getWhiteToMove() ? engineTime <= oppTime+200 : 
//			engineTime - proposedTime <= oppTime + 200;
//		if (lagging) {
//			proposedTime = (proposedTime * 2)/3;
//		}
//		return Math.max(proposedTime, 200); // return at least 1/5 sec
	}


	protected int alphaBeta(EngineBoard board, int alpha, int beta,
			int depthLeft, int ply, LocalVars localVars) {
		if (abandonSearch) {
			traceAbandonSearch();
			return beta; // Doesn't matter what is returned, so return cutoff
		}
		boolean quiesce = (depthLeft <= 0);
		stats.addNode(quiesce);
		localVars.alpha = alpha;
		
		if (retrieveEvalCutoff(board, alpha, beta, depthLeft, localVars)) {
			stats.ttHit(quiesce);
			return localVars.eval;
		}
		
		int staticEval = evaluator.evaluate(board);
		traceStaticEval(staticEval);
		if (isDraw(board)) {
			return -contemptFactor(board, staticEval);
		}
		
		alpha = localVars.alpha;
		
		if (ply > 0 && noMoveEvalCutoff(board, alpha, beta, staticEval, depthLeft, ply, quiesce, localVars)) {
//			store(board, alpha, beta, localVars.eval, 0, depthLeft);
			stats.betaCutoff(0, quiesce);
			return localVars.eval;
		}
		alpha = localVars.alpha;

		if (isGeneralLevel2FutilityPruned(board, alpha, beta, staticEval, depthLeft)) {
			localVars.eval = beta;
			return beta;
		}
		
		if (isGeneralLevel1FutilityPruned(board, alpha, beta, staticEval, depthLeft)) {
			localVars.eval = alpha;
			return alpha;
		}
		int score;
		int bestScore = Evaluator.MIN_EVAL;
		int bestMove = 0;
		boolean pvFound = false;
		int newDepthLeft = (depthLeft <= 0) ? 0 : depthLeft - 1;
		
		int numMoves = generateMoves(board,  quiesce, localVars);
		if (numMoves == 0) {
			return terminalEval(board, quiesce, staticEval, depthLeft);
		}

		

		if (useMoveSorter()) {
//			long start = System.currentTimeMillis();
			moveSorter.sortMoves(board, ply, localVars.hashMove, localVars.moves, numMoves);
//			measuredTime += (System.currentTimeMillis() - start);
		}


		for (int i = 0; i < numMoves; i++) {
			int move = localVars.getMove(i);
			if (isMoveLevel2FutilityPruned(board, move, alpha, beta, staticEval, depthLeft)) {
				bestScore = beta;
				break;
			}
			if (isMoveLevel1FutilityPruned(board, move, alpha, beta, staticEval, depthLeft)) {
				bestScore = Math.max(bestScore, alpha);
				break;
			}
			makeMove(board, move);
			LocalVars childLocalVars = localVarsPool.allocate();
			if (pvFound && useLateMoveReduction() &&
					lateMoveReductionEval(board, alpha, beta, newDepthLeft, ply,
							quiesce, move, childLocalVars)) {
				score = childLocalVars.eval;
			} else {
				traceEnteredNode(board, alpha, beta, newDepthLeft, ply+1, move, EngineListener.NORMAL);
				score = -alphaBeta(board, -beta, -alpha, newDepthLeft, ply+1, 
						 childLocalVars);
				traceExitNode(score);
//				localVars.currentPV.copy(lv.bestNodePV);
			}
			undoLastMove(board, move);
			
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				localVars.bestChildPV.copy(childLocalVars.bestNodePV);
			} else {
				incrementHistoryHeuristicArray(move, false);
			}
			localVarsPool.release(childLocalVars);
			
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
		boolean standPatIsBest = quiesce && staticEval > bestScore && !board.kingInCheck(board.getWhiteToMove());
		if (!standPatIsBest) {
			store(board, alpha, beta, bestScore, bestMove, depthLeft);
			localVars.bestNodePV.addMove(bestMove);
			localVars.bestNodePV.addChild(localVars.bestChildPV);
			localVars.bestMove = bestMove;
		}
		return standPatIsBest ? staticEval : bestScore;

	}

	// For the null move we are interested in seeing whether the null move
	// causes a beta cutoff. This method returns false if this cannot happen
	// using futility arguments.
	protected boolean isNullMoveLevel1FutilityPruned(EngineBoard board, 
			int alpha, int beta, int staticEval, int depthLeft) {
		if (useFutilityPruning()) {
			if (depthLeft <= 1) {
				// Condition : beta1 > static1
				if (beta > staticEval) {
					traceFutilityPrune(staticEval, 0);
					return true;
				}
			}
		}
		return false;
	}
	
	// Fail low if depth<=1 and the static eval is so low that this move's
	// outcome cannot raise the eval in qsearch above alpha
	protected boolean isMoveLevel1FutilityPruned(EngineBoard board, int move,
			int alpha, int beta, int staticEval, int depthLeft) {
		if (useFutilityPruning()) {
			if (depthLeft <= 1) {
				// Condition : alpha1 > static1 + v1 + U(deltaPos1)
				int futilityGap = alpha - staticEval - DELTAPOS_1_UPPERBOUND;
				if (futilityGap > 0) { 
					int materialGain = materialGain(board, move);
					if (materialGain < futilityGap) {
						traceFutilityPrune(staticEval, materialGain);
						return true;
					}
				}
			}
		}
		return false;
	}

	

	// Fail low if depth<=1 and the static eval is so low that not even the best
	// outcome can raise the eval in qsearch above alpha
	protected boolean isGeneralLevel1FutilityPruned(EngineBoard board, int alpha, int beta,
			int staticEval, int depthLeft) {
		if (false && useFutilityPruning()) {
			if (depthLeft == 1) {
				// Condition: alpha1 > static1 + max attacked opponent piece + U(deltaPos1)
				int futilityGap = alpha - staticEval - DELTAPOS_1_UPPERBOUND;
				if (futilityGap > 0) { 
					boolean white = board.getWhiteToMove();
					int highestOppPieceValue = highestAttackedPieceValue(board, !white);
					if (highestOppPieceValue < futilityGap) {
						traceFutilityPrune(staticEval, highestOppPieceValue);
						return true;
					}
				}
			}
		}
		return false;
	}

	// Fail high if depth<=2 and the static eval is so high that this move's
	// outcome cannot lower the eval below beta
	protected boolean isMoveLevel2FutilityPruned(EngineBoard board, int move,
			int alpha, int beta, int staticEval, int depthLeft) {
		if (useFutilityPruning() && !board.kingInCheck(board.getWhiteToMove())) {
			if (depthLeft <= 2) {
				int maxOwnPieceValue = highestPieceValue(board, board.getWhiteToMove());
				// Condition : beta2 < static2 + v2 + L(deltaPos2) - max self piece  - U(deltaPos1)
				int futilityGap = beta - staticEval - DELTAPOS_2_LOWERBOUND + maxOwnPieceValue + DELTAPOS_1_UPPERBOUND;
				if (futilityGap < 0) { 
					return true;
				} else {
					int materialGain = materialGain(board, move);
					if (materialGain > futilityGap) {
						traceFutilityPrune(staticEval, materialGain);
						return true;
					}
				}
			}
		}
		return false;
	}

	// Fail high if depth<=2 and the static eval is so high that no
	// sequence of two moves can lower the eval below beta
	protected boolean isGeneralLevel2FutilityPruned(EngineBoard board, int alpha, int beta,
			int staticEval, int depthLeft) {
		if (false && useFutilityPruning()) {
			if (depthLeft == 2) {
				int maxOwnPieceValue = highestPieceValue(board, board.getWhiteToMove());
				// Condition: beta2 < static2 + 0 + L(deltaPos2) - max own piece - U(deltaPos1)
				if (beta < staticEval + DELTAPOS_2_LOWERBOUND - maxOwnPieceValue - DELTAPOS_1_UPPERBOUND) {
					return true;
				}
			}
		}
		return false;
	}

	private int materialGain(EngineBoard board, int move) {
		int captureValue = pieceCapturedValue(board, move);
		int promotionGain = promotionMaterialGain(board, move);
		int materialGain = captureValue + promotionGain;
		return materialGain;
	}


	private int promotionMaterialGain(EngineBoard board, int move) {
		byte pieceToPromoteTo = Encodings.getPieceToPromoteTo(move);
		int promotionGain = 0;
		if (pieceToPromoteTo != Encodings.EMPTY) {
			// promotion gain = new piece - pawn
			promotionGain = evaluator.pieceWeight(pieceToPromoteTo) - evaluator.pieceWeight(Encodings.WPAWN);
		}
		return promotionGain;
	}

	
	private int pieceCapturedValue(EngineBoard board, int move) {

		if (Encodings.isEnpassantCapture(move)) {
			return evaluator.pieceWeight(Encodings.WPAWN);
		} else {
			byte piece =  board.getPiece(Encodings.getToSquare(move));
			return piece == Encodings.EMPTY ? 0 : evaluator.pieceWeight(piece);
		}
	}

	private int highestAttackedPieceValue(EngineBoard board, boolean white) {
		PositionInfo pinfo = board.getPositionInfo();
		OneSidePositionInfo osp = white ? pinfo.wConfig : pinfo.bConfig;
		byte mvp = osp.most_valuable_attacked_piece;
		return evaluator.pieceWeight(mvp);
	}

	private int highestPieceValue(EngineBoard board, boolean white) {
		PositionInfo pinfo = board.getPositionInfo();
		OneSidePositionInfo osp = white ? pinfo.wConfig : pinfo.bConfig;
		byte mvp = osp.most_valuable_piece;
		return evaluator.pieceWeight(mvp);
	}

	protected void addToKillerMoves(EngineBoard board, int ply, int move,
			int hashMove) {
		if (useKillerMoves() && useMoveSorter()) {
			moveSorter.addToKillerMoves(board, ply, move, hashMove);
		}
		
	}

	protected void incrementHistoryHeuristicArray(int move, boolean increment) {
		if (useHistoryHeuristic() && useMoveSorter()) {
			moveSorter.incrementHistoryHeuristicArray(move, increment);
		}
	}

	protected int generateMoves(EngineBoard board,  boolean quiesce,
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
	 * @param  
	 * @param quiesce
	 * @param localVars
	 * @return
	 */
	protected boolean noMoveEvalCutoff(EngineBoard board, int alpha, int beta,
			int staticEval, int depthLeft, int ply, boolean quiesce, LocalVars localVars) {
		localVars.alpha = alpha;
		if (quiesce) {
			if (board.kingInCheck(board.getWhiteToMove())) {
				return false;
			}
			localVars.eval = staticEval;
			if (localVars.eval > alpha) {
				localVars.alpha = localVars.eval;
			}
			
			return localVars.eval >= beta;
		} else {
			if (useNullMoves()) {
				return nullMoveEvalCutoff(board, alpha, beta, staticEval, 
						depthLeft, ply, localVars);
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
	protected int terminalEval(EngineBoard board, boolean quiesce, int staticEval, int depthLeft) {
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		int eval;
		if (quiesce) {
			eval = staticEval;
			// fake alpha and beta bracketing value to simulate TType.EXACT
			store(board, eval - 1, eval + 1, eval, 0, depthLeft);
		} else {
			if (inCheck) { // checkmate
				eval = -Evaluator.MATE_BOUND;
			} else { // stalemate
				eval = -contemptFactor(board, staticEval);
			}
		}
		
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
	protected boolean retrieveEvalCutoff(EngineBoard board, int alpha, int beta,
			int depthLeft, LocalVars lv) {
		lv.alpha = alpha;
		lv.hashMove = 0;
		if (useTTable()) {

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
						lv.bestNodePV.length = 1;
						lv.bestNodePV.line[0] = lv.bestMove;
						traceRetrieve(stored);

					}
				}
			}
			return rv;
		}
		return false;
	}

	protected void store(EngineBoard board, int alpha, int beta, int eval,
			int move, int depthLeft) {
		if (useTTable()) {
			if (move == 0) {
//			System.out.println("Not storing zero move");
				return;
			}
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
			traceStore(stored);
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
		
		traceEnteredNode(board, beta-1, beta, newDepthLeft-1, ply+1, move, EngineListener.LMR_ZW);
		// Zero width search with reduced depth
		int score = -alphaBeta(board, -beta, -(beta-1), newDepthLeft-1, ply+1, 
				 lv);
		traceExitNode(score);
		
		if (score > alpha) { // do full search
			localVarsPool.release(lv);
			lv = localVarsPool.allocate();
			traceEnteredNode(board, alpha, beta, newDepthLeft, ply+1, move, EngineListener.LMR_FULL);
			score = -alphaBeta(board, -beta, -alpha, newDepthLeft, ply+1, 
					 lv);
			traceExitNode(score);
		} else {
//			System.out.println("lmr success");
		}
		localVars.bestNodePV.copy(lv.bestNodePV);
		localVars.bestMove = lv.bestMove;
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
	 * @param ply2 
	 * @param localVars
	 * @return
	 */
	protected boolean nullMoveEvalCutoff(EngineBoard board, int alpha,
			int beta, int terminalEval, int depthLeft, int ply, LocalVars localVars) {
		if (lastMoveWasNull()) { // don't allow the same side to make two consecutive null moves
			return false;
		}
		if (board.kingInCheck(board.getWhiteToMove())) {
			return false;
		}
		if (depthLeft < NULLMOVE_REDUCTION) {
			return false;
		}
		if (isNullMoveLevel1FutilityPruned(board, alpha, beta, terminalEval, depthLeft-NULLMOVE_REDUCTION)) {
			return false;
		}
		stats.tryNull();
		makeMove(board, 0);
		LocalVars lv = localVarsPool.allocate();
		
		traceEnteredNode(board, beta-1, beta, depthLeft-NULLMOVE_REDUCTION, ply+1, 0, EngineListener.NULLMOVE_ZW);
		int score = -alphaBeta(board, -beta, -beta+1, depthLeft-NULLMOVE_REDUCTION, ply+1,  lv);
		traceExitNode(score);
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
	
	protected boolean isDraw(EngineBoard board) {
		if (board.getHalfMoveClock() >= 50) {
			return true;
		}
		if (board.drawByRepetition()) {
			return true;
		}
		if (board.drawByInsufficientMaterial()) {
			return true;
		}
		return false;
	}
	
	protected int contemptFactor(EngineBoard board, int terminalEval) {
		if (board.getFullMoveNumber() < 30) {
			return 50;
		} else if (board.getFullMoveNumber() < 50) {
			return 25;
		} else if (terminalEval > 200) {
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

	
	/**
	 * Returns the best continuation found. Respects both the maxDepthAllowed
	 * and the abandonSearch variables.
	 * 
	 * @param board
	 * @return
	 */
	protected Continuation search(EngineBoard board) {
		stats.cumulative.reset();
		System.out.println(name);
		int currentSearchDepth = 1;
		int maxDepthSearched = 0;
		do {

			stats.perIter.reset();
			measuredTime= 0;
			long startTime = System.currentTimeMillis();
			traceStartSearch();
			int lastEval = maxDepthSearched == 0 ? Evaluator.MIN_EVAL : evalByDepth[maxDepthSearched].eval;
			Continuation e2 = searchBoard(board, currentSearchDepth, lastEval);
			traceResult(e2);
			if (!searchByDepthOnly) {
				if (abandonSearch) {
					break;
				}
				long currentTime = System.currentTimeMillis();
				long iterTime = currentTime - startTime;
				long timeUsed = currentTime - thisMoveStart;
				long timeLeft = timeAllowed - timeUsed;
				if (timeLeft < iterTime/2) {
					break;
				}
			}
			evalByDepth[currentSearchDepth] = e2;
			String thinking = getThinking(startTime, e2, currentSearchDepth);
			System.out.println(thinking);
			System.out.println("MeasuredTime:" + measuredTime);
			if (thinkingListener != null) {
				thinkingListener.thinkingUpdate(thinking);
			}
			System.out.println(stats.perIter.getStatsString());
			maxDepthSearched = currentSearchDepth;
			currentSearchDepth++;
		
		} while (currentSearchDepth <= maxDepthAllowed);
		
		stats.depth(maxDepthSearched);
		return evalByDepth[maxDepthSearched];
	}
	
	protected Continuation searchBoard(EngineBoard board, int depth, int lastEval) {
		switch (mode) {
		case ASP_WIN: return aspWinSearch(board, depth, lastEval);
		case MTDF: 
		case BIN_MTDF:
		case HYBRID_MTDF:
			return mtdSearch(board, depth, lastEval, mode);
		default:
			throw new RuntimeException("Unknown mode:" + mode);
		}
	}
	protected Continuation mtdSearch(EngineBoard board, int d, int lastEval, SearchMode mode) {
		Continuation e2 = null;
		int g = lastEval;
		int lbound = Evaluator.MIN_EVAL;
		int ubound = Evaluator.MAX_EVAL;
		int iterationCount = 0;
		do {
//			System.out.printf("Init: lbound=%d ubound=%d\n", lbound, ubound);
			traceStartSearch();
			int beta = getNextBeta(g, lbound, ubound, iterationCount, mode);
			
			LocalVars lv = localVarsPool.allocate();
			traceEnteredNode(board, beta-1, beta, d, 0, 0, EngineListener.NORMAL);
			g = alphaBeta(board, beta-1, beta, d, 0, lv);
			traceExitNode(g);
			if (abandonSearch) {
				break;
			}
			e2 = getContinuation(lv.bestNodePV);
			e2.eval = g;
			
			traceResult(e2);
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
			SearchMode mode) {
		switch (mode) {
		case MTDF: return (g == lbound) ? g+1 : g;
		case BIN_MTDF: return (ubound - lbound > 5) ? 
				(lbound + ubound)/2 : (g == lbound) ? g+1 : g;
		case HYBRID_MTDF: return (ubound - lbound > 5 && 
				(iterationCount > 0 && (iterationCount % 5) == 0)) ? 
						(lbound + ubound)/2 : (g == lbound) ? g+1 : g;
			default: throw new RuntimeException("Unknown mode:" + mode);
		}
	}
	protected Continuation aspWinSearch(EngineBoard board, int depth, int lastEval) {
		int alpha = Evaluator.MIN_EVAL;
		int beta = Evaluator.MAX_EVAL;
		if (lastEval != Evaluator.MIN_EVAL) {
			alpha = lastEval - ASP_WIN;
			beta = lastEval + ASP_WIN;
		}
		LocalVars localVars = localVarsPool.allocate();
		traceEnteredNode(board, alpha, beta, depth, 0, 0, EngineListener.NORMAL);
		int eval = alphaBeta(board, alpha, beta, depth, 0,
				localVars);
		traceExitNode(eval);
		Continuation e2 = getContinuation(localVars.bestNodePV);
		e2.eval = eval;
		localVarsPool.release(localVars);
		
		if ((alpha > Evaluator.MIN_EVAL && eval <= alpha)
				|| (beta < Evaluator.MAX_EVAL && eval >= beta)) {
			System.out.println("Repeating depth " + depth);
			alpha = Evaluator.MIN_EVAL;
			beta = Evaluator.MAX_EVAL;
			localVars = localVarsPool.allocate();
			traceEnteredNode(board, alpha, beta, depth, 0, 0, EngineListener.NORMAL);
			eval = alphaBeta(board, alpha, beta, depth, 0,
					localVars);
			traceExitNode(eval);
			e2 = getContinuation(localVars.bestNodePV);
			e2.eval = eval;
			localVarsPool.release(localVars);
		}
		
		return e2;
	}
	
	protected void traceStaticEval(int staticEval) {
		if (listen) {
			listener.staticEval(staticEval);
		}
	}

	protected void traceExitNode(int score) {
		if (listen) {
			listener.exitNode(score);
		}
		
	}

	protected void traceEnteredNode(EngineBoard board, int alpha, int beta, int depthLeft, int ply, int move, int flags) {
		if (listen) {
			if (board.getWhiteToMove()) {
				flags |= EngineListener.WHITE_TO_MOVE;
			}
			listener.enteredNode(alpha, beta, depthLeft, ply, move, flags);
		}
		
	}

	protected void traceResult(Continuation pv) {
		if (listen) {
			listener.searchResult(pv);
		}
	}
	
	protected void traceStartSearch() {
		if (listen) {
			listener.startSearch();
		}
		
	}
	
	protected void traceStartSearch(int type) {
		if (listen) {
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
	
	protected void traceFutilityPrune(int terminalEval, int mvp) {
		if (listen) {
			listener.futilityPrune(terminalEval, mvp);
		}
		
	}
	
	protected void traceAbandonSearch() {
		if (listen) {
			listener.abandonSearch();
		}
		
	}

	protected void traceStore(TTEntry stored) {
		if (listen) {
			listener.store(stored);
		}
		
	}

	private void traceRetrieve(TTEntry stored) {
		if (listen) {
			listener.retrieve(stored);
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
		// Only used by minimax search
		public int minimaxNodeCount;
		
		int[] moves = new int[128];
		public PV bestNodePV = new PV();
		PV bestChildPV = new PV();
//		PV currentPV = new PV();
		public int bestMove;
		public MoveHandler moveHandler = new MoveHandler();
		
		@Override
		public void reset() {
			bestNodePV.length = bestChildPV.length = 0;
			bestMove = hashMove = 0;
			minimaxNodeCount = 0;
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
