package sc.util;

import sc.encodings.Encodings;

public class ParseUtils {

	/** Gets the encoded square for a pair of rank-file characters in carr at start. */
	public static short getSquare(char[] carr, int start) {
		if ('a' <= carr[start] && carr[start] <= 'h' && '1' <= carr[start+1] && carr[start+1] <= '8') {
			int file = carr[start] - 'a';
			int rank = carr[start+1] - '1';
			return Encodings.encodeSquare(file, rank);
		} else {
			return -1; // off board square
		}
	}
	
	public static int getMove(String notationmove) {
		return getMove(notationmove, false, false);
	}
	/** Gets the encoding for the given algebraic move.  */
	public static int getMove(String notationmove, boolean enPassantCapture, boolean castling) {
		char[] carr = notationmove.toCharArray();
		short from = getSquare(carr, 0);
		short to = getSquare(carr, 2);
		byte promotionPiece = Encodings.EMPTY;
		if (carr.length > 4) {
			promotionPiece = FENInfo.fenCharToPiece(carr[4]);
		}
		return Encodings.encodeMove(from, to, promotionPiece, enPassantCapture, castling);
	}

}
