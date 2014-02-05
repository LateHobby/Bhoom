package sc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import sc.SPCBoard;
import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.SearchEngine;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.testing.EngineFactory;
import sc.util.ObjectPool.Factory;

public class UCI {

	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	SearchEngine engine;
	EngineBoard board = new EBitBoard(); // Create a board on which we will be
	Factory<SearchEngine> factory;
	
	public UCI(Factory<SearchEngine> factory) {
		this.factory = factory;
		this.engine = factory.create();
	}
	
	public UCI(SearchEngine se) {
		this.engine = se;
	}

	// playing

	public void uci() throws IOException {
		
		board.initializeStandard(); // Start position

		// set in the settings file
		String openingLine = ""; // Holds the opening line so far
		int searchDepth = 0; // Initialize search depth
		int movetime = 0; // Initialize fixed time per move
		System.out.println("");
		System.out.println("id name " + engine.name());
		System.out.println("id author Shiva Chaudhuri");
		System.out.println("uciok");

		// This is the loop in which we look for incoming commands from Uci
		for (;;) {
			String command = reader.readLine(); // Receive the input

			if ("uci".equals(command)) {
				System.out.println("id name " + engine.name());
				System.out.println("id author Shiva Chaudhuri");
				System.out.println("uciok");

			}

			if ("isready".equals(command)) {
				System.out.println("readyok");

			}

			if ("quit".equals(command))
				System.exit(0);

			// A new game is starting, can be both from start and inserted
			// position
			if ("ucinewgame".equals(command)) {
				board = new EBitBoard();
				engine = factory.create();
				searchDepth = 0;
				movetime = 0;
			}

			// Using the UCI protocol we receive the moves by the opponent
			// in a 'position' string, this string comes with a FEN-string (or
			// states "startpos")
			// followed by the moves played on the board.
			//
			// The UCI protocol states that the position should be set on the
			// board
			// and all moves played
			if (command.startsWith("position")) {
				// Set the position on the board

				if (command.indexOf("startpos") != -1) // Start position
				{
					openingLine = ""; // Initialize opening line
					board.initializeStandard(); // Insert start position
				} else // Fen string
				{
					String fen = extractFEN(command);

					openingLine = "none"; // Make sure we can't receive a book
											// move
					if (!"".equals(fen)) // If fen == "" there was an error
											// in the position-string
					{
						FENInfo fi = FENInfo.parse(fen);
						BoardUtils.initializeBoard(board, fen);
						
					}
				}

				// Play moves if there are any

				String[] moves = extractMoves(command);

				if (moves != null) // There are moves to be played
				{
					openingLine = ""; // Get ready for new input
					for (int i = 0; i < moves.length; i++) {

						int moveToMake = receiveMove(moves[i], board);
						if (moveToMake == 0) {
							System.out
									.println("Error in position string. Move "
											+ moves[i] + " could not be found.");
						} else {
							board.makeMove(moveToMake, false); // Make the move
																// on the
						}
					}
				}

			}

			// The GUI has told us to start calculating on the position, if the
			// opponent made a move this will have been caught in the 'position'
			// string
			if (command.startsWith("go")) {
				int wtime = 0; // Initialize the times
				int btime = 0;
				int winc = 0;
				int binc = 0;

				// If infinite time, set the times to 'infinite'
				if ("infinite".equals(command.substring(3))) {
					wtime = 99990000;
					btime = 99990000;
					winc = 0;
					binc = 0;
				} else if ("depth".equals(command.substring(3, 8))) {
					try {
						searchDepth = Integer.parseInt(command.substring(9));
					} catch (NumberFormatException ex) {
						ex.printStackTrace();
					}
				} else if ("movetime".equals(command.substring(3, 11))) {
					try {
						movetime = Integer.parseInt(command.substring(12));
					} catch (NumberFormatException ex) {
						ex.printStackTrace();
					}
				} else // Extract the times since it's not infinite time
						// controls
				{
					String[] splitGo = command.split(" ");
					for (int goindex = 0; goindex < splitGo.length; goindex++) {

						try {
							if ("wtime".equals(splitGo[goindex]))
								wtime = Integer.parseInt(splitGo[goindex + 1]);
							else if ("btime".equals(splitGo[goindex]))
								btime = Integer.parseInt(splitGo[goindex + 1]);
							else if ("winc".equals(splitGo[goindex]))
								winc = Integer.parseInt(splitGo[goindex + 1]);
							else if ("binc".equals(splitGo[goindex]))
								binc = Integer.parseInt(splitGo[goindex + 1]);
						}

						// Catch possible errors so the engine doesn't crash
						// if the go command is flawed
						catch (ArrayIndexOutOfBoundsException ex) {
						} catch (NumberFormatException ex) {
						}
					}
				}

				// We now have the times so set the engine's time and increment
				// to whatever side he is playing (the side to move on the
				// board)

				int engineTime;
				int engineInc;
				int oppTime;
				int oppInc;
				if (board.getWhiteToMove()) {
					engineTime = wtime;
					engineInc = winc;
					oppTime = btime;
					oppInc = binc;
				} else {
					engineTime = btime;
					engineInc = binc;
					oppTime = wtime;
					oppInc = winc;
				}

				Continuation bestLine = null;
				bestLine = engine.search(board, searchDepth, engineTime,
						engineInc, movetime, oppTime, oppInc);
				if (bestLine.line[0] != 0) // We have found a move to make
				{
					board.makeMove(bestLine.line[0], false); // Make best move

					System.out.println("bestmove "
							+ (PrintUtils.notation(bestLine.line[0])));

				} else {
					System.out.println("Error: No move found");
					System.exit(1);
				}
			}
		}
	} // END uci()

	/**
	 * Used by uci mode
	 * 
	 * Extracts the fen-string from a position-string, do not call if the
	 * position string has 'startpos' and not fen
	 * 
	 * Throws 'out of bounds' exception so faulty fen string won't crash the
	 * program
	 * 
	 * @param position
	 *            The position-string
	 * @return String Either the start position or the fen-string found
	 */
	public static String extractFEN(String position)
			throws ArrayIndexOutOfBoundsException {

		String[] splitString = position.split(" "); // Splits the string at the
													// spaces

		String fen = "";
		if (splitString.length < 6) {
			System.out.println("Error: position fen command faulty");
		} else {
			fen += splitString[2] + " "; // Pieces on the board
			fen += splitString[3] + " "; // Side to move
			fen += splitString[4] + " "; // Castling rights
			fen += splitString[5] + " "; // En passant
			if (splitString.length >= 8) {
				fen += splitString[6] + " "; // Half moves
				fen += splitString[7]; // Full moves
			}
		}

		return fen;
	} // END extractFEN()

	/**
	 * Used by uci mode
	 * 
	 * Extracts the moves at the end of the 'position' string sent by the UCI
	 * interface
	 * 
	 * Originally written by Yves Catineau and modified by Jonatan Pettersson
	 * 
	 * @param parameters
	 *            The 'position' string
	 * @return moves The last part of 'position' that contains the moves
	 */
	private static String[] extractMoves(String position) {
		String pattern = " moves ";
		int index = position.indexOf(pattern);
		if (index == -1)
			return null; // No moves found

		String movesString = position.substring(index + pattern.length());
		String[] moves = movesString.split(" "); // Create an array with the
													// moves
		return moves;
	} // END extractMoves()

	/**
	 * Takes an inputted move-string and matches it with a legal move generated
	 * from the board
	 * 
	 * @param move
	 *            The inputted move
	 * @param board
	 *            The board on which to find moves
	 * @return int The matched move
	 */
	public static int receiveMove(String move, SPCBoard board)
			throws IOException {

		int[] legalMoves = new int[128];
		int totalMoves = board.getMoveGenerator().fillLegalMoves(legalMoves, 0); // All
																					// moves

		for (int i = 0; i < totalMoves; i++) {
			if (PrintUtils.notation(legalMoves[i]).equals(move)) {
				return legalMoves[i];
			}
		}

		// If no move was found return null
		return 0;
	}

	// END receiveMove()

	/**
	 * Starts a loop that checks for input, used for testing mainly, when no
	 * engine is calling for application (used in console window)
	 */
	public void lineInput() throws IOException {

		System.out.println("\nWelcome to SPCCHess "
				+ ". Type 'help' for commands.");
		System.out.print("\n->");
		for (;;) {
			String command = reader.readLine();
			if (command.equals("uci")) {
				uci();
				break;
			}

			if ("quit".equals(command))
				System.exit(0);

			else if ("help".equals(command)) {
				System.out.println("\nCommands:\n");
				System.out.println("quit           ->  Exit the program");
				System.out
						.println("setboard [fen] ->  Set to board to the fen string");
				System.out
						.println("perft [depth]  ->  Run a perft check to [depth]");
				System.out
						.println("divide [depth] ->  Run a divide check to [depth]");
				System.out
						.println("searchd [depth]->  Search the position to [depth]");
				System.out
						.println("searcht [time] ->  Search the position for [time] (in milliseconds)");
				System.out.println("eval           ->  Evaluates the position");

				System.out.print("\n->");
			}

			else if (command.startsWith("setboard ")) {
				FENInfo fi = FENInfo.parse(command.substring(9));
				board.initialize(fi.locatedPieces, fi.whiteToMove,
						fi.enPassantSquare, fi.halfMoveClock,
						fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing,
						fi.cbQueen);
				System.out.print("\n->");
			}

			else if (command.startsWith("perft ")) {
				try {
					if (Integer.parseInt(command.substring(6)) <= 0)
						System.out.println("Depth needs to be higher than 0.");
					else
						System.out.println(DPerft.perft(board,
								Integer.parseInt(command.substring(6)), false));
				} catch (NumberFormatException ex) {
					System.out.println("Depth needs to be an integer.");
				} catch (StringIndexOutOfBoundsException ex) {
					System.out.println("Please enter a search depth.");
				}
				System.out.print("\n->");

			} else if (command.startsWith("searchd ")) {
				try {
					if (Integer.parseInt(command.substring(8)) <= 0)
						System.out.println("Depth needs to be higher than 0.");
					else {
						long time = System.currentTimeMillis();
						engine.searchByDepth(board,
								Integer.parseInt(command.substring(8)));
						System.out.println("Time: "
								+ DPerft.convertMillis((System
										.currentTimeMillis() - time)));
					}
				} catch (NumberFormatException ex) {
					System.out.println("Depth needs to be an integer.");
				} catch (StringIndexOutOfBoundsException ex) {
					System.out.println("Please enter a search depth.");
				}
				System.out.print("\n->");
			} else if (command.startsWith("searcht ")) {
				try {
					if (Integer.parseInt(command.substring(8)) <= 0)
						System.out.println("Time needs to be higher than 0.");
					else {
						long time = System.currentTimeMillis();
						engine.searchByTime(board, 
								Integer.parseInt(command.substring(8)));
						System.out.println("Time: "
								+ DPerft.convertMillis((System
										.currentTimeMillis() - time)));
					}
				} catch (NumberFormatException ex) {
					System.out.println("Time needs to be an integer.");
				} catch (StringIndexOutOfBoundsException ex) {
					System.out.println("Please enter a time.");
				}
				System.out.print("\n->");
			} else if (command.startsWith("divide ")) {
				try {
					if (Integer.parseInt(command.substring(7)) <= 0)
						System.out.println("Depth needs to be higher than 0,");
					else
						System.out.println(DPerft.perft(board,
								Integer.parseInt(command.substring(7)), true));
				} catch (NumberFormatException ex) {
					System.out.println("Depth needs to be an integer.");
				} catch (StringIndexOutOfBoundsException ex) {
					System.out.println("Please enter a search depth.");
				}
				System.out.print("\n->");
			} else {
				System.out.println("Command not found.");
				System.out.print("\n->");
			}

		}
	}

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		SearchEngine 
		try {
			if (args.length >= 1) {
				SearchMode clz = null;
				EngineFactory ef = null;
				String engineClass = args[0];
				if (engineClass.startsWith("IDAsp")) {
					clz = SearchMode.ASP_WIN;
				} else if (engineClass.startsWith("MTDFBinary")) {
					clz = SearchMode.BIN_MTDF;
				} else if (engineClass.startsWith("MTDFEngine")) {
					clz = SearchMode.MTDF;
				} else if (engineClass.startsWith("MTDFHybrid")) {
					clz = SearchMode.HYBRID_MTDF;
				}
				if (args.length >= 2) {
					ef = new EngineFactory(clz, true, true, true, false, true, true, true);
				} else {
					ef = new EngineFactory(clz);
				}
				new UCI(ef).lineInput();
			} else {
				throw new RuntimeException("Incorrect number of args");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
