package sc.engine.engines;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.Evaluator;
import sc.engine.engines.AbstractEngine.LocalVars;

public class MoveHandler {

	EngineBoard board;
	boolean quiesce;
	int numMoves;
	int currentPos;
	int[] moveArr = new int[128];
	int[] rankArr = new int[128];
	boolean processingCaptures;
	boolean sorted;
	int hashMove;
	
	public boolean generateMoves(EngineBoard board, boolean quiesce,
			int hashMove, int ply, int[][] hhArray, int[][] killerMoves) {
		
		this.board = board;
		this.quiesce = quiesce;
		numMoves = 0;
		currentPos = 0;
		sorted = false;
		processingCaptures = true;
		this.hashMove = hashMove;
//		numMoves = board.getMoveGenerator().fillLegalCaptures(moveArr, 0);
//		if (!quiesce) {
//			if (numMoves == 0) {
//				processingCaptures = false;
//				numMoves = board.getMoveGenerator().fillLegalNonCaptures(moveArr, 0);
//			}
//		}
		if (quiesce) {
			numMoves = board.getMoveGenerator().fillLegalCaptures(moveArr, 0);
		} else {
			numMoves = board.getMoveGenerator().fillLegalMoves(moveArr, 0);
			processingCaptures = false;
		}
		fillRanks(ply, hhArray, killerMoves);
		return numMoves > 0;
		
	}

	

	public boolean hasMoreMoves(int ply, int[][] hhArray, int[][] killerMoves) {
		if (currentPos < numMoves) {
			return true;
		} else {
			if (quiesce) {
				return false;
			} else {
				if (processingCaptures) {
					processingCaptures = false;
					currentPos = 0;
					numMoves = board.getMoveGenerator().fillLegalNonCaptures(moveArr, 0);
					fillRanks(ply, hhArray, killerMoves);
				}
				return (currentPos < numMoves);
			}
		}
	}

	public int nextMove() {
		if (sorted) {
			return moveArr[currentPos++];
		}
		if (currentPos == numMoves - 1) {
			return moveArr[currentPos++];
		}
		int maxIndex = numMoves-1;
		int maxRank = rankArr[maxIndex];
		int maxMove = moveArr[maxIndex];
		int index = maxIndex;
		boolean locallySorted = true;
		while (index > currentPos) {
			if (rankArr[index-1] < maxRank) {
				moveArr[index] = moveArr[index-1];
				rankArr[index] = rankArr[index-1];
				locallySorted = false;
			} else {
				moveArr[index] = maxMove;
				rankArr[index] = maxRank;
				maxRank = rankArr[index-1];
				maxMove = moveArr[index-1];
			}
			index--;
		}
		sorted = locallySorted;
		currentPos++;
		return maxMove;
	}

	private void fillRanks(int ply, int[][] hhArray, int[][] killerMoves) {
		for (int i = currentPos; i < numMoves; i++) {
			rankArr[i] = getRank(i, ply, hhArray, killerMoves);
		}
		
	}



	private int getRank(int i, int ply, int[][] hhArray, int[][] killerMoves) {
		int move = moveArr[i];
		if (move == hashMove) {
			return Integer.MAX_VALUE;
		}
//		if (processingCaptures) {
//			return getMvvLvaRank(move);
//		} else {
//			return getHeuristicRank(move, ply, hhArray, killerMoves);
//		}
		return getMvvLvaRank(move) * 100 + getHeuristicRank(move, ply, hhArray, killerMoves);
	}
	
	private int getHeuristicRank(int move, int ply, int[][] historyHeuristicArray, int[][] killerMoves) {
		int kmWt = isKillerMove(ply, move, killerMoves) ? 1 : 0;
		int hhWt = getHistoryRank(move, historyHeuristicArray);
		return 100 * kmWt + hhWt;
	}

	protected int getHistoryRank(int move, int[][] historyHeuristicArray) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		return historyHeuristicArray[from][to];
	}
	
	protected boolean isKillerMove(int ply, int move, int[][] killerMoves) {
		for (int slot = 0; slot < killerMoves[ply].length; slot++) {
		    if (move == killerMoves[ply][slot]) {
		    	return true;
		    }
		}
		return false;
	}

	private int getMvvLvaRank(int move) {
		int attackerValue = getValue(attackingPiece(board, move));
		int captureValue = getValue(capturedPiece(board, move));
		return captureValue * 1000 - attackerValue;
	}



	private byte attackingPiece(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return board.getWhiteToMove() ? Encodings.BPAWN : Encodings.WPAWN;
		}
		short fromSquare = Encodings.getFromSquare(move);
		byte piece = board.getPiece(fromSquare);
		return piece;
	}
	
	byte capturedPiece(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return board.getWhiteToMove() ? Encodings.BPAWN : Encodings.WPAWN;
		}
		short toSquare = Encodings.getToSquare(move);
		byte piece = board.getPiece(toSquare);
		return piece;
	}

	int getValue(byte piece) {
		return Evaluator.STATIC_PIECE_WEIGHTS[piece];
	}

}
