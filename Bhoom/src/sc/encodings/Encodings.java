package sc.encodings;

import sc.util.BitManipulation;

/**
 * This class defines global encodings of pieces, squares and moves into
 * primitive data types. Pieces are represented as bytes, squares are
 * represented as shorts and moves are represented as ints. 
 * A piece on a square is a "located piece", ans can be represented as a char.
 * 
 */

public class Encodings {

	/**
	 * int_byte_masks[i] has only the bits for the i-th byte set 
	 */
	static private int[] int_byte_masks; 
	
	static {
		int_byte_masks = new int[4];
		for (int i = 0; i < 4; i++) {
			int_byte_masks[i] = 0;
			for (int j = i*8; j < (i+1) * 8; j++) {
				int_byte_masks[i] |= BitManipulation.int_bit_masks[j];
			}
		}
	}

	// PIECE ENCODINGS
	static public final byte EMPTY = 0;
	static public final byte WKING = 1;
	static public final byte WQUEEN = 2;
	static public final byte WROOK = 3;
	static public final byte WBISHOP = 4;
	static public final byte WKNIGHT = 5;
	static public final byte WPAWN = 6;
	static public final byte BKING = 7;
	static public final byte BQUEEN = 8;
	static public final byte BROOK = 9;
	static public final byte BBISHOP = 10;
	static public final byte BKNIGHT = 11;
	static public final byte BPAWN = 12;

	static public boolean isPiece(byte piece) {
		return piece >= 1 && piece <= 12;
	}

	static public boolean isWhite(byte piece) {
		return isPiece(piece) && (piece >= 1 && piece <= 6);
	}

	// LOCATED PIECES
	static public char encodeLocatedPiece(byte piece, short square) {
		int csq = square;
		csq <<= 8;
		csq |= piece;
		return (char) csq;
	}
	
	static public byte getPiece(char locatedPiece) {
		int cp = locatedPiece;
		cp &= BitManipulation.int_msb_masks[8];
		return (byte) cp;
	}
	
	static public short getSquare(char locatedPiece) {
		int cp = locatedPiece;
		cp >>>= 8;
		return (short) cp;
	}
	// SQUARE ENCODING
	/*
	 * The simple board encoding encodes a chessboard in a long. 
	 * Each bit of the long represents a square on the chessboard as shown below.
	 * 
	 b63  b62  b61  b60  b59  b58  b57  b56  b55  b54  b53  b52  b51  b50  b49  b48  b47  b46  b45  b44  b43  b42  b41  b40  b39  b38  b37  b36  b35  b34  b33  b32  b31  b30  b29  b28  b27  b26  b25  b24  b23  b22  b21  b20  b19  b18  b17  b16  b15  b14  b13  b12  b11  b10  b9   b8   b7   b6   b5   b4   b3   b2   b1   b0   
	 h8   g8   f8   e8   d8   c8   b8   a8   h7   g7   f7   e7   d7   c7   b7   a7   h6   g6   f6   e6   d6   c6   b6   a6   h5   g5   f5   e5   d5   c5   b5   a5   h4   g4   f4   e4   d4   c4   b4   a4   h3   g3   f3   e3   d3   c3   b3   a3   h2   g2   f2   e2   d2   c2   b2   a2   h1   g1   f1   e1   d1   c1   b1   a1   


	 */
	static public int getFile(short square) {
		return (square & BitManipulation.int_msb_masks[3]);
	}

	static public int getRank(short square) {
		return ((square >>> 3) & BitManipulation.int_msb_masks[3]);
	}

	static public char getFileChar(int file) {
		return (char) ('a' + file);
	}

	static public char getRankChar(int rank) {
		return (char) ('1' + rank);
	}

	static public short encodeSquare(int file, int rank) {
		return (short) (BitManipulation.int_msb_masks[6] & ((rank << 3) | file));
	}

	static public boolean isOffBoard(int file, int rank) {
		return (file < 0 || file >= 8 || rank < 0 || rank >= 8);
	}
	
	static public boolean isOffBoard(short square) {
		return (square < 0 || square >= 64);
	}
	
	static public boolean isWhiteSquare(short square) {
		return (square % 2 == 1);
	}
	
	static public String getNotation(short square) {
		int file = getFile(square);
		int rank = getRank(square);
		return getNotation(file, rank);

	}

	public static String getNotation(int file, int rank) {
		StringBuilder sb = new StringBuilder(2);
		sb.append(getFileChar(file));
		sb.append(getRankChar(rank));
		return sb.toString();
	}
	/**
	 MOVE encoding
	  (b's are bits)
	 <pre>
	 b0-b5 - from
	 b6-b11 - to
	 b12-b15 - piece to promote to, if promotion, zero otherwise
	 b16 - enpassant capture
	 b17 - castling
	 </pre>
	 */
	public static int encodeMove(short from, short to,byte pieceToPromoteTo, boolean enPassantCapture,
			boolean castling) {
		int move = 0x0000;
		if (castling) {
			move |= BitManipulation.bit_masks[1];
		}
		move <<= 1;
		if (enPassantCapture) {
			move |= BitManipulation.bit_masks[1];
		}
		move <<= 4;
		if (isPiece(pieceToPromoteTo)) {
			move |= (pieceToPromoteTo & BitManipulation.int_msb_masks[4]);
		}
		
		move <<= 6;
		move |= (to  & BitManipulation.int_msb_masks[6]);
		move <<= 6;
		move |= (from & BitManipulation.int_msb_masks[6]);
		
		return move;
	}
	
	static public short getFromSquare(int move) {
		return (short) (move & BitManipulation.int_msb_masks[6]);
	}
	
	static public short getToSquare(int move) {
		int cmove = move;
		cmove >>>= 6;
		return (short) (cmove & BitManipulation.int_msb_masks[6]);
	}
	
	static public byte getPieceToPromoteTo(int move) {
		int cmove = move;
		cmove >>>= 12;
		return (byte) (cmove & BitManipulation.int_msb_masks[4]);
	}
	
	static public boolean isEnpassantCapture(int move) {
		return ((move & BitManipulation.int_bit_masks[17]) != 0);
	}
	
	static public boolean isCastling(int move) {
		return ((move & BitManipulation.int_bit_masks[18]) != 0);
	}
	
	
	/**
	 GAME STATE ENCODING
	 <pre>
	 An int is used to record the game state. The 4 bytes of the int (b3, b2, b1, b0) 
	 store information as follows:
	 b0 - enpassant square
	 b1 - halfMoveClock
	 b2 - fullMoveNumber
	 b3 - encodes whiteToMove, and castling rights as follows: if the bits of b3
	      are (c7, c6, ..., c0) then:
	      c0 - w-king-castle  (bit 24)
	  	c1 - w-queen-castle (bit 25)
	  	c2 - b-king-castle  (bit 26)
	  	c3 - b-queen-castle (bit 27)
	  	c4 - whiteToMove    (bit 28)
	  	</pre>
	*/
//	public static int encodeGameState(boolean whiteToMove, short enpassantSquare,
//			int halfMoveClock, int fullMoveNumber, int castlingRights) {
//		int gs = 0;
//		gs = setWhitetoMove(whiteToMove, gs);
//		gs = setEnPassantSquare(enpassantSquare, gs);
//		gs = setHalfMoveClock(halfMoveClock, gs);
//		gs = setFullMoveNumber(fullMoveNumber, gs);
//		gs = setCastlingRights(gs, true, castlingRights);
//		return gs;
//	}
//	
//	
//	public static short getEnpassantSquare(int gameState) {
//		 return (short) (gameState & BitManipulation.int_msb_masks[8]);
//	}
//	
//	public static int setEnPassantSquare(short square, int gameState) {
//		gameState &= ~BitManipulation.int_msb_masks[8];
//		gameState |= (square & BitManipulation.int_msb_masks[8]);
//		return gameState;
//	}
//	
//	public static int getHalfMoveClock(int gameState) {
//		gameState >>>= 8;
//		return (gameState & BitManipulation.int_msb_masks[8]);
//	}
//	
//	public static int setHalfMoveClock(int halfMoveClock, int gameState) {
//		gameState &= ~int_byte_masks[1];
//		return gameState | (halfMoveClock << 8);
//	}
//	
//	public static int incrementHalfMoveClock(int gameState) {
//		int halfMoveClock = getHalfMoveClock(gameState);
//		return setHalfMoveClock(halfMoveClock+1, gameState);
//	}
//	
//	public static int getFullMoveNumber(int gameState) {
//		gameState >>>= 16;
//		return (gameState & BitManipulation.int_msb_masks[8]);
//	}
//	
//	public static int setFullMoveNumber(int fullMoveNumber, int gameState) {
//		gameState &= ~int_byte_masks[2];
//		return gameState | (fullMoveNumber << 16);
//	}
//	
//	public static int incrementFullMoveNumber(int gameState) {
//		int fullMoveNumber = getFullMoveNumber(gameState);
//		return setFullMoveNumber(fullMoveNumber+1, gameState);
//	}
//	
//	public static boolean getWhiteToMove(int gameState) {
//		return (gameState & BitManipulation.int_bit_masks[28]) != 0;
//	}
//	
//	public static int setWhitetoMove(boolean value, int gameState) {
//		return setBit(28, value, gameState);
//	}
//	
//	public static int toggleWhitetoMove(int gameState) {
//		boolean isSet = (gameState & BitManipulation.int_bit_masks[28]) != 0;
//		return setWhitetoMove(!isSet, gameState);
//	}
//	
//	private static int setBit(int index, boolean value, int toChange) {
//		if (value) {
//			return toChange | BitManipulation.int_bit_masks[index];
//		} else {
//			return toChange & ~BitManipulation.int_bit_masks[index];
//		}
//	}
//	/**
//	 * Returns a number between 0 and 16 in which each bit corresponds to one of the
//	 * castling rights.
//	 * @param gameState
//	 * @return
//	 */
//	public static int getCastling(int gameState) {
//		gameState >>>= 24;
//		return (gameState & BitManipulation.int_msb_masks[4]);
//	}
//	
//	public static boolean hasCastlingRights(int gameState, int type) {
//		switch (type) {
//		case Castling.NONE:
//			return (gameState & BitManipulation.int_bit_masks[24]) == 0 &&
//			(gameState & BitManipulation.int_bit_masks[25]) == 0 &&
//			(gameState & BitManipulation.int_bit_masks[26]) == 0 &&
//			(gameState & BitManipulation.int_bit_masks[27]) == 0;
//		case Castling.W_KING:
//			return (gameState & BitManipulation.int_bit_masks[24]) != 0;
//		case Castling.W_QUEEN:
//			return (gameState & BitManipulation.int_bit_masks[25]) != 0;
//		case Castling.B_KING:
//			return (gameState & BitManipulation.int_bit_masks[26]) != 0;
//		case Castling.B_QUEEN:
//			return (gameState & BitManipulation.int_bit_masks[27]) != 0;
//		case Castling.ALL:
//			return (gameState & BitManipulation.int_bit_masks[24]) != 0 &&
//					(gameState & BitManipulation.int_bit_masks[25]) != 0 &&
//					(gameState & BitManipulation.int_bit_masks[26]) != 0 &&
//					(gameState & BitManipulation.int_bit_masks[27]) != 0;
//					
//					
//			
//		}
//		return false;
//	}
//	
//	public static int setCastlingRights(int gameState, boolean value, int... rights) {
//		for (int type: rights) {
//			if (type == Castling.NONE) {
//				throw new RuntimeException("Cannot call this function with NONE");
//			}
//			switch (type) {
//			case Castling.ALL:
//				for (int bitNo= 24; bitNo < 28; bitNo++) {
//					gameState = setBit(bitNo, value, gameState);
//				}
//				break;
//			case Castling.W_KING:
//				gameState = setBit(24, value, gameState);
//				break;
//			case Castling.W_QUEEN:
//				gameState = setBit(25, value, gameState);
//				break;
//			case Castling.B_KING:
//				gameState = setBit(26, value, gameState);
//				break;
//			case Castling.B_QUEEN:
//				gameState = setBit(27, value, gameState);
//				break;
//				default:
//					throw new RuntimeException("Unknown castling rights:" + type);
//			}
//			
//		}
//		return gameState;
//	}
	
	public static void main(String[] args) {
		// for (int i = 0; i < 64; i++) {
		// System.out.print("" + getFileChar(getFile((short)i)) +
		// getRank((short)i) + ",");
		// if (i % 8 == 7) {
		// System.out.println();
		// }
		// }
		for (int rank = 0; rank < 8; rank++) {
			for (int file = 0; file < 8; file++) {
				int i = encodeSquare(file, rank);
				System.out.print("" + getFileChar(getFile((short) i))
						+ getRankChar(getRank((short) i)) + ",");
				if (i % 8 == 7) {
					System.out.println();
				}
			}
		}
	}

	

}
