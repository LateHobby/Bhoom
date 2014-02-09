package sc.bboard;

import java.io.Serializable;
import java.util.Arrays;

import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.util.BitManipulation;
import sc.util.PrintUtils;

public class OneSidePositionInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2854471429543182527L;

	private byte king;

	public boolean white;
	public long[] occ_boards = new long[6];

	public long figure_occ;
	public long pawn_occ;
	public long all_occ;

	public byte most_valuable_piece;
	public byte most_valuable_attacked_piece;
	public boolean hasMaterialToWin;
	
	// includes king attacks
	public long[] figure_attacks = new long[64];
	public long pawn_attacks;
	public long all_attacks;
	public long pawn_pushes;

	// whether this king is currently in check
	public boolean kingInCheck;
		
	public int numCheckers;
	public long checkers;
	public long check_blocking_squares;
	public long check_avoidance_squares;
	// For each relevant square the squares to which the piece on that square
	// is allowed to move to without violating a pin
	public long[] pin_blocking_squares = new long[64];
	public long is_pinned;

	

	OneSidePositionInfo(boolean white) {
		this.white = white;
		this.king = white ? Encodings.WKING : Encodings.BKING;
		initialize();
	}

	public void add(byte piece, short square) {
		occ_boards[piece - king] |= BitManipulation.bit_masks[square];

	}

	public void remove(byte piece, short square) {
		occ_boards[piece - king] &= ~BitManipulation.bit_masks[square];
	}

	public void initialize() {
		for (int i = 0; i < occ_boards.length; i++) {
			occ_boards[i] = 0L;
		}
		for (int i = 0; i < figure_attacks.length; i++) {
			figure_attacks[i] = 0L;
		}

		figure_occ = pawn_occ = all_occ = pawn_attacks = pawn_pushes = all_attacks = 0L;
	}

	public void updateOccupancy() {
		all_occ = occ_boards[0]; // king
		figure_occ = 0L;
		most_valuable_piece = Encodings.EMPTY;
		for (int i = 1; i < 5; i++) {
			all_occ |= occ_boards[i];
			figure_occ |= occ_boards[i];
			if (most_valuable_piece == Encodings.EMPTY && occ_boards[i] != 0L) {
				most_valuable_piece = (byte) (king + i);
			}
		}
		pawn_occ = occ_boards[5]; // pawns
		all_occ |= pawn_occ;
		if (most_valuable_piece == Encodings.EMPTY && pawn_occ != 0L) {
			most_valuable_piece = white ? Encodings.WPAWN : Encodings.BPAWN;
		}
	}

	public void updateMostValuableAttackedPiece(long enemyAttacks) {
		most_valuable_attacked_piece = Encodings.EMPTY;
		for (int i = 1; i < 5; i++) {
			if (most_valuable_attacked_piece == Encodings.EMPTY && 
					(occ_boards[i] & enemyAttacks) != 0L) {
				most_valuable_attacked_piece = (byte) (king + i);
				return;
			}
		}
		if (most_valuable_attacked_piece == Encodings.EMPTY && 
				(pawn_occ & enemyAttacks) != 0L) {
			most_valuable_attacked_piece = white ? Encodings.WPAWN : Encodings.BPAWN;
		}
	}
	
	public void updateAttacks(OneSidePositionInfo enemy) {
		System.arraycopy(zeroArray, 0, figure_attacks, 0, zeroArray.length);
		
		boolean hasWhiteBishop = false;
		boolean hasBlackBishop = false;
		hasMaterialToWin = false;
		
		long enemy_occ = enemy.all_occ;
		
		long combined_occ = all_occ | enemy_occ;
		all_attacks = 0L;
		
		long occ = occ_boards[0]; // king
		if (occ != 0) {
			short square = (short) Long.numberOfTrailingZeros(occ);
			figure_attacks[square] = FMoves.kingAttacks(square);
			all_attacks |= figure_attacks[square];
		}

		occ = occ_boards[1]; // queen
		while (occ != 0) {
			hasMaterialToWin = true;
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			figure_attacks[square] = FMoves.queenAttacks(square, combined_occ);
			all_attacks |= figure_attacks[square];
			occ &= ~loc;
		}

		occ = occ_boards[2]; // rook
		while (occ != 0) {
			hasMaterialToWin = true;
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			figure_attacks[square] = FMoves.rookAttacks(square, combined_occ);
			all_attacks |= figure_attacks[square];
			occ &= ~loc;
		}

		occ = occ_boards[3]; // bishop
		while (occ != 0) {
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			if (Encodings.isWhiteSquare(square)) {
				hasWhiteBishop = true;
			} else {
				hasBlackBishop = true;
			}
			figure_attacks[square] = FMoves.bishopAttacks(square, combined_occ);
			all_attacks |= figure_attacks[square];
			occ &= ~loc;
		}

		occ = occ_boards[4]; // knight
		while (occ != 0) {
			if (hasWhiteBishop || hasBlackBishop) {
				hasMaterialToWin = true;
			}
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			figure_attacks[square] = FMoves.knightAttacks(square);
			all_attacks |= figure_attacks[square];
			occ &= ~loc;
		}

		pawn_attacks = FMoves.pawnAttacks(pawn_occ, white);
		pawn_pushes = FMoves.pawnPushes(pawn_occ, combined_occ, white);
		all_attacks |= pawn_attacks;

		hasMaterialToWin |= (pawn_occ != 0L);
		

	}

	public void updatePinsAndChecks(OneSidePositionInfo enemy) {
//		System.arraycopy(zeroArray, 0, enemy.pin_blocking_squares, 0, zeroArray.length);

		long enemy_occ = enemy.all_occ;
		long enemy_king_loc = enemy.occ_boards[0]; // king
		
		long combined_occ = all_occ | enemy_occ;
		checkers = 0L;
		check_blocking_squares = 0L;
		check_avoidance_squares = 0L;
		enemy.is_pinned = 0L;
		short enemy_king_square = (short) Long
				.numberOfTrailingZeros(enemy_king_loc);

		long occ = occ_boards[1]; // queen
		while (occ != 0L) {
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);

			long ray = queen_rays[square][enemy_king_square]; 
			long check_avoidance = queen_check_avoidance[square][enemy_king_square];
			setBlockingSquaresAndCheckers(enemy, loc, square, enemy_king_loc, 
					enemy_king_square, ray, check_avoidance);
			
			occ &= ~loc;
		}

		occ = occ_boards[2]; // rook
		while (occ != 0) {
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			
			long pin_blocking_squares = rook_rays[square][enemy_king_square];
			long check_avoidance = rook_check_avoidance[square][enemy_king_square];
			setBlockingSquaresAndCheckers(enemy, loc, square, enemy_king_loc, 
					enemy_king_square, pin_blocking_squares, check_avoidance);
			
			occ &= ~loc;
		}

		occ = occ_boards[3]; // bishop
		while (occ != 0) {
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			
			long pin_blocking_squares = bishop_rays[square][enemy_king_square]; 
			long check_avoidance = bishop_check_avoidance[square][enemy_king_square];
			setBlockingSquaresAndCheckers(enemy, loc, square, enemy_king_loc, 
					enemy_king_square, pin_blocking_squares, check_avoidance);
			
			occ &= ~loc;
		}

		occ = occ_boards[4]; // knight
		while (occ != 0) {
			long loc = Long.lowestOneBit(occ);
			short square = (short) Long.numberOfTrailingZeros(loc);
			if ((figure_attacks[square] & enemy_king_loc) != 0L) {
				checkers |= loc;
			}
			all_attacks |= figure_attacks[square];
			occ &= ~loc;
		}


		// add pawn checkers
		checkers |= FMoves.pawnAttacks(enemy_king_loc, !white) & pawn_occ;
		
		numCheckers = Long.bitCount(checkers);
		
	}

	private void setBlockingSquaresAndCheckers(OneSidePositionInfo enemy, long loc,
			short square, long enemy_king_loc, short enemy_king_square, long ray, long check_avoidance) {
		
		long pin_blocking_squares = ray & ~enemy_king_loc & ~loc; // Remove piece and king locs
		
		if (pin_blocking_squares != 0L && (pin_blocking_squares & all_occ) == 0L) {
			long possible_pins = pin_blocking_squares & enemy.all_occ;
			int numBlockers = Long.bitCount(possible_pins);
			if (numBlockers == 1) { // this is a pinned piece
				int pin_square = Long.numberOfTrailingZeros(possible_pins);
				enemy.pin_blocking_squares[pin_square] = (pin_blocking_squares | loc);
				enemy.is_pinned |= possible_pins;
			} else { // these are not pinned
				enemy.is_pinned &= ~possible_pins;
			}
		}
		if ((figure_attacks[square] & enemy_king_loc) != 0L) { // the figure is giving check
			checkers |= loc;
			check_blocking_squares |= pin_blocking_squares; 
			check_avoidance_squares |= check_avoidance;
		}
	}

	
	public void updateKingInCheck(long enemy_attacks) {
		kingInCheck = ((occ_boards[0] & enemy_attacks) != 0L);

	}

	static private long[] zeroArray = new long[64];
	
	static public long[][] rook_rays = new long[64][64];
	static public long[][] bishop_rays = new long[64][64];
	static public long[][] queen_rays = new long[64][64];
	
	static public long[][] rook_check_avoidance = new long[64][64];
	static public long[][] bishop_check_avoidance = new long[64][64];
	static public long[][] queen_check_avoidance = new long[64][64];
	
	static {
		generateRays();
		Arrays.fill(zeroArray, 0L);
	}
	
	static void generateRays() {
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {
				rook_rays[i][j] = getRookRay(i, j);
				bishop_rays[i][j] = getBishopRay(i, j);
				queen_rays[i][j] = rook_rays[i][j] | bishop_rays[i][j];
				
				rook_check_avoidance[i][j] = getRookCheckAvoidance(i, j);
				bishop_check_avoidance[i][j] = getBishopCheckAvoidance(i, j);
				queen_check_avoidance[i][j] = rook_check_avoidance[i][j] | bishop_check_avoidance[i][j];
			}
		}
	}
	
	private static long getBishopCheckAvoidance(int square, int king_square) {
		if ((EConstants.diag45[square] & EConstants.diag45[king_square]) != 0L) {
			int avoidance_square = (square < king_square) ? king_square + 9 : king_square - 9;
			if (avoidance_square >= 0 && avoidance_square < 64) {
				long avoidance_loc = BitManipulation.bit_masks[avoidance_square];
				if ((avoidance_loc & EConstants.diag45[king_square]) != 0L) {
					return avoidance_loc;
				}
			}
		}
		if ((EConstants.diag135[square] & EConstants.diag135[king_square]) != 0L) {
			int avoidance_square = (square < king_square) ? king_square + 7 : king_square - 7;
			if (avoidance_square >= 0 && avoidance_square < 64) {
				long avoidance_loc = BitManipulation.bit_masks[avoidance_square];
				if ((avoidance_loc & EConstants.diag135[king_square]) != 0L) {
					return avoidance_loc;
				}
			}
		}
		return 0L;
	}

	private static long getRookCheckAvoidance(int square, int king_square) {
		if ((EConstants.ranks[square] & EConstants.ranks[king_square]) != 0L) {
			int avoidance_square = (square < king_square) ? king_square + 1 : king_square - 1;
			if (avoidance_square >= 0 && avoidance_square < 64) {
				long avoidance_loc = BitManipulation.bit_masks[avoidance_square];
				if ((avoidance_loc & EConstants.ranks[king_square]) != 0L) {
					return avoidance_loc;
				}
			}
		}
		if ((EConstants.files[square] & EConstants.files[king_square]) != 0L) {
			int avoidance_square = (square < king_square) ? king_square + 8 : king_square - 8;
			if (avoidance_square >= 0 && avoidance_square < 64) {
				long avoidance_loc = BitManipulation.bit_masks[avoidance_square];
				if ((avoidance_loc & EConstants.files[king_square]) != 0L) {
					return avoidance_loc;
				}
			}
		}
		return 0L;
	}

	private static long getRookRay(int square, int king_square) {
		long ray = (EConstants.ranks[square] & EConstants.ranks[king_square]) |
				(EConstants.files[square] & EConstants.files[king_square]);
		return trimRay(ray, square, king_square);
		
	}
	
	private static long getBishopRay(int square, int king_square) {
		long ray = (EConstants.diag45[square] & EConstants.diag45[king_square]) |
				(EConstants.diag135[square] & EConstants.diag135[king_square]);
		return trimRay(ray, square, king_square);
	}
	
	private static long trimRay(long ray, int square, int king_square) {
		int min = square;
		int max = king_square;
		if (max < min) {
			max = square;
			min = king_square;
		}
		for (int i = 0; i < min; i++) {
			ray &= ~BitManipulation.bit_masks[i];
		}
		for (int i = max+1; i < 64; i++) {
			ray &= ~BitManipulation.bit_masks[i];
		}
		return ray;
	}

	public static void main(String[] args) {
		OneSidePositionInfo bi = new OneSidePositionInfo(true);
		PrintUtils.printAsBoards(BitManipulation.long_msb_masks[63], ~BitManipulation.long_msb_masks[0]);
		PrintUtils.printAsBoards(bi.getRookRay(8, 48), bi.getRookRay(56, 16), bi.getRookRay(0, 12), bi.getBishopRay(0, 63));
	}

	public long knightOcc() {
		return occ_boards[white ? Encodings.WKNIGHT - king : Encodings.BKNIGHT - king];
	}

	public long bishopOcc() {
		return occ_boards[white ? Encodings.WBISHOP - king : Encodings.BBISHOP - king];
	}

	public long rookOcc() {
		return occ_boards[white ? Encodings.WROOK - king : Encodings.BROOK - king];
	}

	public long queenOcc() {
		return occ_boards[white ? Encodings.WQUEEN - king : Encodings.BQUEEN - king];
	}

	public long kingOcc() {
		return occ_boards[white ? Encodings.WKING - king : Encodings.BKING - king];
	}

	public void setTo(OneSidePositionInfo other) {
		initialize();
		king = other.king;
		white = other.white;
		for (int i = 0; i < occ_boards.length; i++) {
			occ_boards[i] = other.occ_boards[i];
		}
		
	}
}
