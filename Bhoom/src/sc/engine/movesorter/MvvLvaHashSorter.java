package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.Evaluator;
import sc.evaluators.SideToMoveEvaluator;

public class MvvLvaHashSorter extends AbstractMoveSorter {

	Evaluator eval = new SideToMoveEvaluator();
	int[] killerMoveRanks = new int[128];
	int[] victimRanks = new int[128];
	int[] attackerRanks = new int[128];
	
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
		}
		isortDescending(moves, numMoves, victimRanks, attackerRanks, killerMoveRanks, hhranks);
		swapWithStart(0, moves, hashMove, numMoves);

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
		return eval.pieceWeight(piece);
	}

}
