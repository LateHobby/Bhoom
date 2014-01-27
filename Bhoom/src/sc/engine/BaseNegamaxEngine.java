package sc.engine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sc.encodings.Encodings;
import sc.util.ObjectPool;
import sc.util.ObjectPool.Factory;
import sc.util.Poolable;
import sc.util.PrintUtils;

/**
 * This class is the base class for variations on search implementations. This
 * implementation returns the static eval after the search depth has been
 * reached. To change the implementation, substitute a different routine in the
 * staticEval() function.
 * 
 * @author Shiva
 * 
 */
public class BaseNegamaxEngine implements SearchEngine {

	static public int MAX_EVAL = Short.MAX_VALUE;
	static public int MIN_EVAL = Short.MIN_VALUE;
	static public int MATE_BOUND = MAX_EVAL - 2;
	
	static private int ASP_WIN_SIZE = 50;

	protected EngineListener defaultListener = new DummyEngineListener();
	protected EngineListener listener = defaultListener;
//	private Stats stats;
//	protected Stats qstats;
	protected EStats stats = new EStats();
	protected String name;
	protected Evaluator evaluator;

	protected ObjectPool<Moves> movesPool;
	protected int searchDepth;
	protected EvalTable evalTable = new EvalTable(20, 2, false);
	private int[][] historyHeuristicArray = new int[64][64];
	protected int[][] killerMovesByDepth = new int[4][100];

	protected SeeOld see = new SeeOld();
	
	protected boolean abandonSearch = false;
	private int maxDepthSearched;
	
	private Continuation[] bestEvalsByDepth = new Continuation[100];
	
	
	public BaseNegamaxEngine(String name, Evaluator eval, int searchDepth) {
		this.name = name;
		this.evaluator = eval;
		this.searchDepth = searchDepth;

		
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {
				historyHeuristicArray[i][j] = 0;
			}
		}
		
		movesPool = new ObjectPool<Moves>(new Factory<Moves>() {

			@Override
			public Moves create() {
				return new Moves();
			}

			@Override
			public Moves[] getArray(int size) {
				return new Moves[size];
			}

		}, 256, name + ":MovesPool");
//		pvNodePool = new ObjectPool<PVNode>(new Factory<PVNode>() {
//
//			@Override
//			public PVNode create() {
//				return new PVNode();
//			}
//
//			@Override
//			public PVNode[] getArray(int size) {
//				return new PVNode[size];
//			}
//
//		}, 256, name + ":PVNodePool");
		
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void setEvaluator(Evaluator eval) {
		this.evaluator = eval;
	}

	@Override
	public void setListener(EngineListener listener) {
		this.listener = listener;
	}

	@Override
	public void setDefaultListener() {
		this.listener = defaultListener;
	}

	@Override
	public Continuation search(EngineBoard board, int depth, int engineTime,
			int engineInc, int movetime) {
		if (depth > 0) {
			return searchByDepth(board, depth);
		}
		if (movetime > 0) {
			return searchByTime(board, movetime);
		}
		if (engineTime > 0) {
			int lim = 50;
			int moveNum = board.getFullMoveNumber();
			if (moveNum > 50) {
				lim = 75;
			}
			long timeForMove = (engineTime + (40 - 1) * engineInc) / 40;
			return searchByTime(board, timeForMove);
		}
		return null;
	}
	
	@Override
	public Continuation searchByDepth(EngineBoard board, int searchDepth) {
		this.searchDepth = searchDepth;
		System.out.println("Starting search with depth=" + searchDepth);
		preSearchPrep();
		Continuation e2 = searchWithIterativeDeepening(board);
		postMoveOutput(e2);
		return e2;
	}

	@Override
	public Continuation searchByTime(EngineBoard board, long msTime) {
		this.searchDepth = 100;   // essentially, infinity
		System.out.println("Starting search with time=" + msTime);
		preSearchPrep();
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			@Override
			public void run() {
				abandonSearch = true;
				
			}
		};
		
		ses.schedule(command, msTime, TimeUnit.MILLISECONDS);

		Continuation e2 = searchWithIterativeDeepening(board);
		postMoveOutput(e2);
		return e2;
	}

	private void preSearchPrep() {
		stats.cumulative.reset();
		maxDepthSearched = 0;
		abandonSearch = false;
	}
	
//	@Override
//	public int getBestMove(EngineBoard board) {
//		preSearchPrep();
//		Continuation e2 = searchWithIterativeDeepening(board);
//		postMoveOutput(e2);
//		return e2.line[0];
//	}

	private void postMoveOutput(Continuation e2) {
		PrintUtils.printMoves(e2.line, 10);
		System.out.println(stats.perIter.getStatsString());
		evalTable.printStats();
		System.out.println("Move=" + PrintUtils.notation(e2.line[0]));
	}


	private Continuation searchNoIterativeDeepening(EngineBoard board) {
		int alpha = MIN_EVAL;
		int beta = MAX_EVAL;
		int eval = alphaBeta(0, board, alpha, beta, true, searchDepth, 0);
		Continuation e2 = getLineFromEvalTable(board, searchDepth);
		return e2;
	}

	private Continuation searchWithIterativeDeepening(EngineBoard board) {
		int currentSearchDepth = 1;
		int alpha = MIN_EVAL;
		int beta = MAX_EVAL;
		while (currentSearchDepth <= searchDepth) {
			
			stats.perIter.reset();
			listener.startSearch();
			long startTime = System.currentTimeMillis();
			listener.enteredNode(alpha, beta, false, 0, EngineListener.NORMAL);
			int eval = alphaBeta(0, board, alpha, beta, true,
					currentSearchDepth, 0);
			listener.exitNode(eval);
			Continuation e2 = getLineFromEvalTable(board, currentSearchDepth);
			listener.searchResult(e2);
			if (abandonSearch) {
				maxDepthSearched = currentSearchDepth - 1;
				break;
			}
			bestEvalsByDepth[currentSearchDepth] = e2;
			if ((alpha > MIN_EVAL && eval <= alpha) || 
					(beta < MAX_EVAL && eval >= beta)) {
				alpha = MIN_EVAL;
				beta = MAX_EVAL;
				System.out.println("Repeating depth:" + currentSearchDepth);
			} else {
				alpha = eval - ASP_WIN_SIZE;
				beta = eval + ASP_WIN_SIZE;
				
				System.out.println(getThinking(startTime, e2, currentSearchDepth));
				maxDepthSearched = currentSearchDepth;
				currentSearchDepth++;
			}
		}
		stats.depth(maxDepthSearched);
		return bestEvalsByDepth[maxDepthSearched];
	}

	public int alphaBeta(int entryMove, EngineBoard board, int alpha, int beta,
			boolean maximizingPlayer, int depthLeft, int distanceFromRoot) {
		if (abandonSearch) { // doesn't matter what you return
			return beta;
		}
		stats.addNode(false);
		long zKey = board.getZobristKey();
		int tmove = 0;
		int tscore = EvalTTable.NO_IEVAL;
		boolean positionInTable = evalTable.hasPosition(zKey);
		if (positionInTable) {
			tmove = evalTable.getMove(zKey);
			tscore = evalTable.getEval(zKey, alpha, beta, depthLeft,
					maximizingPlayer);
			if (tscore != EvalTTable.NO_IEVAL && tmove != 0) {
				stats.ttHit(false);
				return tscore;
			}
		}

		if (isDraw(board)) {
			return 0;
		}
		if (depthLeft == 0) {
			int rv = staticEval(entryMove, board, maximizingPlayer, alpha,
					beta);
			return rv;
		}
		listener.enteredNode(alpha, beta, false,
				entryMove, EngineListener.NORMAL);
		
		// maximizingPlayer because we want to allow null moves only in alternate depths.
		if (!board.kingInCheck(board.getWhiteToMove()) && maximizingPlayer && depthLeft  >= 3) {
			board.makeNullMove();
			int nullScore = -alphaBeta(0, board, -beta, -alpha, !maximizingPlayer, depthLeft - 2, distanceFromRoot+1);
			if (nullScore >= beta) {
				board.undoNullMove();
				stats.betaCutoff(0, false);
				return beta;
			}
			board.undoNullMove();
		}

		Moves m = movesPool.allocate();
		int numMoves = board.getMoveGenerator().fillLegalMoves(m.moves, 0);
		if (numMoves == 0) {
			boolean white = board.getWhiteToMove();
			if (board.kingInCheck(white)) { // checkmate
				return maximizingPlayer ? -MATE_BOUND : MATE_BOUND;
			} else { // stalemate
				return 0;
			}
		}

		sortByHistoryAndSee(board, m, numMoves, historyHeuristicArray);
		
		int swapIndex = 0;
		if (positionInTable && tmove != 0) {
			if (swapWithIndex(tmove, swapIndex, m.moves, numMoves)) {
				swapIndex++;
			}
		}
		swapKillersToFront(m, numMoves, swapIndex, distanceFromRoot);
		
		int score = 0;
		int bestMove = 0;
		int bestScore = MIN_EVAL;

		boolean betaCutoff = false;
		boolean pvFound = false;
		
		for (int i = 0; i < numMoves; i++) {
			int move = m.moves[i];
			board.makeMove(move, false);
			boolean pvSearchSuccessful = false;
			if (pvFound) {
				if (i >= 4 && !board.kingInCheck(board.getWhiteToMove()) && depthLeft >= 3) {
					score = -alphaBeta(move, board, -alpha-1, -alpha, !maximizingPlayer,
							depthLeft - 2, distanceFromRoot+1);
					pvSearchSuccessful = (score <= alpha);
				}
				if (!pvSearchSuccessful) {
					score = -alphaBeta(move, board, -alpha-1, -alpha, !maximizingPlayer,
							depthLeft - 1, distanceFromRoot+1);
					pvSearchSuccessful = (score <= alpha);
				}
			} 
			
			if (!pvSearchSuccessful) {
				score = -alphaBeta(move, board, -beta, -alpha, !maximizingPlayer,
						depthLeft - 1, distanceFromRoot+1);
			} 

			board.undoLastMove();
			if (score > bestScore) {
				bestScore = score;
			} 
			if (score >= beta) {
				// beta = score;
				betaCutoff = true;
				incrementHistoryHeuristicArray(move, true, historyHeuristicArray);
				evalTable.storePosition(zKey, beta, alpha, beta, depthLeft,
						move, maximizingPlayer);
				addToKillerMoves(distanceFromRoot, move);
				stats.betaCutoff(i, false);
				break;
			}
			if (score > alpha) {
				alpha = score;
				bestMove = move;
				pvFound = true;
			} else {
				incrementHistoryHeuristicArray(move, false, historyHeuristicArray);
			}
		}

		movesPool.release(m);
		int rv = betaCutoff ? beta : alpha;
		listener.exitNode(score);
		if (!betaCutoff && bestMove != 0) {
			evalTable.storePosition(zKey, bestScore, alpha, beta, depthLeft,
					bestMove, maximizingPlayer);
		}
		// evalTable.storeFen(zKey, BoardUtils.getFen(board));
		return rv;
	}

	

	//	protected void release(PVNode node) {
	//		if (node != null) {
	//			release((PVNode) node.next);
	//			pvNodePool.release(node);
	//		}
	//	}
	
		protected int staticEval(int entryMove, EngineBoard board,
				boolean maximizingPlayer, int alpha, int beta) {
			int rv = evaluator.evaluate(board);
			return rv;
		}


	private Continuation getLineFromEvalTable(EngineBoard board, int maxDepth) {
		long zKey = board.getZobristKey();
		if (evalTable.hasPosition(zKey)) {
			int i = 0;
			Continuation leval = new Continuation();
			leval.eval = evalTable.getEvalForPV(zKey);
			do {
				leval.line[i] = evalTable.getMove(zKey);
				if (!board.makeMove(leval.line[i], true)) {
					throw new RuntimeException("Invalid move:" + PrintUtils.notation(leval.line[i]));
				}
				zKey = board.getZobristKey();
				i++;
			} while (evalTable.hasPosition(zKey) && i < maxDepth);
			while (i > 0) {
				board.undoLastMove();
				i--;
			}
			return leval;
		}
		return null;
	}

	// Taken from mediocre, basically
	private String getThinking(long startTime, Continuation e2, int depth) {
		StringBuilder sb = new StringBuilder();
		int eval = e2.eval;
		int length = 0;
		while (e2.line[length] != 0) {
			sb.append(PrintUtils.notation(e2.line[length]));
			sb.append(" ");
			length++;
		}
		long nodesSearched = stats.perIter.getNodes(false)+ stats.perIter.getNodes(true);
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
		if (eval >= MATE_BOUND) {
			mateInN = length;
		} else if (eval <= -MATE_BOUND) {
			mateInN = -length;
		}
		if (mateInN != 0) {
			return "info score mate " + mateInN + " depth " + depth
					+ " nodes " + nodesSearched + " nps " + nps + " time "
					+ splitTime + " pv " + sb.toString();
		}
		return "info score cp " + eval + " depth " + depth
				+ " nodes " + nodesSearched + " nps " + nps + " time "
				+ splitTime + " pv " + sb.toString();
	}

	private void addToKillerMoves(int distanceFromRoot, int move) {
		for (int i = 0; i < 4; i++) {
			if (killerMovesByDepth[i][distanceFromRoot] != 0) {
				killerMovesByDepth[i][distanceFromRoot] = move;
				break;
			}
		}
		
	}

	protected void swapKillersToFront(Moves m, int numMoves, int swapIndex, int distanceFromRoot) {
		for (int j = 0; j < 4; j++) {
			if (killerMovesByDepth[j][distanceFromRoot] != 0) {
				if (swapWithIndex(killerMovesByDepth[j][distanceFromRoot], swapIndex, m.moves, numMoves)) {
					swapIndex++;
				}
			} else {
				break;
			}
		}
	}
	
	protected void incrementHistoryHeuristicArray(int move, boolean increment, int[][] hhArray) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		if (increment) {
			hhArray[from][to]++;
		} else {
			if (hhArray[from][to] > 0) {
				hhArray[from][to]--;
			}
		}
		
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

	/**
	 * Swaps the PV move with the first move.
	 * 
	 * @param pvMove
	 * @param moves
	 * @param numMoves
	 */
	protected boolean swapWithIndex(int pvMove, int swapIndex, int[] moves,
			int numMoves) {
		for (int i = swapIndex; i < numMoves; i++) {
			if (moves[i] == pvMove) {
				int first = moves[swapIndex];
				moves[swapIndex] = pvMove;
				moves[i] = first;
				return true;
			}
		}
		return false;

	}


//	private void sortMovesBySee(EngineBoard board, Moves m, int numMoves, boolean maximizingPlayer) {
//		Moves ranks = movesPool.allocate();
//		for (int i = 0; i < numMoves; i++) {
//			ranks.moves[i] = see.evaluateMove(board, m.moves[i], evaluator, movesPool, maximizingPlayer);
//		}
////		System.out.print(maximizingPlayer + " Before: ");
////		PrintUtils.printAsArray(ranks.moves, numMoves);
//		isortDescending(m, numMoves, ranks);
////		System.out.print(maximizingPlayer + " After: ");
////		PrintUtils.printAsArray(ranks.moves, numMoves);
//		
//		movesPool.release(ranks);
//		
//	}
	
	protected void sortByHistoryAndSee(EngineBoard board, Moves m, int numMoves, int[][] hhArray) {
		Moves historyRanks = movesPool.allocate();
		for (int i = 0; i < numMoves; i++) {
			short from = Encodings.getFromSquare(m.moves[i]);
			short to = Encodings.getToSquare(m.moves[i]);
			historyRanks.moves[i] = hhArray[from][to];
		}
		Moves seeRanks = movesPool.allocate();
		for (int i = 0; i < numMoves; i++) {
			seeRanks.moves[i] = see.evaluateMove(board, m.moves[i], movesPool);
		}
		isortDescending(m, numMoves, historyRanks, seeRanks);
		movesPool.release(historyRanks);
		movesPool.release(seeRanks);
	}


	protected void sortByHistory(EngineBoard board, Moves m, int numMoves, int[][] hhArray) {
		Moves ranks = movesPool.allocate();
		for (int i = 0; i < numMoves; i++) {
			short from = Encodings.getFromSquare(m.moves[i]);
			short to = Encodings.getToSquare(m.moves[i]);
			ranks.moves[i] = hhArray[from][to];
		}
		isortDescending(m, numMoves, ranks);

		movesPool.release(ranks);

		
	}

	protected void sortMvvLva(EngineBoard board, Moves m, int numMoves,
			boolean reverse) {
		int mult = reverse ? -1 : 1;
		Moves ranks = movesPool.allocate();
		for (int i = 0; i < numMoves; i++) {
			ranks.moves[i] = mult * rankMove(board, m.moves[i]);
		}
		isortDescending(m, numMoves, ranks);

		movesPool.release(ranks);
	}

	protected void sortMvvLva(EngineBoard board, Moves m, int numMoves) {
		sortMvvLva(board, m, numMoves, false);
	}

	void isortDescending(Moves m, int numMoves, Moves... ranks) {
		int lo = 0;
		while (lo < numMoves - 1) {
			int maxIndex = lo;
			for (int i = lo; i < numMoves; i++) {
				for (int j = 0; j < ranks.length; j++) {
					if (ranks[j].moves[i] > ranks[j].moves[maxIndex]) {
						maxIndex = i;
						break;
					} else if (ranks[j].moves[i] < ranks[j].moves[maxIndex]){
						break;
					}
				}
			}
			if (maxIndex > lo) {
				for (int j = 0; j < ranks.length; j++) {
					int tr = ranks[j].moves[maxIndex];
					ranks[j].moves[maxIndex] = ranks[j].moves[lo];
					ranks[j].moves[lo] = tr;
				}
				int tm = m.moves[maxIndex];
				m.moves[maxIndex] = m.moves[lo];
				m.moves[lo] = tm;
			}
			lo++;
		}

	}
//
//	void isort(Moves ranks, Moves m, int numMoves) {
//		int lo = 0;
//		while (lo < numMoves - 1) {
//			int maxIndex = lo;
//			for (int i = lo + 1; i < numMoves; i++) {
//				if (ranks.moves[i] > ranks.moves[maxIndex]) {
//					maxIndex = i;
//
//				}
//			}
//			int tr = ranks.moves[maxIndex];
//			int tm = m.moves[maxIndex];
//			ranks.moves[maxIndex] = ranks.moves[lo];
//			m.moves[maxIndex] = m.moves[lo];
//			ranks.moves[lo] = tr;
//			m.moves[lo] = tm;
//			lo++;
//		}
//
//	}

	private int rankMove(EngineBoard board, int move) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		byte victim = board.getPiece(to);
		byte attacker = board.getPiece(from);
		int vscore = score(victim);
		int ascore = score(attacker);
		return vscore << 3 | (7 - ascore);
	}

	private int score(byte piece) {
		switch (piece) {
		case Encodings.WKING:
		case Encodings.BKING:
			return 7;
		case Encodings.WQUEEN:
		case Encodings.BQUEEN:
			return 6;
		case Encodings.WROOK:
		case Encodings.BROOK:
			return 5;
		case Encodings.WBISHOP:
		case Encodings.BBISHOP:
			return 4;
		case Encodings.WKNIGHT:
		case Encodings.BKNIGHT:
			return 3;
		case Encodings.WPAWN:
		case Encodings.BPAWN:
			return 2;
		case Encodings.EMPTY:
			return 1;
		}
		return 0;
	}

	@Override
	public EngineStats getEngineStats() {
		return stats.cumulative;
	}
	
	@Override
	public Evaluator getEvaluator() {
		return evaluator;
	}
	
	protected class Moves implements Poolable {
		int[] moves = new int[128];

		@Override
		public void reset() {
		}

	}

//	protected class PVNode extends ListNode implements Poolable {
//		int eval = 0;
//		int move = 0;
//
//		@Override
//		public void reset() {
//			move = 0;
//			next = null;
//
//		}
//	}

	protected class Stats {
		 int subAlpha;
		int pvFail;
		int pvSucc;
		int lmrSucc;
		int lmrFail;
		int nodes = 0;
		int movesMade = 0;
		int movesGenerated = 0;
		int betaCutoffs = 0;
		int alphaImprovements = 0;
		double avgBetaIndex = 0;
		
		void reset() {
			nodes = 0;
			movesMade = 0;
			movesGenerated = 0;
			betaCutoffs = 0;
			alphaImprovements = 0;
			avgBetaIndex = 0;
			pvFail = 0;
			pvSucc = 0;
			lmrSucc = 0;
			lmrFail = 0;
			subAlpha = 0;
		}
		
		void avgBetaIndex(int index) {
			avgBetaIndex = ((avgBetaIndex * betaCutoffs) + index)/(betaCutoffs + 1);
		}
		void printStats() {
			if (nodes == 0) {
				return;
			}
			System.out.printf("Nodes=%d MovesGen=%d (%d per node, %d %% pruned) BCutoffs=%d (%d %%) AvgBetaIndex=%3.2f AImp=%d  SubAlpha=%d \n", 
					nodes,  movesGenerated, movesGenerated/nodes, ((movesGenerated - movesMade) * 100)/movesGenerated, betaCutoffs, (betaCutoffs*100)/nodes, 
					avgBetaIndex, alphaImprovements, subAlpha);
			System.out.printf("LMR[ %d succ, %d fail] PVS[%d succ, %d fail]\n", lmrSucc, lmrFail, pvSucc, pvFail);
		}
		
	}

	@Override
	public void setThinkingListener(ThinkingListener listener) {
		// TODO Auto-generated method stub
		
	}

	



	

}
