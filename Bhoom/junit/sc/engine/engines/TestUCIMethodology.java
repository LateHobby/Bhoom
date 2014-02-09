package sc.engine.engines;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.util.BoardUtils;
import sc.util.ExternalUCIEngine;

public class TestUCIMethodology {

	static ExternalUCIEngine uci;
	static {
		try {
			uci = new ExternalUCIEngine(
					"C:\\Program Files (x86)\\BabasChess\\Engines\\toga\\togaII.exe");
			uci.startup();
			uci.send("setoption name MultiPV value 1");
			uci.send("setoption name Ponder value false");
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	EngineBoard board = new EBitBoard();
	
	@Test
	public void test1() {
		String fen = "8/k7/p7/3Qp2P/n1P5/3KP3/1q6/8 b - - 0 0";
		String goodMove = "b2b1";
		String badMove = "b2c2";
		testMoves(goodMove, badMove, 6, fen);
		
		
	}
	
	
	private void testMoves(String goodMove, String badMove, int depth, String fen) {
		BoardUtils.initializeBoard(board, fen);
		int move1 = BoardUtils.encodeMove(board, goodMove);
		int move2 = BoardUtils.encodeMove(board, badMove);
		
		int goodEval = uci.evaluateMove(board, move1, depth, 0);
		int badEval = uci.evaluateMove(board, move2, depth, 0);
		
		assertTrue(String.format("GoodEval=%d  BadEval=%d",  goodEval, badEval),
				badEval < goodEval);
	}
	
}
