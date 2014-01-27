package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.See;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.IntArray;
import sc.util.PrintUtils;
import sc.util.TTable.TTEntry;

public class SeeMoveSorter implements MoveSorter {

	/*
	 * Monsoon/Typhoon move order
	 * 
Hash move
Winning captures (judged by the SEE)
Even captures (judged by SEE)
Killer moves
Other non-captures (ranked by history value)
Losing captures (judged by SEE)
	 */
	protected int[] seeRanks = new int[128];
	protected int[] hhranks = new int[128];
	protected int[][] historyHeuristicArray = new int[64][64];
	protected int[][] killerMoves = new int[30][4];
	IntArray winningAndEvenCaptures = new IntArray(128);
	IntArray losingCaptures = new IntArray(128);
	IntArray nonCaptures = new IntArray(128);
	IntArray killerMovesArr = new IntArray(8);
	
	See see = new See();
	Evaluator eval = new SideToMoveEvaluator();
	
	public SeeMoveSorter() {
		for (int i = 0; i < killerMoves.length; i++) {
			killerMoves[i] = new int[4];
		}
	}
	
	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves,
			int numMoves) {
		for (int i = 0; i < numMoves; i++) {
			try {
				seeRanks[i] = see.evaluateMove(board, moves[i], eval);
				hhranks[i] = getHistoryRank(moves[i]);
			} catch (Throwable t) {
				System.out.println(BoardUtils.getFen(board));
				System.out.println(PrintUtils.notation(moves[i]));
				throw t;
			}
		}
		isortDescending(moves, numMoves, seeRanks, hhranks);
		orderMovesFinally(board, ply, moves, numMoves);
		
	}


	protected void orderMovesFinally(EngineBoard board, int ply, int[] moves,
			int numMoves) {
		winningAndEvenCaptures.clear();
		losingCaptures.clear();
		nonCaptures.clear();
		killerMovesArr.clear();
		for (int i = 0; i < numMoves; i++) {
			if (isCapture(board, moves[i])) {
				if (seeRanks[i] >= 0) {
					winningAndEvenCaptures.push(moves[i]);
				} else {
					losingCaptures.push(moves[i]);
				}
			} else if (isKillerMove(ply, moves[i])){
				killerMovesArr.push(moves[i]);
			} else {
				nonCaptures.push(moves[i]);
			}
		}
		int index = 0;
		index = append(moves, winningAndEvenCaptures, index);
		index = append(moves, killerMovesArr, index);
		index = append(moves, nonCaptures, index);
		index = append(moves, losingCaptures, index);
		if (index != numMoves) {
			throw new RuntimeException("Error in move sorting");
		}
	}

	private int getHistoryRank(int move) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		return historyHeuristicArray[from][to];
	}
	
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
	
	protected boolean isKillerMove(int ply, int move) {
		for (int slot = 0; slot < killerMoves[ply].length; slot++) {
		    if (move == killerMoves[ply][slot]) {
		    	return true;
		    }
		}
		return false;
	}


	protected boolean isCapture(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return true;
		} else {
			return board.getPiece(Encodings.getToSquare(move)) != Encodings.EMPTY;
		}
	}

	private int append(int[] moves, IntArray arr, int startIndex) {
		for (int i = 0; i < arr.size(); i++) {
			moves[startIndex + i] = arr.get(i);
		}
		return startIndex + arr.size();
	}

	void isortDescending(int[] m, int numMoves, int[]... ranks) {
		int lo = 0;
		while (lo < numMoves - 1) {
			int maxIndex = lo;
			for (int i = lo; i < numMoves; i++) {
				for (int j = 0; j < ranks.length; j++) {
					if (ranks[j][i] > ranks[j][maxIndex]) {
						maxIndex = i;
						break;
					} else if (ranks[j][i] < ranks[j][maxIndex]){
						break;
					}
				}
			}
			if (maxIndex > lo) {
				for (int j = 0; j < ranks.length; j++) {
					int tr = ranks[j][maxIndex];
					ranks[j][maxIndex] = ranks[j][lo];
					ranks[j][lo] = tr;
				}
				int tm = m[maxIndex];
				m[maxIndex] = m[lo];
				m[lo] = tm;
			}
			lo++;
		}

	}


}
