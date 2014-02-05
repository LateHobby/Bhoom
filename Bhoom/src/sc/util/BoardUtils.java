package sc.util;

import java.util.ArrayList;
import java.util.List;

import sc.SPCBoard;
import sc.bboard.EBitBoard;
import sc.encodings.Castling;
import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;

// This class is inherently non re-entrant - see static variable moves[]
public class BoardUtils {

	public static char[] getLocatedPieces(SPCBoard board) {
		List<Character> cl = new ArrayList<Character>();
		for (short i = 0; i < 64; i++) {
			byte piece = board.getPiece(i);
			if (piece != Encodings.EMPTY) {
				char c = Encodings.encodeLocatedPiece(piece, i);
				cl.add(c);
			}
		}
		char[] lp = new char[cl.size()];
		for (int i = 0; i < cl.size(); i++) {
			lp[i] = cl.get(i);
		}

		return lp;
	}

	public static String getFen(SPCBoard board) {
		FENInfo fi = new FENInfo();
		fi.whiteToMove = board.getWhiteToMove();
		fi.enPassantSquare = board.getEnPassantSquare();
		fi.halfMoveClock = board.getHalfMoveClock();
		fi.fullMoveNumber = board.getFullMoveNumber();
		fi.cwKing = board.hasCastlingRights(Castling.W_KING);
		fi.cwQueen = board.hasCastlingRights(Castling.W_QUEEN);
		fi.cbKing = board.hasCastlingRights(Castling.B_KING);
		fi.cbQueen = board.hasCastlingRights(Castling.B_QUEEN);

		fi.locatedPieces = getLocatedPieces(board);

		return fi.toFEN();
	}

	public static void initializeBoard(SPCBoard board, String fen) {
		FENInfo fi = FENInfo.parse(fen);
		board.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare,
				fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen,
				fi.cbKing, fi.cbQueen);

	}

	// Performs the inverse of PrintUtils.notation(move)
	public static int encodeMove(SPCBoard board, String notation) {
		int numMoves = board.getMoveGenerator().fillLegalMoves(moves, 0);
		char[] ca = notation.toCharArray();
		short from = ParseUtils.getSquare(ca, 0);
		short to = ParseUtils.getSquare(ca, 2);
		byte pieceToPromoteTo = Encodings.EMPTY;
		if (ca.length > 4) {
			char c = ca[4]; //fifth character
			if (board.getWhiteToMove()) {
				c = Character.toUpperCase(c);
			}
			pieceToPromoteTo = FENInfo.fenCharToPiece(c);
		}
		for (int i = 0; i < numMoves; i++) {
			short fromSquare = Encodings.getFromSquare(moves[i]);
			short toSquare = Encodings.getToSquare(moves[i]);
			if (from == fromSquare && to == toSquare) {
				byte promoteTarget = Encodings.getPieceToPromoteTo(moves[i]);
				if (pieceToPromoteTo == promoteTarget) {
					return moves[i];
				} 
			}
		}
		return 0;
	}
	
	public static final String pgnForm(SPCBoard board, int move) {
		StringBuilder ret = new StringBuilder();
		short fromSquare = Encodings.getFromSquare(move);
		short toSquare = Encodings.getToSquare(move);
		if (Encodings.isCastling(move)) {
			if (toSquare == EConstants.w_king_castle_to
					|| toSquare == EConstants.b_king_castle_to) {
				return "O-O";
			} else {
				return "O-O-O";
			}
		}

		byte piece = board.getPiece(fromSquare);
		byte capturedPiece = board.getPiece(toSquare);
		if (piece == Encodings.WPAWN || piece == Encodings.BPAWN) {
			ret.append(getPawnMoveString(fromSquare, toSquare, capturedPiece,
					Encodings.isEnpassantCapture(move)));
		} else {
			ret.append(getDisambiguatedPieceString(board, move));
			if (capturedPiece != Encodings.EMPTY) {
				ret.append("x");
			}
			ret.append(Encodings.getNotation(toSquare));
		}
		ret.append(getMateOrCheckString(board, move));
		return ret.toString();
	}

	private static String getMateOrCheckString(SPCBoard board, int move) {
		boolean mate = false;
		boolean check = false;
		board.makeMove(move, false);
		if (board.kingInCheck(board.getWhiteToMove())) {
			check = true;
			int numMoves = board.getMoveGenerator().fillLegalMoves(moves, 0);
			if (numMoves == 0) {
				mate = true;
			}
		}

		board.undoLastMove();
		if (mate) {
			return "#";
		}
		if (check) {
			return "+";
		}
		return "";
	}

	private static String getPawnMoveString(short fromSquare, short toSquare,
			byte capturedPiece, boolean enpassantCapture) {
		boolean capture = enpassantCapture
				|| (capturedPiece != Encodings.EMPTY);
		if (capture) {
			return Encodings.getFileChar(Encodings.getFile(fromSquare)) + "x"
					+ Encodings.getNotation(toSquare);
		} else {
			return Encodings.getNotation(toSquare);
		}
	}

	static int[] moves = new int[128];

	private static String getDisambiguatedPieceString(SPCBoard board, int move) {
		short fromSquare = Encodings.getFromSquare(move);
		short toSquare = Encodings.getToSquare(move);
		byte piece = board.getPiece(fromSquare);
		int numMoves = board.getMoveGenerator().fillLegalMoves(moves, 0);
		short[] locs = new short[8];
		int numLocs = 0;
		for (int i = 0; i < numMoves; i++) {
			if (Encodings.getToSquare(moves[i]) == toSquare
					&& board.getPiece(Encodings.getFromSquare(moves[i])) == piece) {
				locs[numLocs++] = Encodings.getFromSquare(moves[i]);
			}
		}
		String pieceS = "" + getPieceChar(piece);
		if (numLocs > 1) {
			if (fileDisambiguatesLocs(fromSquare, locs, numLocs)) {
				return pieceS
						+ Encodings.getFileChar(Encodings.getFile(fromSquare));
			} else if (rankDisambiguatesLocs(fromSquare, locs, numLocs)) {
				return pieceS
						+ Encodings.getRankChar(Encodings.getRank(fromSquare));
			} else {
				return pieceS + Encodings.getNotation(fromSquare);
			}
		}
		return pieceS;
	}

	private static char getPieceChar(byte piece) {
		switch (piece) {
		case Encodings.WPAWN:
		case Encodings.BPAWN:
			return 'P';
		case Encodings.WKNIGHT:
		case Encodings.BKNIGHT:
			return 'N';
		case Encodings.WBISHOP:
		case Encodings.BBISHOP:
			return 'B';
		case Encodings.WROOK:
		case Encodings.BROOK:
			return 'R';
		case Encodings.WQUEEN:
		case Encodings.BQUEEN:
			return 'Q';
		case Encodings.WKING:
		case Encodings.BKING:
			return 'K';
		}
		throw new RuntimeException("No char for piece:" + piece);
	}

	private static boolean rankDisambiguatesLocs(short fromSquare,
			short[] locs, int numLocs) {
		int rank = Encodings.getRank(fromSquare);
		int count = 0;
		for (int i = 0; i < numLocs; i++) {
			if (rank == Encodings.getRank(locs[i])) {
				count++;
			}
		}
		return count == 0;
	}

	private static boolean fileDisambiguatesLocs(short fromSquare,
			short[] locs, int numLocs) {
		int file = Encodings.getFile(fromSquare);
		int count = 0;
		for (int i = 0; i < numLocs; i++) {
			if (file == Encodings.getFile(locs[i])) {
				count++;
			}
		}
		return count == 0;
	}

	public static String convertToPGNString(EngineBoard board, String[] sa,
			int start, int end) {
		StringBuilder sb = new StringBuilder();
		int[] moveArray = new int[128];
		int numMovesMade = 0;
		for (int i = start; i < end; i++) {
			int tmpMove = encodeMove(board, sa[i]);
			short from = Encodings.getFromSquare(tmpMove);
			short to = Encodings.getToSquare(tmpMove);
			int numMoves = board.getMoveGenerator()
					.fillLegalMoves(moveArray, 0);
			for (int j = 0; j < numMoves; j++) {
				int cm = moveArray[j];
				if ((Encodings.getFromSquare(cm) == from)
						&& (Encodings.getToSquare(cm) == to)) {
					sb.append(pgnForm(board, cm));
					sb.append(" ");
					board.makeMove(cm, false);
					numMovesMade++;
					break;
				}
			}
		}
		while (numMovesMade > 0) {
			board.undoLastMove();
			numMovesMade--;
		}
		return sb.toString();
	}

	public static int encodedForm(SPCBoard board, String notationMove) {
		int[] moves = new int[128];
		int numMoves = board.getMoveGenerator().fillLegalMoves(moves, 0);
		boolean kingSideCastle = notationMove.equals("O-O");
		boolean queenSideCastle = notationMove.equals("O-O-O");
		short from = 0;
		short to = 0;
		if (!(kingSideCastle || queenSideCastle)) {
			char[] carr = notationMove.toCharArray();
			from = ParseUtils.getSquare(carr, 0);
			to = ParseUtils.getSquare(carr, 2);
		}
		for (int i = 0; i < numMoves; i++) {
			short fromSq = Encodings.getFromSquare(moves[i]);
			short toSq = Encodings.getToSquare(moves[i]);
			if (kingSideCastle) {
				if (Encodings.isCastling(moves[i])) {
					if (board.getWhiteToMove()) {
						if (fromSq == EConstants.w_king_castle_from
								&& toSq == EConstants.w_king_castle_to) {
							return moves[i];
						}
					} else {
						if (fromSq == EConstants.b_king_castle_from
								&& toSq == EConstants.b_king_castle_to) {
							return moves[i];
						}
					}
				}
			} else if (queenSideCastle) {
				if (Encodings.isCastling(moves[i])) {
					if (board.getWhiteToMove()) {
						if (fromSq == EConstants.w_queen_castle_from
								&& toSq == EConstants.w_queen_castle_to) {
							return moves[i];
						}
					} else {
						if (fromSq == EConstants.b_queen_castle_from
								&& toSq == EConstants.b_queen_castle_to) {
							return moves[i];
						}
					}
				}
			} else {
				if (from == fromSq && to == toSq) {
					return moves[i];
				}
			}
		}
		return 0;
	}

}
