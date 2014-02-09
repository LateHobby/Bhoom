package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.MoveSorter;

public abstract class AbstractMoveSorter implements MoveSorter {

	protected int[][] historyHeuristicArray = new int[64][64];
	protected int[][] killerMoves = new int[200][4];

	protected int[] hhranks = new int[128];
	protected int[] killerMoveRanks = new int[128];

	public AbstractMoveSorter() {
		createArrays();
	}
	
	private void createArrays() {
		historyHeuristicArray = new int[64][64];
		killerMoves = new int[200][4];
	}

	@Override
	public void reset() {
		createArrays();
	}
	
	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove,
			int[] moves, int numMoves) {
		// TODO Auto-generated method stub

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
	
	protected int getHistoryRank(int move) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		return historyHeuristicArray[from][to];
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
