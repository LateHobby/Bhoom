package sc.bboard;

import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.util.BitManipulation;

public class FMoves {

	public static long[] kingAttacks = new long[64];
	public static long[] knightAttacks = new long[64];

	static {
		generateKingAttacks();
		generateKnightAttacks();
	}

	
	public static long slidingPieceAttack(byte piece, short square, long occ) {
		switch (piece) {
		case Encodings.WQUEEN:
		case Encodings.BQUEEN:
			return queenAttacks(square, occ);
		case Encodings.WROOK:
		case Encodings.BROOK:
			return rookAttacks(square, occ);
		case Encodings.WBISHOP:
		case Encodings.BBISHOP:
			return bishopAttacks(square, occ);
		default:
			throw new RuntimeException("Non-sliding piece");
		}

	}

	/**
	 * Returns the squares that the king on square can attack. INCLUDES squares
	 * occupied by friendly pieces, and does not include the square that the
	 * piece is on.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long kingAttacks(short square) {
		return kingAttacks[square];
	}

	/**
	 * Returns the squares that the queen on square can attack. INCLUDES squares
	 * occupied by friendly pieces, and does not include the square that the
	 * piece is on.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long queenAttacks(short square, long occ) {
		return rookAttacks(square, occ) | bishopAttacks(square, occ);
	}

	/**
	 * Returns the squares that the rook on square can attack. INCLUDES squares
	 * occupied by friendly pieces, and does not include the square that the
	 * piece is on.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long rookAttacks(short square, long occ) {
		return RookBishopAttacksFaster.getRookAttacks(square, occ, 0L);
	}

	/**
	 * Returns the squares that the bishop on square can attack. INCLUDES
	 * squares occupied by friendly pieces, and does not include the square that
	 * the piece is on.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long bishopAttacks(short square, long occ) {
		return RookBishopAttacksFaster.getBishopAttacks(square, occ, 0L);
	}

	/**
	 * Returns the squares that the knight on square can attack. INCLUDES
	 * squares occupied by friendly pieces, and does not include the square that
	 * the piece is on.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long knightAttacks(short square) {
		return knightAttacks[square];
	}

	public static long pawnAttacks(long pawnOcc, boolean white) {
		if (white) {
			long leftattacks = (pawnOcc & ~EConstants.files[63]) << 9;
			long rightattacks = (pawnOcc & ~EConstants.files[0]) << 7;
			return leftattacks | rightattacks;
		} else {
			long leftattacks = (pawnOcc & ~EConstants.files[0]) >>> 9;
			long rightattacks = (pawnOcc & ~EConstants.files[63]) >>> 7;
			return leftattacks | rightattacks;
		}
	}

	public static long pawnPushes(long pawnOcc, long occ, boolean white) {
		if (white) {
			long singlePushes = (pawnOcc << 8) & ~occ;
			// only pawns that were moved to the third rank by a single push
			long doublePushes = ((singlePushes & EConstants.ranks[16]) << 8)
					& ~occ;
			return singlePushes | doublePushes;
		} else {
			long singlePushes = (pawnOcc >>> 8) & ~occ;
			// only pawns that were moved to the sixth rank by a single push
			long doublePushes = ((singlePushes & EConstants.ranks[40]) >>> 8)
					& ~occ;
			return singlePushes | doublePushes;
		}
	}

	/**
	 * Finds the pawns in pawn_occ that can capture on the given square.
	 * @param white
	 * @param square
	 * @param pawn_occ
	 * @return
	 */
	public static long capturingPawns(boolean white, short square, long pawn_occ) {
		int file = Encodings.getFile(square);
		long pawns = 0L;
		if (white) {
			if (file != 0 && square >= 9) {
				pawns |= BitManipulation.bit_masks[square - 9];
			}
			if (file != 7 && square >= 7) {
				pawns |= BitManipulation.bit_masks[square - 7];
			}
		} else {
			if (file != 7 && square <= 54) {
				pawns |= BitManipulation.bit_masks[square + 9];
			}
			if (file != 0 && square <= 56) {
				pawns |= BitManipulation.bit_masks[square + 7];
			}
		}
		return pawns & pawn_occ;
	}
	/**
	 * Finds the pawn in pawn_occ that can push to the given square.
	 * @param white
	 * @param to
	 * @param pawn_occ
	 * @param all_occ
	 * @return
	 */
	public static long pushingPawn(boolean white, short to, long pawn_occ) {
		long pawn = 0L;
		if (white) {
			if ((pawn_occ & BitManipulation.bit_masks[to - 8]) != 0) {
				return BitManipulation.bit_masks[to - 8];
			} else if ((pawn_occ & BitManipulation.bit_masks[to - 16]) != 0) {
				return BitManipulation.bit_masks[to - 16];
			}
		} else {
			if ((pawn_occ & BitManipulation.bit_masks[to + 8]) != 0) {
				return BitManipulation.bit_masks[to + 8];
			} else if ((pawn_occ & BitManipulation.bit_masks[to + 16]) != 0) {
				return BitManipulation.bit_masks[to + 16];
			}
		}
		return pawn;
	}

	/**
	 * Determines whether the piece on a square can be captured enpassant.
	 * Only tests relation between given square and the en-passant square.
	 * @param piece
	 * @param white
	 * @param square
	 * @param enpassantSquare
	 * @return
	 */
	public static boolean canBeCapturedEnPassant(byte piece, boolean white,
			short square, short enpassantSquare) {
		if (enpassantSquare < 0) {
			return false;
		}
		byte targetPiece = white ? Encodings.BPAWN : Encodings.WPAWN;
		if (piece != targetPiece) {
			return false;
		}
		if (white && square == enpassantSquare - 8) {
			return true;
		}
		if (!white && square == enpassantSquare + 8) {
			return true;
		}
		return false;
	}
	/**
	 * The squares that block a check by a sliding piece between the attacking
	 * piece location and the king location. Do not include either the attacker
	 * location or the king location. Does consider attackers to be blocked by other 
	 * pieces of the same color.
	 * 
	 * @param attPiece
	 * @param attSquare
	 * @param kingSquare
	 * @param kingLoc
	 * @param attackingOcc
	 * @return
	 */
	public static long checkBlockingSquares(byte attPiece, short attSquare,
			short kingSquare, long kingLoc, long attackingOcc) {
		long pinSquares = 0L;
		long attLoc = BitManipulation.bit_masks[attSquare];
		switch (attPiece) {
		case Encodings.WQUEEN:
		case Encodings.BQUEEN:
			pinSquares |= rookCheckBlockingSquares(attSquare, attLoc,
					kingSquare, kingLoc, attackingOcc);
			pinSquares |= bishopCheckBlockingSquares(attSquare, attLoc,
					kingSquare, kingLoc, attackingOcc);
			break;
		case Encodings.WROOK:
		case Encodings.BROOK:
			pinSquares |= rookCheckBlockingSquares(attSquare, attLoc,
					kingSquare, kingLoc, attackingOcc);
			break;
		case Encodings.WBISHOP:
		case Encodings.BBISHOP:
			pinSquares |= bishopCheckBlockingSquares(attSquare, attLoc,
					kingSquare, kingLoc, attackingOcc);
			break;
		default:
			throw new RuntimeException("Not a sliding piece:" + attPiece);
		}
		return pinSquares;
	}

	private static long bishopCheckBlockingSquares(short attSquare,
			long attLoc, short kingSquare, long kingLoc, long attackingOcc) {
		long att = bishopAttacks(attSquare, attackingOcc | kingLoc);
		if ((att & kingLoc) == 0) {
			return 0L;
		}
		long reverseAtt = bishopAttacks(kingSquare, attLoc);
		return (att & reverseAtt);
	}

	private static long rookCheckBlockingSquares(short attSquare, long attLoc,
			short kingSquare, long kingLoc, long attackingOcc) {
		long att = rookAttacks(attSquare, attackingOcc | kingLoc);
		if ((att & kingLoc) == 0) {
			return 0L;
		}
		long reverseAtt = rookAttacks(kingSquare, attLoc);
		return (att & reverseAtt);
	}

	private static void generateKingAttacks() {
		for (int index = 0; index < 64; index++) {
			long attacks = 0L;
			short square = (short) index;
			int rank = Encodings.getRank(square);
			int file = Encodings.getFile(square);
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					int nrank = rank + i;
					int nfile = file + j;
					if (nrank == rank && nfile == file) {
						continue;
					}
					if (!Encodings.isOffBoard(nfile, nrank)) {
						short to = Encodings.encodeSquare(nfile, nrank);
						attacks = BitManipulation.set(to, attacks);
					}
				}
			}
			kingAttacks[index] = attacks;
		}
	}

	private static void generateKnightAttacks() {
		for (int index = 0; index < 64; index++) {
			long attacks = 0L;
			short square = (short) index;
			int rank = Encodings.getRank(square);
			int file = Encodings.getFile(square);
			for (int i : new int[] { -2, -1, 1, 2 }) {
				for (int j : new int[] { -2, -1, 1, 2 }) {
					int nrank = rank + i;
					int nfile = file + j;
					if (Math.abs(i) == Math.abs(j)) {
						continue;
					}
					if (!Encodings.isOffBoard(nfile, nrank)) {
						short to = Encodings.encodeSquare(nfile, nrank);
						attacks = BitManipulation.set(to, attacks);
					}
				}
			}
			knightAttacks[index] = attacks;
		}

	}

	

	

	

}
