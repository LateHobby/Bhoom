package sc.performance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.SearchEngine;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.engines.CTestEngine;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.ParseUtils;

public class TestSuites {

	File DIR = new File(".");
	
	@Test 
	public void runECM() throws IOException {
		File epd = new File(DIR, "ECM.EPD");
		Benchmark bm = new Benchmark("Encyclopedia of Chess Middlegames", epd, null);
		bm.run();
	}
	
	
	private SearchEngine getEngine() {
		return new CTestEngine("test", SearchMode.ASP_WIN, 200, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
	}
	
	private class Benchmark {
		
		private String name;
		private File epdFile;
		private File resultsFile;

		Benchmark(String name, File epdFile, File resultsFile) {
			this.name = name;
			this.epdFile = epdFile;
			this.resultsFile = resultsFile;
		}
		
		public void run() throws IOException {
			Map<String, EPDTest> tests = new HashMap<String, EPDTest>();
			BufferedReader br = new BufferedReader(new FileReader(epdFile));
			String line = null;
			EngineBoard board = new EBitBoard();
			while ((line = br.readLine()) != null) {
				EPDTest test = parseTest(line, board);
				tests.put(test.name, test);
			}
			br.close();
			board = new EBitBoard();
			boolean result = true;
			for (EPDTest test : tests.values()) {
				BoardUtils.initializeBoard(board, test.fen);
				SearchEngine engine = getEngine();
				for (int i = 0; i < test.solution.length; i++) {
					Continuation e2 = engine.searchByTime(board, 20000);
					int move = e2.line[0];
					if (move != test.solution[i]) {
						result = false;
						break;
					}
				}
				System.out.println(test.name + (result ? "Passed" : "Failed"));
			}
			
		}
		
		private EPDTest parseTest(String line, EngineBoard board) {
			String[] sa = line.split(";");
			String[] fa = sa[0].split("bm");
			String fen = fa[0] + "0 0";
			String moves = fa[1];
			String name = sa[1].substring(sa[1].indexOf("\"") + 1, sa[1].lastIndexOf("\""));
			int[] marr = parseMoves(moves, fen, board);
			return new EPDTest(name, fen, marr);
		}

		private int[] parseMoves(String moves, String fen, EngineBoard board) {
			BoardUtils.initializeBoard(board, fen);
			String[] ma = moves.split("\\s+");
			int[] results = new int[ma.length];
			int rindex = 0;
			for (String pm : ma) {
				pm = pm.trim();
				char[] ca = pm.toCharArray();
				short toSquare = -1;
				byte piece = Encodings.EMPTY;
				char fromFile = ' ';
				int squareIndex = findToSquareIndex(ca);
				if (squareIndex >= 0) {
					toSquare = ParseUtils.getSquare(ca, squareIndex);
					piece = getPieceMoving(ca, board);
					fromFile = getFromFile(ca);
					int[] marr = new int[128];
					int numMoves = board.getMoveGenerator().fillLegalMoves(marr, 0);
					for (int i = 0; i < numMoves; i++) {
						short from = Encodings.getFromSquare(marr[i]);
						byte moving = board.getPiece(from);
						if (toSquare == Encodings.getToSquare(marr[i]) &&
								moving == piece) {
							if (fromFile == ' ') {
								results[rindex] = marr[i];
								break;
							} else {
								int file = Encodings.getFile(from);
								if (file == (fromFile - 'a')) {
									results[rindex] = marr[i];
									break;
								}
							}
						}
						rindex++;
					}
				} else { // castling
					
				}
				
			}
			return results;
		}

		private char getFromFile(char[] ca) {
			return ' ';
		}

		private byte getPieceMoving(char[] ca, EngineBoard board) {
			boolean white = board.getWhiteToMove();
			for (int i = 0; i < ca.length-1; i++) {
				if (Character.isUpperCase(ca[i])) {
					switch (ca[i]) {
					case 'K' : return white ? Encodings.WKING : Encodings.BKING;
					case 'Q' : return white ? Encodings.WQUEEN : Encodings.BQUEEN;
					case 'R' : return white ? Encodings.WROOK : Encodings.BROOK;
					case 'B' : return white ? Encodings.WBISHOP : Encodings.BBISHOP;
					case 'N' : return white ? Encodings.WKNIGHT : Encodings.BKNIGHT;
					default:
						throw new RuntimeException("Unknown piece:" + ca[i]);
					}
				}
			}
			return white? Encodings.WPAWN : Encodings.BPAWN;
		}

		private int findToSquareIndex(char[] ca) {
			for (int i = 0; i < ca.length-1; i++) {
				if ('a' <= ca[i] && ca[i] <= 'h') {
					if ('1' <= ca[i+1] && ca[i+1] <= 8) {
						return i;
					}
				}
			}
			return -1;
		}

		private class EPDTest {
			String name;
			String fen;
			int[] solution;
			
			public EPDTest(String name, String fen, int[] solution) {
				super();
				this.name = name;
				this.fen = fen;
				this.solution = solution;
			}
			
			
		}
	}
}
