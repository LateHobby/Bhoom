package sc.util;

import java.util.ArrayList;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;



/**
 * class Perft
 * 
 * This class runs a test on different positions to make sure all moves are
 * generated correctly.
 * 
 * It has its own main-method so it can be run separately
 * 
 * @author Jonatan Pettersson (mediocrechess@gmail.com)
 */
public class DPerft {

	/**
	 * Goes through the test-positions declared in this class, the argument is
	 * the max number of plies the correct solution can contain (to limit the
	 * time)
	 * 
	 * @param args
	 *            max number of plies
	 */
	public static void main(String args[]) {
//		Profiler.profile();
		EBitBoard dBitBoard = new EBitBoard();
		long maxPly = 40060325; // Standard depth
		int maxDepth = 20;
		//doPrint = false;

		/* Set the depth in args */
		if(args.length > 0) {
			try {
				maxDepth = Integer.parseInt(args[0]);
				maxPly = Long.parseLong(args[1]);
			} catch (NumberFormatException e) {}
		}

		/* Set up the positions */
		ArrayList<PerftTestPos> positions = new ArrayList<PerftTestPos>();

		/* Pos 1 - Start pos */
		long[] pos1ans = {-1L, 20L, 400L, 8902L, 197281L, 4865609L, 119060324L, 3195901860L, 84998978956L, 2439530234167L, 69352859712417L};
		positions.add(new PerftTestPos("Pos 1", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", pos1ans));

		/* Pos 2 */
		long[] pos2ans = {-1L, 48L, 2039L, 97862L, 4085603L, 193690690L, 8031647685L};
		positions.add(new PerftTestPos("Pos 2", "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", pos2ans));

		/* Pos 3 */
		long[] pos3ans = {-1L, 50L, 279L};
		positions.add(new PerftTestPos("Pos 3", "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67", pos3ans));

		/* Pos 4 */
		long[] pos4ans = {-1L, -1L, -1L, -1L, -1L, -1L, 38633283L};
		positions.add(new PerftTestPos("Pos 4", "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28", pos4ans));

		/* Pos 5 */
		long[] pos5ans = {-1L, -1L, -1L, -1L, -1L, 11139762L};
		positions.add(new PerftTestPos("Pos 5", "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", pos5ans));

		/* Pos 6 */
		long[] pos6ans = {-1L, -1L, -1L, -1L, -1L, -1L, 11030083L, 178633661L};
		positions.add(new PerftTestPos("Pos 6", "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", pos6ans));


		/* Check all the positions to the given depth */
		long startTot = System.currentTimeMillis();
		for(PerftTestPos ptp : positions) {
			System.out.println(ptp.getName());
			for(int i = 1; i < maxDepth && i < ptp.answerLength(); i++){
				if(ptp.getAnswerAtDepth(i) != -1L && ptp.getAnswerAtDepth(i) < maxPly) {
					FENInfo fi = FENInfo.parse(ptp.getFen());
					dBitBoard.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
					long start = System.currentTimeMillis();
					long answer = perft(dBitBoard, i, false);
					System.out.print("  Depth: " + i + " Answer: " + answer);
					if(answer == ptp.getAnswerAtDepth(i)) {
						System.out.print(" (Correct)");
					} else {
						System.out.print(" (Incorrect)");
					}
					System.out.println(" Time: " + convertMillis(System.currentTimeMillis()-start));
				}
			}

			
		}
		System.out.println("Total time: " + convertMillis(System.currentTimeMillis()-startTot));
	}

	private static class PerftTestPos {
		private String name;
		private String fen;
		private long[] answers;

		public PerftTestPos(String name, String fen, long[] answers) {
			this.name = name;
			this.fen = fen;
			this.answers = answers;
		}
		
		public String getName() {
			return name;
		}

		public long getAnswerAtDepth(int depth) {
			if(depth > answers.length) {
				return -1;
			}

			return answers[depth];
		}

		public String getFen() {
			return fen;
		}
		
		public int answerLength() {
			return answers.length;
		}
	}


	/**
	 * Start the perft search
	 * 
	 * @param DBitBoard
	 *            The DBitBoard to search
	 * @param depth
	 *            The depth to search to
	 * @param divide
	 *            Should we divide the first moves or just return the total
	 *            value
	 * @return number of nodes
	 */
	public static long perft(EngineBoard eBitBoard, int depth, boolean divide) {
		long nNodes;
		long zobrist = eBitBoard.getZobristKey();

		if (divide) {
			nNodes = divide(eBitBoard, depth);
		} else {
			nNodes = miniMax(eBitBoard, depth);
		}

		if (zobrist != eBitBoard.getZobristKey())
			System.out.println("Error in zobrist update!");

		return nNodes;

	}

	/**
	 * Keeps track of every starting move and its number of child moves, and
	 * then prints it on the screen.
	 * 
	 * @param DBitBoard
	 *            The position to search
	 * @param depth
	 *            The depth to search to
	 */
	private static long divide(EngineBoard dBitBoard, int depth) {
		int[] moves = new int[128];
		int totalMoves = dBitBoard.getMoveGenerator().fillLegalMoves(moves, 0);
		Long[] children = new Long[128];

		for (int i = 0; i < totalMoves; i++) {

			dBitBoard.makeMove(moves[i], false);
			children[i] = new Long(miniMax(dBitBoard, depth - 1));
			dBitBoard.undoLastMove();
		}

		long nodes = 0;
		for (int i = 0; i < totalMoves; i++) {
			System.out.print(PrintUtils.notation(moves[i]) + " ");
			System.out.println(((Long) children[i]).longValue());
			nodes += ((Long) children[i]).longValue();
		}

		System.out.println("Moves: " + totalMoves);
		return nodes;
	}

	/**
	 * Generates every move from the position on DBitBoard and returns the total
	 * number of moves found to the depth
	 * 
	 * @param DBitBoard
	 *            The DBitBoard used
	 * @param depth
	 *            The depth currently at
	 * @return int The number of moves found
	 */
	private static long miniMax(EngineBoard dBitBoard, int depth) {
		long nodes = 0;

		if (depth == 0)
			return 1;

		int[] moves = new int[128];
		int totalMoves = dBitBoard.getMoveGenerator().fillLegalMoves(moves, 0);
		
		for (int i = 0; i < totalMoves; i++) {
			dBitBoard.makeMove(moves[i], false);
			nodes += miniMax(dBitBoard, depth - 1);
			dBitBoard.undoLastMove();
		}

		return nodes;
	}

	/**
	 * Takes number and converts it to minutes, seconds and fraction of a second
	 * also includes leading zeros
	 * 
	 * @param millis
	 *            the Milliseconds to convert
	 * @return String the conversion
	 */
	public static String convertMillis(long millis) {
		long minutes = millis / 60000;
		long seconds = (millis % 60000) / 1000;
		long fracSec = (millis % 60000) % 1000;

		String timeString = "";

		// Add minutes to the string, if no minutes this part will not add to
		// the string
		if (minutes < 10 && minutes != 0)
			timeString += "0" + Long.toString(minutes) + ":";
		else if (minutes >= 10)
			timeString += Long.toString(minutes) + ":";

		// Add seconds to the string
		if (seconds == 0)
			timeString += "0";
		else if (minutes != 0 && seconds < 10)
			timeString += "0" + Long.toString(seconds);
		else if (seconds < 10)
			timeString += Long.toString(seconds);
		else
			timeString += Long.toString(seconds);

		timeString += ".";

		// Add fractions of a second to the string
		if (fracSec == 0)
			timeString += "000";
		else if (fracSec < 10)
			timeString += "00" + Long.toString(fracSec);
		else if (fracSec < 100)
			timeString += "0" + Long.toString(fracSec);
		else
			timeString += Long.toString(fracSec);

		return timeString;
	}
	
//	Correct answers
//	Pos 1
//	  Depth: 1 Answer: 20 (Correct) Time: 0.000
//	  Depth: 2 Answer: 400 (Correct) Time: 0.004
//	  Depth: 3 Answer: 8902 (Correct) Time: 0.080
//	  Depth: 4 Answer: 197281 (Correct) Time: 0.113
//	  Depth: 5 Answer: 4865609 (Correct) Time: 1.110
//	Pos 2
//	  Depth: 1 Answer: 48 (Correct) Time: 0.000
//	  Depth: 2 Answer: 2039 (Correct) Time: 0.000
//	  Depth: 3 Answer: 97862 (Correct) Time: 0.020
//	  Depth: 4 Answer: 4085603 (Correct) Time: 0.977
//	Pos 3
//	  Depth: 1 Answer: 50 (Correct) Time: 0.000
//	  Depth: 2 Answer: 279 (Correct) Time: 0.000
//	Pos 4
//	  Depth: 6 Answer: 38633283 (Correct) Time: 8.496
//	Pos 5
//	  Depth: 5 Answer: 11139762 (Correct) Time: 2.356
//	Pos 6
//	  Depth: 6 Answer: 11030083 (Correct) Time: 2.543
//	Total time: 15.705
//

}
