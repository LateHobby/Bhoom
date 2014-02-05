package sc.engine.engines;

import java.util.HashMap;
import java.util.Map;

import sc.bboard.PositionInfo;
import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.util.DeepEquals;
import sc.util.ObjectCloner;
import sc.util.StringDump;

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

	Map<Long, Integer> evalTestMap = new HashMap<Long, Integer>();
	Map<Long, PositionInfo> posInfoMap = new HashMap<Long, PositionInfo>();
	
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
	
	private int simpleAlphaBeta(EngineBoard board, int alpha, int beta, int depthLeft,
			LocalVars localVars) {
		minimaxNodeCount++;
		if (minimaxNodeCount > 1 && minimaxNodeCount % 10000000 == 0) {
			System.out.println("Alphabeta nodes:" + minimaxNodeCount);
		}
		int staticEval = evaluator.evaluate(board);
//		storeStaticEval(board.getZobristKey(), staticEval, board.getPositionInfo());
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

	private void storeStaticEval(long key, int staticEval, PositionInfo posInfo) {
		
		if (evalTestMap.containsKey(key)) {
			if (staticEval != evalTestMap.get(key)) {
				PositionInfo oldPosInfo = posInfoMap.get(key);
				boolean posInfoEqual = DeepEquals.deepEquals(oldPosInfo, posInfo);
				System.out.println(StringDump.dump(oldPosInfo));
				System.out.println(StringDump.dump(posInfo));
				throw new RuntimeException("Static eval mismatch: posInfoEqual=" + posInfoEqual);
			}
		}
		try {
			posInfoMap.put(key, (PositionInfo) ObjectCloner.deepCopy(posInfo));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		evalTestMap.put(key, staticEval);
		
	}

}
