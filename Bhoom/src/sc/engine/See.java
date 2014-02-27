package sc.engine;

import java.util.Random;

import sc.bboard.FMoves;
import sc.bboard.OneSidePositionInfo;
import sc.bboard.PositionInfo;
import sc.encodings.EZobrist;
import sc.encodings.Encodings;
import sc.util.BitManipulation;
import sc.util.BoardUtils;
import sc.util.LPTTable;
import sc.util.PrintUtils;
import sc.util.TTable.ProbeResult;

public class See {

	// If true, then the scores are stored in evaltable but not used except to check,
	// if the same position occurs again, that the score evaluated is the same as the one stored.
	final boolean debug = false;
	
	long[] squareKey = new long[64];
	long[] pieceKey = new long[13];
	
	LPTTable evalTable = new LPTTable(21, 1);
//	Map<Long, String> fenMap = new HashMap<Long, String>();
//	Map<Long, Long> bmap = new HashMap<Long, Long>();

	int[] pieceValues = new int[100];
	int[] scores = new int[100];
	byte[] piecesCaptured = new byte[100];

	Evaluator eval;
	int numCaptures = 0;

	short toSquare;
	long toLoc;
	PositionInfo pinfo = new PositionInfo();
	boolean whiteToMove;
	byte pieceOnToSquare;

	int calls;
	int successes;
	
	public See() {
		Random r = new Random();
		for (int i = 0; i < squareKey.length; i++) {
			squareKey[i] = r.nextLong();
		}
		for (int i = 0; i < pieceKey.length; i++) {
			pieceKey[i] = r.nextLong();
		}
	}
	ProbeResult probeResult = new ProbeResult();
	public int evaluateMove(EngineBoard board, int move, Evaluator eval) {
		int storedEval = Integer.MIN_VALUE;
		calls++;
		long zKey = getPositionAndMoveKey(board, move);
		
		if (evalTable.contains(zKey, probeResult)) {
			successes++;
			storedEval = (int) evalTable.get(zKey);
			if (!debug) {
				return storedEval;
			}
		}

		if (Encodings.isCastling(move)) {
			return 0;
		}
		this.eval = eval;
		toSquare = Encodings.getToSquare(move);
		toLoc = BitManipulation.bit_masks[toSquare];
		pinfo.setTo(board.getPositionInfo());
		pinfo.updateDerivedBoards();
		whiteToMove = board.getWhiteToMove();

		numCaptures = 0;
		piecesCaptured[numCaptures] = capturedPiece(board, move);
		pieceValues[numCaptures] = 0;
		if (piecesCaptured[numCaptures] != Encodings.EMPTY) {
			pieceValues[numCaptures] = getValue(piecesCaptured[numCaptures]);
		}
		scores[numCaptures] = pieceValues[numCaptures];
		numCaptures++;
		pieceValues[numCaptures] = 0;
		makeVirtualMove(board, move);
		pinfo.updateDerivedBoards();
		whiteToMove = !whiteToMove;
		short attackerSquare = -1;
		while ((attackerSquare = leastValuableAttackerSquare(board)) != -1) {
			byte movingPiece = board.getPiece(attackerSquare);
			if (movingPiece == Encodings.EMPTY) {
				System.out.println(BoardUtils.getFen(board));
				System.out.println(PrintUtils.notation(move));
				throw new RuntimeException("No piece moving from "
						+ PrintUtils.notation(attackerSquare));
			}
			piecesCaptured[numCaptures] = captureFrom(movingPiece,
					attackerSquare);
			pieceValues[numCaptures] = getValue(piecesCaptured[numCaptures]);
			numCaptures++;
			pieceValues[numCaptures] = 0;
			whiteToMove = !whiteToMove;
			try {
				pinfo.updateDerivedBoards();
				
			} catch (Throwable t) {
				System.out.println(BoardUtils.getFen(board));
				System.out.println(PrintUtils.notation(move));
				throw t;
			}
		}

		int value = rootScore(pieceValues, numCaptures);

		if (debug && storedEval != Integer.MIN_VALUE) {
			if (value != storedEval) {
				throw new RuntimeException("Stored Eval doesn't match value:"
						+ storedEval + " " + value);
			} 
		}
		
		evalTable.store(zKey, value);
		return value;

	}

	/**
	 * Returns a key unique to this position and move. This isn't the same as
	 * the zobrist key of the board AFTER the move.
	 * 
	 * @param board
	 * @param move
	 * @return
	 */
	private long getPositionAndMoveKey(EngineBoard board, int move) {
		long key = board.getZobristKey();
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		byte piece = board.getPiece(from);
		byte pieceCaptured = board.getPiece(to);
		if (piece == Encodings.EMPTY) {
			throw new RuntimeException("No piece moving?");
		}
		byte pieceToPromoteTo = Encodings.getPieceToPromoteTo(move);
		key ^=  EZobrist.getMoveKey(piece, from);
		if (pieceCaptured != Encodings.EMPTY) {
			key ^= EZobrist.getMoveKey(pieceCaptured, to);
		}
		if (pieceToPromoteTo != Encodings.EMPTY) {
			key ^= EZobrist.getMoveKey(pieceToPromoteTo, to);
		} else {
			key ^= EZobrist.getMoveKey(piece, to);
		}
		long keyAfterMove = key;
//		keyAfterMove ^= squareKey[from];
		keyAfterMove ^= squareKey[to];
//		keyAfterMove ^= pieceKey[piece];
		keyAfterMove ^= pieceKey[pieceCaptured];
		
		return keyAfterMove;
	}

	int rootScore(int[] values, int length) {
		for (int i = 1; i < numCaptures; i++) {
			scores[i] = 0;
		}
		return values[0] - score(values, length, 1);
	}

	int score(int[] values, int length, int index) {
		int subscore = (index >= length - 1) ? 0 : -score(values, length,
				index + 1);
		scores[index] = Math.max(0, values[index] + subscore);
		return scores[index];
	}

	byte captureFrom(byte movingPiece, short attackerSquare) {
		byte rv = pieceOnToSquare;
		OneSidePositionInfo mover = whiteToMove ? pinfo.wConfig : pinfo.bConfig;
		OneSidePositionInfo other = whiteToMove ? pinfo.bConfig : pinfo.wConfig;
		other.remove(pieceOnToSquare, toSquare);
		mover.remove(movingPiece, attackerSquare);
		mover.add(movingPiece, toSquare);
		pieceOnToSquare = movingPiece;

		return rv;
	}

	short leastValuableAttackerSquare(EngineBoard board) {
		OneSidePositionInfo mover = whiteToMove ? pinfo.wConfig : pinfo.bConfig;
		OneSidePositionInfo other = whiteToMove ? pinfo.bConfig : pinfo.wConfig;
		long all_occ = mover.all_occ | other.all_occ;
		long capturingPawns = FMoves.capturingPawns(whiteToMove, toSquare,
				mover.pawn_occ);
		while (capturingPawns != 0L) {
			long loc = Long.lowestOneBit(capturingPawns);
			if (board.getPiece((short) Long.numberOfTrailingZeros(loc)) == Encodings.EMPTY) {
				PrintUtils.printAsBoards(mover.pawn_occ);

				throw new RuntimeException();
			}
			if (canMoveToSquare(loc, mover, other)) {
				return (short) Long.numberOfTrailingZeros(loc);
			}
			capturingPawns &= ~loc;
		}
		// Knight
		long attackingKnights = FMoves.knightAttacks(toSquare)
				& mover.knightOcc();
		while (attackingKnights != 0L) {
			long loc = Long.lowestOneBit(attackingKnights);
			if (canMoveToSquare(loc, mover, other)) {
				return (short) Long.numberOfTrailingZeros(loc);
			}
			attackingKnights &= ~loc;
		}
		// Bishop
		long bishopAttacks = FMoves.bishopAttacks(toSquare, all_occ);
		long attackingBishops = bishopAttacks & mover.bishopOcc();
		while (attackingBishops != 0L) {
			long loc = Long.lowestOneBit(attackingBishops);
			if (canMoveToSquare(loc, mover, other)) {
				return (short) Long.numberOfTrailingZeros(loc);
			}
			attackingBishops &= ~loc;
		}
		// Rook
		long rookAttacks = FMoves.rookAttacks(toSquare, all_occ);
		long attackingRooks = rookAttacks & mover.rookOcc();
		while (attackingRooks != 0L) {
			long loc = Long.lowestOneBit(attackingRooks);
			if (canMoveToSquare(loc, mover, other)) {
				return (short) Long.numberOfTrailingZeros(loc);
			}
			attackingRooks &= ~loc;
		}
		// Queen
		long attackingQueens = bishopAttacks & rookAttacks & mover.queenOcc();
		while (attackingQueens != 0L) {
			long loc = Long.lowestOneBit(attackingQueens);
			if (canMoveToSquare(loc, mover, other)) {
				return (short) Long.numberOfTrailingZeros(loc);
			}
			attackingQueens &= ~loc;
		}
		// King
		long kingAttack = FMoves.kingAttacks(toSquare) & mover.kingOcc();
		if (kingAttack != 0L) {
			if ((toLoc & other.all_attacks) == 0L) {
				return (short) Long.numberOfTrailingZeros(kingAttack);
			}
		}
		return -1;
	}

	boolean canMoveToSquare(long loc, OneSidePositionInfo mover,
			OneSidePositionInfo other) {
		if (mover.kingInCheck) {
			if (other.numCheckers > 1) {
				return false;
			}
			if (toLoc != other.checkers) { // not capturing the checker
				return false;
			}
		}
		if ((loc & mover.is_pinned) != 0) {
			short square = (short) Long.numberOfTrailingZeros(loc);
			if ((toLoc & mover.pin_blocking_squares[square]) == 0L) {
				return false;
			}
		}
		return true;
	}

	void makeVirtualMove(EngineBoard board, int move) {
		short from = Encodings.getFromSquare(move);
		byte pieceToPromoteTo = Encodings.getPieceToPromoteTo(move);
		byte piece = board.getPiece(from);
		byte captured = Encodings.EMPTY;
		boolean enPassantCapture = Encodings.isEnpassantCapture(move);
		// boolean castling = Encodings.isCastling(move);
		OneSidePositionInfo mover = whiteToMove ? pinfo.wConfig : pinfo.bConfig;
		OneSidePositionInfo other = whiteToMove ? pinfo.bConfig : pinfo.wConfig;

		if (enPassantCapture) {
			short enpassantSquare = board.getEnPassantSquare();
			short capturePawnSquare = (short) (Encodings.isWhite(piece) ? enpassantSquare - 8
					: enpassantSquare + 8);
			captured = board.getPiece(capturePawnSquare);
			mover.remove(piece, from);
			other.remove(captured, enpassantSquare);
			mover.add(piece, toSquare);
			pieceOnToSquare = piece;
		} else { // regular move
			mover.remove(piece, from);
			captured = board.getPiece(toSquare);
			if (captured != Encodings.EMPTY) {
				other.remove(captured, toSquare);
			}
			if (Encodings.isPiece(pieceToPromoteTo)) {
				mover.add(pieceToPromoteTo, toSquare);
				pieceOnToSquare = pieceToPromoteTo;
			} else {
				mover.add(piece, toSquare);
				pieceOnToSquare = piece;
			}
		}

	}

	byte capturedPiece(EngineBoard board, int move) {
		if (Encodings.isEnpassantCapture(move)) {
			return board.getWhiteToMove() ? Encodings.BPAWN : Encodings.WPAWN;
		}
		byte piece = board.getPiece(toSquare);
		return piece;
	}

	int getValue(byte piece) {
		return eval.pieceWeight(piece);
	}

}
