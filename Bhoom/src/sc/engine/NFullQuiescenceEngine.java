package sc.engine;



/**
 * This implementation does a full quiescent search over all captures once the
 * search depth has been reached.
 * 
 * @author Shiva
 * 
 */
public class NFullQuiescenceEngine extends BaseNegamaxEngine {

//	protected EvalTable qevalTable = new EvalTable(20, 1, false);
	protected int[][] qhistoryHeuristicArray = new int[64][64];
	
	public NFullQuiescenceEngine(String name, Evaluator eval, int searchDepth) {
		super(name, eval, searchDepth);
	}

	@Override
	protected int staticEval(int entryMove, EngineBoard board,
			boolean maximizingPlayer, int alpha, int beta) {
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		return quiescence(entryMove, board, Integer.MAX_VALUE,
				maximizingPlayer, alpha, beta, inCheck);
	}

	protected int quiescence(int entryMove, EngineBoard board, int qDepthLeft,
			boolean maximizingPlayer, int alpha, int beta, boolean inCheck) {
		if (abandonSearch) { // doesn't matter what you return
			return beta;
		}
		stats.addNode(true);
		long zKey = board.getZobristKey();
		int tmove = 0;
		int tscore = EvalTable.NO_IEVAL;
		boolean positionStored = evalTable.hasPosition(zKey);
		if (positionStored) {
			tmove = evalTable.getMove(zKey);
			tscore = evalTable.getEval(zKey, alpha, beta, 0, maximizingPlayer);
			if (tscore != EvalTable.NO_IEVAL && tmove != 0) {
				stats.ttHit(true);
				return tscore;
			}
		}
		
		listener.enteredNode(alpha, beta, true,
				entryMove, 0);
		int standingPat = inCheck? alpha : evaluator.evaluate(board); 
		// side);

		if (!inCheck && qDepthLeft <= 0) {
			return standingPat;
		}

		if (standingPat >= beta) {
			listener.exitNode(standingPat);
			stats.betaCutoff(0, true);
			return standingPat;
		}

		if (alpha < standingPat) {
			alpha = standingPat;
		}

		Moves m = movesPool.allocate();
		int numMoves = board.getMoveGenerator().fillLegalCaptures(m.moves, 0);
		sortByHistoryAndSee(board, m, numMoves, qhistoryHeuristicArray);

		int swapIndex = 0;
		if (positionStored && tmove != 0) {
			swapWithIndex(tmove, swapIndex, m.moves, numMoves);
		}
		
		int score = standingPat;
		int bestScore = MIN_EVAL;
		int bestMove = 0;

		boolean betaCutoff = false;
		for (int i = 0; i < numMoves; i++) {
			int move = m.moves[i];
			board.makeMove(move, false);
			
			boolean moveCausedCheck = board.kingInCheck(board.getWhiteToMove());
			score = -quiescence(move, board, qDepthLeft - 1, !maximizingPlayer,
					-beta, -alpha, moveCausedCheck);
			board.undoLastMove();

			if (score > bestScore) {
				bestScore = score;
			} else {
			}
			if (score >= beta) {
				betaCutoff = true;
				incrementHistoryHeuristicArray(move, true, qhistoryHeuristicArray);
				evalTable.storePosition(zKey, score, alpha, beta, 0, move, maximizingPlayer);
				stats.betaCutoff(i, true);
				break;
			}
			if (score > alpha) {
				alpha = score;
				bestMove = move;
			} else {
				incrementHistoryHeuristicArray(move, false, qhistoryHeuristicArray);
			}
		}
		movesPool.release(m);

		int rv = betaCutoff ? beta : alpha;
		listener.exitNode(score);
		return rv;
	}


}
