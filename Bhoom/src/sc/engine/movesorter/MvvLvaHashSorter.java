package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.Evaluator;
import sc.evaluators.SideToMoveEvaluator;

public class MvvLvaHashSorter extends AbstractMoveSorter {

	
//	Evaluator eval = new SideToMoveEvaluator();
	int[] victimRanks = new int[128];
	int[] attackerRanks = new int[128];
	int[] combinedRanks = new int[128];
	
	boolean combineRanks;
	boolean slide;
	
	public MvvLvaHashSorter() {
		this(true, false);
	}
	
	public MvvLvaHashSorter(boolean combineRanks, boolean slide) {
		this.combineRanks = combineRanks;
		this.slide = slide;
	}
	
	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves,
			int numMoves) {
		for (int i = 0; i < numMoves; i++) {
			killerMoveRanks[i] = isKillerMove(ply, moves[i])? 1 : 0;
			hhranks[i] = getHistoryRank(moves[i]);
			if (isCapture(board, moves[i])) {
				victimRanks[i] = getValue(capturedPiece(board, moves[i]));
				attackerRanks[i] = -getValue(attackingPiece(board, moves[i]));
			} else {
				victimRanks[i] = 0;
				attackerRanks[i] = 0;
			}
			if (combineRanks) {
				combinedRanks[i] = 100 * (victimRanks[i] * 1000 + attackerRanks[i]) + 100 * killerMoveRanks[i];
			}
		}
		if (combineRanks) {
			isortDescending(moves, numMoves, combinedRanks, hhranks);
		} else {
			isortDescending(moves, numMoves, victimRanks, attackerRanks, killerMoveRanks, hhranks);
		}
		if (slide) {
			slideUpTo(0, moves, hashMove, numMoves);
		} else {
			swapWithStart(0, moves, hashMove, numMoves);
		}

	}
	
	

	private void slideUpTo(int index, int[] moves, int move, int numMoves) {
		for (int i = index; i < numMoves; i++) {
			if (moves[i] == move) {
				int curr = i-1;
				while (curr >= index) {
					moves[curr+1] = moves[curr];
					curr--;
				}
				moves[index] = move;
				break;
			}
		}
	}

	protected void swapWithStart(int startIndex, int[] moves, int move, int numMoves) {
		for (int i = startIndex + 1; i < numMoves; i++ ) {
			if (moves[i] == move) {
				int temp = moves[i];
				moves[i] = moves[startIndex];
				moves[startIndex] = temp;
				break;
			}
		}
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
