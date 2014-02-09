package sc.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sc.bboard.EBitBoard;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EngineStats;
import sc.engine.SearchEngine;
import sc.engine.SearchEngine.Continuation;
import sc.util.BoardUtils;
import sc.util.ObjectPool.Factory;
import sc.util.ParseUtils;
import sc.util.PrintUtils;

public class EPDTestingUtils {

	static public SuiteResult getTestResults(String filename, Factory<SearchEngine> engineFactory, EngineSetting setting) throws Exception {
		SuiteResult sr = new SuiteResult();
		sr.fileName = filename;
		sr.results = new ArrayList<TestResult>();
		sr.engineSetting = setting;
		sr.engineName = engineFactory.create().name();
		List<EPDTest> tests = getTestsFromFile(filename);
		System.out.println("NumTests:" + tests.size());
		EngineBoard board = new EBitBoard();
		for (EPDTest test : tests) {
			System.out.println("Starting test " + test.name + "   NumMoves:" + test.solution.length);
			TestResult result = getTestResult(engineFactory, setting, board,
					test);
			System.out.println("Finished test " + test.name);
			sr.results.add(result);
		}

		return sr;
	}

	public static TestResult getTestResult(Factory<SearchEngine> engineFactory,
			EngineSetting setting, EngineBoard board, EPDTest test)
			throws Exception {
		SearchEngine engine = engineFactory.create();
//			sr.engineName = engine.name();
		BoardUtils.initializeBoard(board, test.fen);
		TestResult result = new TestResult();
		result.testName = test.name;
		result.moves = new int[test.solution.length];
		result.passed = true;
		for (int i = 0; i < test.solution.length; i++) {
			Continuation e2 = null;
			long start = System.currentTimeMillis();
			if (setting.timeMs == 0) {
				e2 = engine.searchByDepth(board, setting.depth);
			} else {
				e2 = engine.searchByTime(board, setting.timeMs);
			}
			result.timeMs += System.currentTimeMillis() - start;
			EngineStats est = engine.getEngineStats();
			result.stats = (EngineStats) deepCopy((Serializable) est);
			result.engineComments += "";
			
			int move = e2.line[0];
			result.moves[i] = move;
			if (move != test.solution[i]) {
				result.passed = false;
			}
			board.makeMove(move, false);
			System.out.println("Finished move " + i);
		}
		return result;
	}
	
	static public List<EPDTest> getTestsFromFile(String filename) throws IOException {
		return getTestsFromFile(new File(filename));
	}
	
	static public List<EPDTest> getTestsFromFile(File file) throws IOException {
		List<EPDTest> tlist = new ArrayList<EPDTest>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		EngineBoard board = new EBitBoard();
		while ((line = br.readLine()) != null) {
			EPDTest test = parseTest(line, board);
			tlist.add(test);
		}
		br.close();
		
		return tlist;
	}
	
	static private EPDTest parseTest(String line, EngineBoard board) {
		String[] sa = line.split(";");
		String[] fa = sa[0].split("bm");
		String fen = fa[0] + "0 0";
		String name = "";
		int[] marr = new int[1];
		if (fa.length > 1) {
			String moves = fa[1];
			name = sa[1].substring(sa[1].indexOf("\"") + 1, sa[1].lastIndexOf("\""));
			marr = parseMoves(moves, fen, board);
		}
		return new EPDTest(name, fen, marr);
	}

	static private int[] parseMoves(String moves, String fen, EngineBoard board) {
		moves = moves.trim();
		BoardUtils.initializeBoard(board, fen);
		String[] ma = moves.split("\\s+");
		if (ma.length == 0) {
			return new int[1];
		}
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

	static private char getFromFile(char[] ca) {
		return ' ';
	}

	static private byte getPieceMoving(char[] ca, EngineBoard board) {
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

	static private int findToSquareIndex(char[] ca) {
		for (int i = 0; i < ca.length-1; i++) {
			if ('a' <= ca[i] && ca[i] <= 'h') {
				if ('1' <= ca[i+1] && ca[i+1] <= 8) {
					return i;
				}
			}
		}
		return -1;
	}

	static public Object deepCopy(Serializable oldObj) throws Exception
	   {
	      ObjectOutputStream oos = null;
	      ObjectInputStream ois = null;
	      try
	      {
	         ByteArrayOutputStream bos = 
	               new ByteArrayOutputStream(); // A
	         oos = new ObjectOutputStream(bos); // B
	         // serialize and pass the object
	         oos.writeObject(oldObj);   // C
	         oos.flush();               // D
	         ByteArrayInputStream bin = 
	               new ByteArrayInputStream(bos.toByteArray()); // E
	         ois = new ObjectInputStream(bin);                  // F
	         // return the new object
	         return ois.readObject(); // G
	      }
	      catch(Exception e)
	      {
	         System.out.println("Exception in ObjectCloner = " + e);
	         throw(e);
	      }
	      finally
	      {
	         oos.close();
	         ois.close();
	      }
	   }
	static public class EPDTest {
		public String name;
		public String fen;
		public int[] solution;
		public int eval;
		
		public EPDTest(String name, String fen, int[] solution) {
			super();
			this.name = name;
			this.fen = fen;
			this.solution = solution;
		}
		
		
	}
	
	static public class SuiteResult implements Serializable {
		
		private static final long serialVersionUID = 3924208089197960476L;
		public String fileName;
		public List<TestResult> results;
		public String engineName;
		public EngineSetting engineSetting;
		
	}
	
	
	static public class TestResult implements Serializable {


		private static final long serialVersionUID = 990665124325939021L;
		
		public String testName;
		public EngineStats stats;
		
		public long timeMs;
		public int [] moves;
		boolean passed;
		public String engineComments;
		@Override
		public String toString() {
			return testName + " " + passed + " " + timeMs + ", " + (stats.getNodes(false) + stats.getNodes(true)) 
					+ ", " + stats.getDepth() + ", " + PrintUtils.notation(moves[0]);
		}
		
		
		
	}

	static public class EngineSetting  implements Serializable {
		private static final long serialVersionUID = 4503904925088347236L;
		public int depth;
		public int timeMs;
		@Override
		public String toString() {
			return "Depth: " + depth + "  Time: " + timeMs;
		}
		
	}
	

}
