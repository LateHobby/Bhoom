package sc.engine.engines;

import java.util.HashMap;
import java.util.Map;

import sc.bboard.PositionInfo;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.engines.AbstractEngine.LocalVars;

public class CTestEngine extends AbstractEngine {

	// switches
	private  boolean useTTable= true;
	private  boolean useMoveSorter= true;
	
	private  boolean useNullMoves= true;
	private  boolean useLateMoveReduction= true;
	private  boolean useHistoryHeuristic= true;
	private  boolean useKillerMoves= true;
	private  boolean useFutilityPruning= true;
	
	
	
	public CTestEngine(String name, SearchMode mode, int aspWin, Evaluator eval, EvalTT ttable,
			MoveSorter sorter) {
		super(name, mode, aspWin, eval, ttable, sorter);
	}
	
	public CTestEngine(String name, SearchMode mode, Evaluator eval, EvalTT ttable,
			MoveSorter sorter) {
		super(name, mode, eval, ttable, sorter);
	}

	public void setFlags(boolean useTTable, boolean useMoveSorter, boolean useNullMoves,
			boolean useLateMoveReduction, boolean useHistoryHeuristic,
			boolean useKillerMoves, boolean useFutilityPruning) {
		this.useTTable = useTTable && evaltt != null;
		this.useMoveSorter = useMoveSorter && moveSorter != null;
		this.useNullMoves = useNullMoves;
		this.useLateMoveReduction = useLateMoveReduction;
		this.useHistoryHeuristic = useHistoryHeuristic;
		this.useKillerMoves = useKillerMoves;
		this.useFutilityPruning = useFutilityPruning;
		System.out.println(getFlagString());
	}

	public EngineParameters getEngineParameters() {
		return null;
	}
	
	public void setParameters(double[] values) {
		
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
		
		int numMoves = generateMoves(board,  quiesce, localVars.hashMove, ply, localVars);
		if (numMoves == 0) {
			return terminalEval(board, quiesce, staticEval, depthLeft);
		}

		

//		if (useMoveSorter()) {
////			long start = System.currentTimeMillis();
//			moveSorter.sortMoves(board, ply, localVars.hashMove, localVars.moves, numMoves);
////			measuredTime += (System.currentTimeMillis() - start);
//		}

		int i = 0;
		while (localVars.moveHandler.hasMoreMoves()) {
			int move = localVars.moveHandler.nextMove();
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
			i++;
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

	

	protected int[][] historyHeuristicArray = new int[64][64];
	protected int[][] killerMoves = new int[200][4];
	
	@Override
	public void incrementHistoryHeuristicArray(int move, boolean increment) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		if (increment) {
			historyHeuristicArray[from][to]++;
		} else {
			if (historyHeuristicArray[from][to] > 0) {
				historyHeuristicArray[from][to]--;
			}
		}
	}

	@Override
	public void addToKillerMoves(EngineBoard board, int ply, int move, int hashMove) {
		if (move == hashMove || isCapture(board, move)) { // don't add captures or the hash move
			return;
		}
		for (int i = killerMoves[ply].length - 2; i >= 0; i--) {
		    killerMoves[ply][i + 1] = killerMoves[ply][i];
		}
		killerMoves[ply][0] = move;
	}
	
	protected int generateMoves(EngineBoard board,  boolean quiesce, int hashMove, int ply, 
			LocalVars localVars) {
		
		return localVars.moveHandler.generateMoves(board, quiesce, hashMove, ply, historyHeuristicArray, killerMoves);
		
		
	}
	
	protected boolean isCapture(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return true;
		} else {
			return board.getPiece(Encodings.getToSquare(move)) != Encodings.EMPTY;
		}
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

	private int minimaxNodeCount;
	protected Continuation minimaxSearch(EngineBoard board, int depth, int dummy) {
		LocalVars localVars = localVarsPool.allocate();
		minimaxNodeCount = 0;
		traceEnteredNode(board, Evaluator.MIN_EVAL, Evaluator.MAX_EVAL, depth, 0, 0, 0);
		int eval = minimax(board, depth, localVars);
		traceExitNode(eval);
		Continuation c = getContinuation(localVars.bestNodePV);
		c.eval = eval;
		localVarsPool.release(localVars);
		System.out.println("MiniMaxNodes=" + minimaxNodeCount);
		return c;
		
	}
	
	private int minimax(EngineBoard board, int depthLeft,
			LocalVars localVars) {
		minimaxNodeCount++;
		if (minimaxNodeCount > 1 && minimaxNodeCount % 10000000 == 0) {
			System.out.println("Minimax nodes:" + minimaxNodeCount);
		}
		int staticEval = evaluator.evaluate(board);
//		storeStaticEval(board.getZobristKey(), staticEval, board.getPositionInfo());
		
		traceStaticEval(staticEval);
		if (isDraw(board)) {
			return -contemptFactor(board, staticEval);
		}
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		boolean quiesce = (depthLeft == 0);
		int numMoves = generateMoves(board, quiesce, localVars);
		if (numMoves == 0) {
			return terminalEval(board, quiesce, staticEval, depthLeft);
		}
		
		int score;
		int bestScore = Evaluator.MIN_EVAL;
		int bestMove = 0;
		int newDepthLeft = (depthLeft <= 0) ? 0 : depthLeft - 1;
		for (int i = 0; i < numMoves; i++) {
			int move = localVars.getMove(i);
			makeMove(board, move);
			
			LocalVars childLocalVars = localVarsPool.allocate();
			traceEnteredNode(board, Evaluator.MIN_EVAL, Evaluator.MAX_EVAL, newDepthLeft, 0, move, 0);
			score = -minimax(board,  newDepthLeft,  childLocalVars);
			traceExitNode(score);
			
			undoLastMove(board, move);
			
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				localVars.bestChildPV.copy(childLocalVars.bestNodePV);
			}
			localVarsPool.release(childLocalVars);
		}
		boolean standPatIsBest = quiesce && staticEval > bestScore && !inCheck;
		if (!standPatIsBest) {
			localVars.bestNodePV.addMove(bestMove);
			localVars.bestNodePV.addChild(localVars.bestChildPV);
			localVars.bestMove = bestMove;
		}
		return standPatIsBest ? staticEval : bestScore;
	}


	protected Continuation simpleAlphaBeta(EngineBoard board, int depth, int dummy) {
		LocalVars localVars = localVarsPool.allocate();
		minimaxNodeCount = 0;
		int alpha = Evaluator.MIN_EVAL;
		int beta = Evaluator.MAX_EVAL;
		traceEnteredNode(board, alpha, beta, depth, 0, 0, 0);
		int eval = simpleAlphaBeta(board, alpha, beta, depth, localVars);
		traceExitNode(eval);
		
		Continuation c = getContinuation(localVars.bestNodePV);
		c.eval = eval;
		localVarsPool.release(localVars);
		System.out.println("SimpleAlphaBetaNodes=" + minimaxNodeCount);
		return c;
		
	}
	
	protected int simpleAlphaBeta(EngineBoard board, int alpha, int beta, int depthLeft,
			LocalVars localVars) {
		minimaxNodeCount++;
		if (minimaxNodeCount > 1 && minimaxNodeCount % 10000000 == 0) {
			System.out.println("Alphabeta nodes:" + minimaxNodeCount);
		}
		int staticEval = getStaticEval(board);

		traceStaticEval(staticEval);
		if (isDraw(board)) {
			return -contemptFactor(board, staticEval);
		}
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		boolean quiesce = (depthLeft == 0);
		if (quiesce && !inCheck) { // stand-pat not allowed when in check
			if (staticEval >= beta) {
				return staticEval;
			}
			if (staticEval > alpha) {
				alpha = staticEval;
			}
		}
		int numMoves = generateMoves(board, quiesce, localVars);
		if (numMoves == 0) {
			return terminalEval(board, quiesce, staticEval, depthLeft);
		}
		
		int score;
		int bestScore = Evaluator.MIN_EVAL;
		int bestMove = 0;
		int newDepthLeft = (depthLeft <= 0) ? 0 : depthLeft - 1;

		for (int i = 0; i < numMoves; i++) {
			int move = localVars.getMove(i);
			makeMove(board, move);
			LocalVars childLocalVars = localVarsPool.allocate();
			traceEnteredNode(board, -beta, -alpha, newDepthLeft, 0, move, 0);
			score = -simpleAlphaBeta(board,  -beta, -alpha, newDepthLeft,  childLocalVars);
			traceExitNode(score);
			
			undoLastMove(board, move);
			
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				localVars.bestChildPV.copy(childLocalVars.bestNodePV);
			}
			localVarsPool.release(childLocalVars);
			if (score >= beta) {
				break;
			}
			if (score > alpha) {
				alpha = score;
			}
		}
		boolean standPatIsBest = quiesce && staticEval > bestScore && !inCheck;
		if (!standPatIsBest) {
			localVars.bestNodePV.addMove(bestMove);
			localVars.bestNodePV.addChild(localVars.bestChildPV);
			localVars.bestMove = bestMove;
		}
		return standPatIsBest? staticEval : bestScore;
	}

	// This method is overridden in subclasses for debugging
	protected int getStaticEval(EngineBoard board) {
		return evaluator.evaluate(board);
	}


}
