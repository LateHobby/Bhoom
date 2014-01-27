package sc.encodings;

import java.util.Random;

import sc.util.BitManipulation;

public class EZobrist {

	private static final int POSITION_TABLE_SIZE = 1024;  //2**10
	private static long[] position = new long[POSITION_TABLE_SIZE]; 
	private static long[] enPassant = new long[64];
	private static long[] castling = new long[4];
	private static long toMoveKeyWhite;
	private static long toMoveKeyBlack;
	static {
		Random r = new Random(78186); // my student number
		for (int i = 0; i < POSITION_TABLE_SIZE; i++) {
			position[i] = r.nextLong();
		}
		for (int i = 0; i < 64; i++) {
			enPassant[i] = r.nextLong();
		}
		for (int i = 0; i < 4; i++) {
			castling[i] = r.nextLong();
		}
		toMoveKeyWhite = r.nextLong();
		toMoveKeyBlack = r.nextLong();
	}
	
	public static long getMoveKey(byte piece, short square) {  
		int tableIndex = constructTableIndex(piece, square);
		return position[tableIndex];
	}

	

	/**
	 * This ten-bit index is constructed as follows:
	 * b0-b5 - square
	 * b6-b9 - piece
	 * 
	 * 
	 * @param piece
	 * @param square
	 * @return
	 */
	private static int constructTableIndex(byte piece, short square) {
		int index = (piece & BitManipulation.int_msb_masks[4]);
		index <<= 6;
		index |= (square & BitManipulation.int_msb_masks[6]);
		return index;
	}

	public static long getStateKey(boolean whiteToMove, short enPassantSquare,
			boolean cwKing, boolean cwQueen, boolean cbKing, boolean cbQueen) {
		long key = whiteToMove ? toMoveKeyWhite : toMoveKeyBlack;
		key ^= enPassant[enPassantSquare];
		if (cwKing) {
			key ^= castling[0];
		}
		if (cwQueen) {
			key ^= castling[1];
		}
		if (cbKing) {
			key ^= castling[2];
		}
		if (cbQueen) {
			key ^= castling[3];
		}
		return key;
		
	}
}
