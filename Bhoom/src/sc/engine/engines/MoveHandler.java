package sc.engine.engines;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.Evaluator;
import sc.engine.engines.AbstractEngine.LocalVars;
import sc.util.BinaryMaxHeap;

public class MoveHandler {

	private class MoveAndRank implements Comparable<MoveAndRank>{
		int move;
		int materialGain;
		int materialRisk;
		int combinedRank;
		int hhrank;
		int killerMoveRank;
		@Override
		public int compareTo(MoveAndRank other) {
			int diff = combinedRank - other.combinedRank;
			if (diff == 0) {
				diff = hhrank - other.hhrank;
			}
			return diff;
		}
		
	}
	
	int numMoves;
	int[] moveArr = new int[128];
	MoveAndRank[] rankArr = new MoveAndRank[128];
	BinaryMaxHeap<MoveAndRank> heap = new BinaryMaxHeap<MoveHandler.MoveAndRank>(new MoveAndRank[128]);
	int hashMove;
	
	public MoveHandler() {
		for (int i = 0; i < rankArr.length; i++) {
			rankArr[i] = new MoveAndRank();
		}
	}
	
	
	public int generateMoves(EngineBoard board, boolean quiesce,
			int hashMove, int ply, int[][] hhArray, int[][] killerMoves) {
		if (quiesce) {
			numMoves = board.getMoveGenerator().fillLegalCaptures(moveArr, 0);
		} else {
			numMoves = board.getMoveGenerator().fillLegalMoves(moveArr, 0);
		}
		if (numMoves > 0) {
			fillRanks(board, ply, hhArray, killerMoves, hashMove);
		}
		return numMoves;
	}
	
	private void fillRanks(EngineBoard board, int ply, int[][] hhArray, int[][] killerMoves, int hashMove) {
		heap.reset();
		for (int i = 0; i < numMoves; i++) {
			int move = moveArr[i];
			MoveAndRank mr = rankArr[i];
			mr.move = move;
			fillRanks(mr, board, ply, hhArray, killerMoves);
			if (move == hashMove) {
				mr.combinedRank = Integer.MAX_VALUE;
			} else {
				mr.combinedRank = 1000 * ((mr.materialGain * 1000 - mr.materialRisk) +  mr.killerMoveRank) /* + (int) Math.log(mr.hhrank) */;
			}
			heap.insert(mr);
		}
		heap.insertsDone();
		
	}
	
	
	private void fillRanks(MoveAndRank mr, EngineBoard board, int ply,
			int[][] hhArray, int[][] killerMoves) {
		mr.materialGain = mr.materialRisk = mr.killerMoveRank = mr.hhrank = 0;
		if (isCapture(board, mr.move)) {
			int attackerValue = getValue(attackingPiece(board, mr.move));
			int captureValue = getValue(capturedPiece(board, mr.move));
			mr.materialGain = captureValue;
			mr.materialRisk = attackerValue;
		}
		byte pieceToPromoteTo = Encodings.getPieceToPromoteTo(mr.move);
		if (pieceToPromoteTo != Encodings.EMPTY) {
			// Gained the promoted piece and lost a pawn
			mr.materialGain += getValue(pieceToPromoteTo) - getValue(Encodings.WPAWN);
		}
		mr.killerMoveRank = isKillerMove(ply, mr.move, killerMoves) ? 1 : 0;
		mr.hhrank = getHistoryRank(mr.move, hhArray);
		
	}


	public boolean hasMoreMoves() {
		return !heap.isEmpty();
	}
	
	public int nextMove() {
		MoveAndRank m = heap.extractMax();
		return m.move;
	}
	
	protected boolean isCapture(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return true;
		} else {
			return board.getPiece(Encodings.getToSquare(move)) != Encodings.EMPTY;
		}
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
