package sc.bboard;

import sc.SPCBoard.MoveGenerator;
import sc.encodings.Castling;
import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.util.BitManipulation;

public class FMoveGenerator implements MoveGenerator {

	// private ObjectPool<Moves> movesPool;

	EBitBoard board;

	boolean debug;

	private enum CaptureMode {CAPTURES_ONLY, NONCAPTURES_ONLY, ALL};
	
	FMoveGenerator(EBitBoard board) {
		this.board = board;
	}

	public boolean isMoveLegal(int move) {
		boolean rv = false;
		int[] moves = new int[128];
		int totalMoves = fillLegalMoves(moves, 0);
		for (int i = 0; i < totalMoves; i++) {
			int cmove = moves[i];
			if (move == cmove) {
				rv = true;
				break;
			}
		}
		return rv;
	}

	@Override
	public int fillLegalMoves(int[] moveArr, int startIndex) {
		return fillMoves(moveArr, startIndex, CaptureMode.ALL, ~0L);
	}

	@Override
	public int fillLegalCaptures(int[] moveArr, int startIndex) {
		return fillMoves(moveArr, startIndex, CaptureMode.CAPTURES_ONLY, ~0L);
	}
	@Override
	public int fillLegalNonCaptures(int[] moveArr, int startIndex) {
		return fillMoves(moveArr, startIndex, CaptureMode.NONCAPTURES_ONLY, ~0L);
	}
	
	@Override
	public int fillLegalCapturesTo(int[] moveArr, int startIndex, short toSquare) {
		long targetMask = BitManipulation.bit_masks[toSquare];
		return fillMoves(moveArr, startIndex, CaptureMode.CAPTURES_ONLY, targetMask);
	}

	private int fillMoves(int[] moveArr, int fillindex, CaptureMode mode, long targetMask) {
		board.updateDerivedBoards();
		boolean whiteToMove = board.getWhiteToMove();
		OneSidePositionInfo friendly = whiteToMove ? board.posInfo.wConfig
				: board.posInfo.bConfig;
		OneSidePositionInfo enemy = whiteToMove ? board.posInfo.bConfig
				: board.posInfo.wConfig;
		switch (mode) {
		case CAPTURES_ONLY: 
			targetMask &= enemy.all_occ; 
			break;
		case NONCAPTURES_ONLY:
			targetMask &= ~enemy.all_occ; 
			break;
		case ALL:
			break;
		}
		
		if (enemy.numCheckers == 2) {
			// only king moves
		} else {
			if (mode != CaptureMode.CAPTURES_ONLY) {
				fillindex = fillCastlingMoves(friendly, enemy, moveArr,
						fillindex);
			}
			if (mode == CaptureMode.CAPTURES_ONLY) {
				// first the captures with the least valuable pieces
				fillindex = fillPawnMoves(friendly, enemy, moveArr, fillindex,
						targetMask);
				fillindex = fillFigureMoves(friendly, enemy, moveArr, fillindex,
						targetMask);
			} else {
				// prefer figure moves to pawn moves
				fillindex = fillFigureMoves(friendly, enemy, moveArr, fillindex,
						targetMask);
				fillindex = fillPawnMoves(friendly, enemy, moveArr, fillindex,
						targetMask);
			}
		}
		fillindex = fillKingMoves(friendly, enemy, moveArr, fillindex,
				targetMask);
		return fillindex;
	}

	private int fillFigureMoves(OneSidePositionInfo friendly,
			OneSidePositionInfo enemy, int[] moveArr, int fillindex,
			 long targetMask) {
		if (friendly.kingInCheck) {
			targetMask &= (enemy.check_blocking_squares | enemy.checkers);
		}
		if (targetMask != 0L) {
			// move less valuable pieces first
			for (int i = 4; i >= 1; i--) { // 1=queen, 4=knight 
				long figures = friendly.figure_occ & friendly.occ_boards[i];
				while (figures != 0L) {
					long fromLoc = Long.lowestOneBit(figures);
					short from = (short) Long.numberOfTrailingZeros(fromLoc);
					long targets = friendly.figure_attacks[from]
							& ~friendly.all_occ & targetMask;
					if ((friendly.is_pinned & fromLoc) != 0L) {
						targets &= friendly.pin_blocking_squares[from];
					}
					;
					while (targets != 0L) {
						long toLoc = Long.lowestOneBit(targets);
						short to = (short) Long.numberOfTrailingZeros(toLoc);
						moveArr[fillindex++] = Encodings.encodeMove(from, to,
								Encodings.EMPTY, false, false);

						targets &= ~toLoc;

					}
					figures &= ~fromLoc;
				}
			}
		}
		return fillindex;

	}

	private int fillKingMoves(OneSidePositionInfo friendly,
			OneSidePositionInfo enemy, int[] moveArr, int fillindex,
			long targetMask) {
		if (friendly.kingInCheck) {
			targetMask &= ~enemy.check_avoidance_squares;
		}
		long fromLoc = friendly.occ_boards[0];
		short from = (short) Long.numberOfTrailingZeros(fromLoc);
		long targets = friendly.figure_attacks[from] & ~friendly.all_occ
				& ~enemy.all_attacks & targetMask;
		while (targets != 0L) {
			long toLoc = Long.lowestOneBit(targets);
			short to = (short) Long.numberOfTrailingZeros(toLoc);
			moveArr[fillindex++] = Encodings.encodeMove(from, to,
					Encodings.EMPTY, false, false);
			targets &= ~toLoc;
		}
		return fillindex;
	}

	private int fillPawnMoves(OneSidePositionInfo friendly,
			OneSidePositionInfo enemy, int[] moveArr, int fillindex,
			long targetMask) {
		// pawn captures
		long pawnCaptureTargets = friendly.pawn_attacks & enemy.all_occ & targetMask;
		if (friendly.kingInCheck) {
			pawnCaptureTargets &= (enemy.check_blocking_squares | enemy.checkers);
		}
		// capture most valuable pieces first
		for (int i = 1; i < 6; i++) { // 1=queen, 5=pawn
			long localTargets = pawnCaptureTargets & enemy.occ_boards[i];
			while (localTargets != 0L) {
				long toLoc = Long.lowestOneBit(localTargets);
				short to = (short) Long.numberOfTrailingZeros(toLoc);
				long capturingPawns = FMoves.capturingPawns(friendly.white, to,
						friendly.occ_boards[5]); // 5 = pawn
				while (capturingPawns != 0L) {
					long pawnLoc = Long.lowestOneBit(capturingPawns);
					short from = (short) Long.numberOfTrailingZeros(pawnLoc);

					if ((friendly.is_pinned & pawnLoc) == 0L
							|| (toLoc & friendly.pin_blocking_squares[from]) != 0) {
						fillindex = fillPawnMovesWithPromotions(friendly.white,
								from, to, moveArr, fillindex);
					}

					capturingPawns &= ~pawnLoc;
				}
				localTargets &= ~toLoc;
			}
		}

		// en passant capture
		short enPassantSquare = board.getEnPassantSquare();
		if (enPassantSquare > 0 && 
				((friendly.white && enPassantSquare > 32) || (!friendly.white && enPassantSquare < 24))) { // include en-passant square
			long enPassantLoc = BitManipulation.bit_masks[enPassantSquare];
			if ((enPassantLoc & targetMask) != 0L) {
				boolean whiteToMove = board.getWhiteToMove();
				long capturingPawns = FMoves.capturingPawns(friendly.white,
						enPassantSquare, friendly.occ_boards[5]); // 5 = pawn
				while (capturingPawns != 0L) {
					long pawnLoc = Long.lowestOneBit(capturingPawns);
					short from = (short) Long.numberOfTrailingZeros(pawnLoc);

					int move = Encodings.encodeMove(from, enPassantSquare,
							Encodings.EMPTY, true, false);
					board.makeMove(move, false);
					board.updateDerivedBoards();
					if (!board.kingInCheck(whiteToMove)) {
						moveArr[fillindex++] = move;
					}
					board.undoLastMove();
					board.updateDerivedBoards();

					capturingPawns &= ~pawnLoc;
				}
			}
		}

		// pawn pushes
		long pawnPushTargets = friendly.pawn_pushes & targetMask;
		if (friendly.kingInCheck) {
			pawnPushTargets &= enemy.check_blocking_squares;
		}
		while (pawnPushTargets != 0L) {
			long toLoc = Long.lowestOneBit(pawnPushTargets);
			short to = (short) Long.numberOfTrailingZeros(toLoc);
			long pawnLoc = FMoves.pushingPawn(friendly.white, to,
					friendly.pawn_occ);
			if (pawnLoc != 0L) {
				short from = (short) Long.numberOfTrailingZeros(pawnLoc);
				if ((friendly.is_pinned & pawnLoc) == 0L
						|| (toLoc & friendly.pin_blocking_squares[from]) != 0) {
					fillindex = fillPawnMovesWithPromotions(friendly.white,
							from, to, moveArr, fillindex);
				}

			}
			pawnPushTargets &= ~toLoc;
		}
		return fillindex;
	}

	private int fillCastlingMoves(OneSidePositionInfo friendly,
			OneSidePositionInfo enemy, int[] moveArr, int fillindex) {
		boolean white = friendly.white;
		long occ = friendly.all_occ | enemy.all_occ;
		long enemyAttacks = enemy.all_attacks;
		short from = (short) Long.numberOfTrailingZeros(friendly.occ_boards[0]); // king
																					// board
		if (white) {
			if (board.hasCastlingRights(Castling.W_KING)) {
				if ((EConstants.w_kingside_castle_unoccupied & occ) == 0
						&& (EConstants.w_kingside_castle_checkfree & enemyAttacks) == 0) {
					moveArr[fillindex++] = Encodings.encodeMove(from,
							EConstants.w_king_castle_to, Encodings.EMPTY,
							false, true);

				}
			}
			if (board.hasCastlingRights(Castling.W_QUEEN)) {
				if ((EConstants.w_queenside_castle_unoccupied & occ) == 0
						&& (EConstants.w_queenside_castle_checkfree & enemyAttacks) == 0) {
					moveArr[fillindex++] = Encodings.encodeMove(from,
							EConstants.w_queen_castle_to, Encodings.EMPTY,
							false, true);
				}
			}

		} else {
			if (board.hasCastlingRights(Castling.B_KING)) {
				if ((EConstants.b_kingside_castle_unoccupied & occ) == 0
						&& (EConstants.b_kingside_castle_checkfree & enemyAttacks) == 0) {
					moveArr[fillindex++] = Encodings.encodeMove(from,
							EConstants.b_king_castle_to, Encodings.EMPTY,
							false, true);
				}
			}
			if (board.hasCastlingRights(Castling.B_QUEEN)) {
				if ((EConstants.b_queenside_castle_unoccupied & occ) == 0
						&& (EConstants.b_queenside_castle_checkfree & enemyAttacks) == 0) {
					moveArr[fillindex++] = Encodings.encodeMove(from,
							EConstants.b_queen_castle_to, Encodings.EMPTY,
							false, true);
				}
			}
		}
		return fillindex;
	}

	/**
	 * Fills pawn moves including promotions. Does not do any legality checks.
	 * 
	 * @param white
	 * @param from
	 * @param to
	 * @param moveArr
	 * @param fillindex
	 * @return
	 */
	private int fillPawnMovesWithPromotions(boolean white, short from,
			short to, int[] moveArr, int fillindex) {
		int rank = Encodings.getRank(to);
		boolean promotion = white ? (rank == 7) : (rank == 0);
		boolean enPassantCapture = (board.getEnPassantSquare() == to);
		// byte piece = white ? Encodings.WPAWN : Encodings.BPAWN;
		if (!promotion) {
			moveArr[fillindex++] = Encodings.encodeMove(from, to,
					Encodings.EMPTY, enPassantCapture, false);
		} else {
			byte[] promotionPieces = white ? EConstants.w_promotions
					: EConstants.b_promotions;
			for (byte promotionPiece : promotionPieces) {
				moveArr[fillindex++] = Encodings.encodeMove(from, to,
						promotionPiece, false, false);
			}
		}
		return fillindex;
	}

}
