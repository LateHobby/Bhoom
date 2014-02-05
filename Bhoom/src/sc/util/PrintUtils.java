package sc.util;

import java.io.PrintStream;

import sc.encodings.Encodings;

public class PrintUtils {

	public static String[] padding = new String[64];
	static {
		padding[0] = "";
		for (int i = 1; i < padding.length; i++) {
			padding[i] = padding[i - 1] + " ";
		}
	}

	public static String notation(short square) {
		int file = Encodings.getFile(square);
		int rank = Encodings.getRank(square);
		char fchar = (char) ('a' + file);
		char rchar = (char) ('1' + rank);
		return "" + fchar + rchar;
	}

	public static String notation(int move) {
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		String not = notation(from) + notation(to);
		byte promotionPiece = Encodings.getPieceToPromoteTo(move);
		if (Encodings.isPiece(promotionPiece)) {
			not = not + Character.toLowerCase(FENInfo.pieceToFenChar(promotionPiece));
		}
		return not;
	}

	public static String binaryRepresentationLong(long l) {
		String str = Long.toBinaryString(l);
		String pad = "";
		for (int i = 0; i < 64 - str.length(); i++) {
			pad += '0';
		}
		str = pad + str;
		return str.substring(str.length() - 64, str.length());

	}

	public static String binaryRepresentationInt(int l) {
		String str = Integer.toBinaryString(l);
		String pad = "";
		for (int i = 0; i < 32 - str.length(); i++) {
			pad += '0';
		}
		str = pad + str;
		return str.substring(str.length() - 32, str.length());

	}

	public static String binaryRepresentationShort(short l) {
		String str = Integer.toBinaryString(l);
		String pad = "";
		for (int i = 0; i < 16 - str.length(); i++) {
			pad += '0';
		}
		str = pad + str;
		return str.substring(str.length() - 16, str.length());

	}

	public static String binaryRepresentationByte(byte l) {
		String str = Integer.toBinaryString(l);
		String pad = "";
		for (int i = 0; i < 8 - str.length(); i++) {
			pad += '0';
		}
		str = pad + str;
		return str.substring(str.length() - 8, str.length());

	}

	/**
	 * Prints the array as an array of longs in a Java compatible way, e.g
	 * {255L, 65280L, 16711680L, 4278190080L, 1095216660480L, 280375465082880L,
	 * 71776119061217280L, -72057594037927936L}
	 * 
	 * @param ranks
	 */
	public static void printAsArray(long[] ranks) {
		boolean first = true;
		System.out.print('{');
		for (long l : ranks) {
			if (!first) {
				System.out.print(", ");
			}
			System.out.print(l);
			System.out.print('L');
			first = false;
		}
		System.out.println('}');
	}

	public static void printAsArray(int[] ranks) {
		printAsArray(ranks, ranks.length);
	}

	public static void printAsArray(int[] ranks, int num) {
		boolean first = true;
		System.out.print('{');
		for (int i = 0; i < num; i++) {
			int l = ranks[i];
			if (!first) {
				System.out.print(", ");
			}
			System.out.print(l);
			first = false;
		}
		System.out.println('}');
	}

	public static void printAsBoards(long... boards) {
		printAsBoards(System.out, boards);
	}

	public static void printAsBoards(PrintStream stream, long... boards) {
		int width = boards.length * 10;
		int height = 8;
		char[][] buffer = new char[height][width];
		for (int i = 0; i < height; i++) {
			// buffer[i] = new char[width];
			for (int j = 0; j < width; j++) {
				buffer[i][j] = ' ';
			}
		}
		for (int i = 0; i < boards.length; i++) {
			char[][] board = toBoardChars(boards[i]);
			twoDimCopy(board, buffer, 0, i * 10);
		}
		printCharArray(stream, buffer);
	}

	private static void printCharArray(PrintStream stream, char[][] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			stream.println(buffer[i]);
		}
	}

	/**
	 * Copies the contents of src into the given location of buffer.
	 * 
	 * @param src
	 * @param buffer
	 * @param startRow
	 * @param startCol
	 */
	private static void twoDimCopy(char[][] src, char[][] buffer, int startRow,
			int startCol) {
		for (int i = 0; i < src.length; i++) {
			char[] arr = src[i];
			char[] line = buffer[startRow + i];
			System.arraycopy(arr, 0, line, startCol, arr.length);
		}

	}

	private static char[][] toBoardChars(long l) {
		char[][] ca = new char[8][8];
		for (int file = 0; file < 8; file++) {
			for (int rank = 0; rank < 8; rank++) {
				if (BitManipulation
						.isSet(Encodings.encodeSquare(file, rank), l)) {
					ca[7 - rank][file] = '1';
				} else {
					ca[7 - rank][file] = '0';
				}

			}
		}
		return ca;
	}

	public static void printMoves(int[] moves, int numMoves) {
		for (int i = 0; i < numMoves; i++) {
			System.out.print(notation(moves[i]));
			System.out.print(", ");
		}
		System.out.println();

	}

	public static void printFormattedMatrix(PrintStream stream, String[][] m,
			int padding) {
		int[] columnWidths = new int[m[0].length];
		for (int i = 0; i < columnWidths.length; i++) {
			columnWidths[i] = m[0][i].length();
		}
		for (int i = 1; i < m.length; i++) {
			for (int j = 0; j < columnWidths.length; j++) {
				if (m[i][j].length() > columnWidths[j]) {
					columnWidths[j] = m[i][j].length();
				}
			}
		}
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < columnWidths.length; j++) {
				printSpaces(stream, columnWidths[j] - m[i][j].length()
						+ padding);
				stream.print(m[i][j]);
			}
			stream.println();
		}

	}

	private static void printSpaces(PrintStream stream, int num) {
		for (int i = 0; i < num; i++) {
			stream.print(" ");
		}

	}


}
