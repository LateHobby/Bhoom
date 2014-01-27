package sc;


public interface SPCBoard {

	public static interface MoveGenerator {

		boolean isMoveLegal(int move);
		int fillLegalMoves(int[] moveArr, int startIndex);
		int fillLegalCaptures(int[] moveArr, int startIndex);
		int fillLegalCapturesTo(int[] moveArr, int startIndex, short toSquare);
		
	}
	
	boolean makeMove(int move, boolean checkLegality);
	
	void undoLastMove();
	
	MoveGenerator getMoveGenerator();
	
//	int getGameState();
	
	boolean getWhiteToMove();
	int getHalfMoveClock();
	int getFullMoveNumber();
	short getEnPassantSquare();
	boolean hasCastlingRights(int rights);
	
	boolean isWhiteSquare(short squareEncoding);
	boolean isOffBoard(short squareEncoding);
	
	byte getPiece(short squareEncoding);
	
	boolean kingInCheck(boolean white);
	
	void initializeStandard();
	
	void initialize(char[] locatedPieces, boolean whiteToMove, short enPassantSquare,
			int halfMoveClock, int fullMoveNumber, 
			boolean castlingWKing, boolean castlingWQueen, boolean castlingBKing, boolean castlingBQueen);
	
	SPCBoard getCopy();



}
