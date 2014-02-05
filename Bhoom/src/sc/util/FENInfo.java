package sc.util;

import java.util.ArrayList;
import java.util.List;

import sc.encodings.Encodings;

public class FENInfo  {

	// Forsyth–Edwards Notation (FEN) is a standard notation for describing a
	// particular board position of a chess game. The purpose of FEN is to
	// provide all
	// the necessary information to restart a game from a particular position. A
	// FEN record contains six fields. The separator between fields is a space.
	// The fields are:
	// 1. Piece placement on squares (A8 B8 .. G1 H1) Each piece is identified
	// by a letter taken from the standard English names (white upper-case,
	// black lower-case).
	// Blank squares are noted using digits 1 through 8 (the number of blank
	// squares), and "/" separate ranks.
	// 
	//	2. Active color. "w" means white moves next, "b" means black.
	//
	// 3. Castling availability. Either - if no side can castle or a letter
	// (K,Q,k,q) for each side and castle possibility.
	
	// 4. En passant target square in algebraic notation or "-".
	
	// 5. Halfmove clock: This is the number of halfmoves since the last pawn
	// advance or capture.
	
	// 6. Fullmove number: The number of the current full move.

	public char[] locatedPieces;
//	public int gameState;
	public short enPassantSquare;
	public int halfMoveClock;
	public int fullMoveNumber;
	public boolean cwKing;
	public boolean cwQueen;
	public boolean cbKing;
	public boolean cbQueen;
	public boolean whiteToMove;

	
	public FENInfo() {
		super();
		
	}

	public static FENInfo parse(String fen) {
		FENInfo fi = new FENInfo();
		fi.cwKing = fi.cwQueen = fi.cbKing = fi.cbQueen = false;
		
		List<Character> cl = new ArrayList<Character>();

		char nextRow = '/';
		char fieldSeparator = ' ';

		char[] farr = fen.toCharArray();

		int file = 0; // starting at file a
		int rank = 7; // and rank 8
		int position = 0; // current position in fen string
		char nextPiece = '0'; // piece being evaluated

		// At the end of this loop you can expect that:
		// 1: The current position is the point within the fen string that is
		// one character beyond the
		// point at which the list ends.
		// 2: That the newBoard has been filled with pieces.
		nextPiece = farr[position++];
		while (nextPiece != fieldSeparator) {
			if (nextRow == nextPiece) {
				rank -= 1;
				file = 0;
			} else if (nextPiece >= '1' && nextPiece <= '8') {
				file += (nextPiece - '0');
			} else {
				byte piece = fenCharToPiece(nextPiece);
				short square = Encodings.encodeSquare(file, rank);
				char c = Encodings.encodeLocatedPiece(piece, square);
				cl.add(c);
				file++;
			}
			nextPiece = farr[position++];
		} 

		fi.locatedPieces = new char[cl.size()];
		for (int i = 0; i < cl.size(); i++) {
			fi.locatedPieces[i] = cl.get(i);
		}

		nextPiece = farr[position++];
		boolean whiteToMove = (nextPiece == 'w');
		fi.whiteToMove = whiteToMove;
		nextPiece = farr[position++];
		if (nextPiece != fieldSeparator) {
			throw new RuntimeException("Expected space after [" + nextPiece + "]");
		}

		nextPiece = farr[position++];
		while (nextPiece != fieldSeparator) {
			
			switch (nextPiece) {
			case 'K':
				fi.cwKing = true;
				break;
			case 'Q':
				fi.cwQueen = true;
				break;
			case 'k':
				fi.cbKing = true;
				break;
			case 'q':
				fi.cbQueen = true;
				break;
			case '-':
				
				break;
			default:
				throw new RuntimeException("Unknown castling type:" + nextPiece);

			}
			nextPiece = farr[position++];
		} ;

		int enpStart = position;
		nextPiece = farr[position++];
		while (nextPiece != fieldSeparator) {
			nextPiece = farr[position++];
		} ;
		short square = ParseUtils.getSquare(farr, enpStart);
		if (square >= 0) {
			fi.enPassantSquare = square;
		}
		
		nextPiece = farr[position++];
		
		int number = 0;
		while (nextPiece >= '0' && nextPiece <= '9') {
			number = number * 10 + (nextPiece - '0');
			nextPiece = farr[position++];
		}
		fi.halfMoveClock = number;
		if (nextPiece != fieldSeparator) {
			throw new RuntimeException("Expected space");
		}
		
		nextPiece = farr[position++];
		number = 0;
		while (nextPiece >= '0' && nextPiece <= '9') {
			number = number * 10 + (nextPiece - '0');
			if (position >= farr.length) {
				break;
			}
			nextPiece = farr[position++];
		}
		fi.fullMoveNumber = number;

		return fi;
	}

	public static byte fenCharToPiece(char pieceChar) {
		switch (pieceChar) {
		case 'K' : return Encodings.WKING;
		case 'Q' : return Encodings.WQUEEN;
		case 'R' : return Encodings.WROOK;
		case 'B' : return Encodings.WBISHOP;
		case 'N' : return Encodings.WKNIGHT;
		case 'P' : return Encodings.WPAWN;
		case 'k' : return Encodings.BKING;
		case 'q' : return Encodings.BQUEEN;
		case 'r' : return Encodings.BROOK;
		case 'b' : return Encodings.BBISHOP;
		case 'n' : return Encodings.BKNIGHT;
		case 'p' : return Encodings.BPAWN;
		default: throw new RuntimeException("Unrecognized piece:" + pieceChar);
		}
	}

	
	public String toFEN() {
		StringBuilder sb = new StringBuilder();
		fillPieces(sb);
		sb.append(" ");
		sb.append(whiteToMove ? "w" : "b");
		sb.append(" ");
		if (!(cwKing | cwQueen | cbKing | cbQueen)) {
			sb.append("-");
		} else {
			if (cwKing) {
				sb.append("K");
			}
			if (cwQueen) {
				sb.append("Q");
			}
			if (cbKing) {
				sb.append("k");
			}
			if (cbQueen) {
				sb.append("q");
			}
		}
		sb.append(" ");
		if (enPassantSquare <= 0) {
			sb.append("-");
		} else {
			sb.append(PrintUtils.notation(enPassantSquare));
		}
		sb.append(" ");
		sb.append(halfMoveClock);
		sb.append(" ");
		sb.append(fullMoveNumber);
		
		return sb.toString();
	}

	private void fillPieces(StringBuilder sb) {
		char[][] grid = new char[8][8];
		for (int file = 0; file < 8; file++) {
			for (int rank = 0; rank < 8; rank++) {
				grid[file][rank] = ' ';
			}
		}
		
		for (char lp : locatedPieces) {
			short square = Encodings.getSquare(lp);
			byte piece = Encodings.getPiece(lp);
			int file = Encodings.getFile(square);
			int rank = Encodings.getRank(square);
			grid[file][rank] = pieceToFenChar(piece);
		}
		
		int blanks = 0;
		for (int rank = 7; rank >= 0; rank --) {
			for (int file = 0; file < 8; file++) {
				if (grid[file][rank] == ' ') {
					blanks++;
				} else {
					if (blanks > 0) {
						sb.append(blanks);
						blanks = 0;
					}
					sb.append(grid[file][rank]);
				}
			}
			if (blanks > 0) {
				sb.append(blanks);
				blanks = 0;
			}
			if (rank > 0) {
				sb.append("/");
			}
		}
	}

	static public char pieceToFenChar(byte piece) {
		switch (piece) {
		case Encodings.WKING: return 'K';
		case Encodings.BKING: return 'k';
		case Encodings.WQUEEN: return 'Q';
		case Encodings.BQUEEN: return 'q';
		case Encodings.WROOK: return 'R';
		case Encodings.BROOK: return 'r';
		case Encodings.WBISHOP: return 'B';
		case Encodings.BBISHOP: return 'b';
		case Encodings.WKNIGHT: return 'N';
		case Encodings.BKNIGHT: return 'n';
		case Encodings.WPAWN: return 'P';
		case Encodings.BPAWN: return 'p';
			default : throw new RuntimeException("Unknown piece:" + piece);
		}
	}
}