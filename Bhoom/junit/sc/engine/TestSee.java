package sc.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.ParseUtils;
import sc.util.PrintUtils;

public class TestSee {

	@Test
	public void testScoringFunction() {
		See see = new See();
		assertEquals(-2, see.rootScore(new int[] { 2, 4 }, 2));
		assertEquals(2, see.rootScore(new int[] { 2, 4, 6 }, 3));
		assertEquals(-1, see.rootScore(new int[] { 2, 4, 1 }, 3));
		assertEquals(2, see.rootScore(new int[] { 2, 4, 6, 2 }, 4));
		assertEquals(1, see.rootScore(new int[] { 2, 4, 5, 2 }, 4));
//		PrintUtils.printAsArray(see.scores, 4);
	}
	
	@Test
	public void testEvaluation() {
		Evaluator eval = new SideToMoveEvaluator();
		See see = new See();
		EngineBoard board = new EBitBoard();
		String fen = "2q1r1k1/1ppb4/r2p1Pp1/p4n1p/2P1n3/5NPP/PP3Q1K/2BRRB2 w - - 0 0";
		BoardUtils.initializeBoard(board, fen);
		int move = ParseUtils.getMove("f6f7");
		assertEquals(-100, see.evaluateMove(board, move, eval));
		move = ParseUtils.getMove("e1e4");
		assertEquals(-200, see.evaluateMove(board, move, eval));
		fen = "7r/1p2k3/2bpp3/p3np2/P1PR4/2N2PP1/1P4K1/3B4 b - - 0 0";
		BoardUtils.initializeBoard(board, fen);
		move = ParseUtils.getMove("c6f3");
		assertEquals(-200, see.evaluateMove(board, move, eval));
		move = ParseUtils.getMove("e5g6");
		assertEquals(0, see.evaluateMove(board, move, eval));
	}
	
	@Test
	public void testPosition1() {
		Evaluator eval = new SideToMoveEvaluator();
		See see = new See();
		EngineBoard board = new EBitBoard();
		String fen = "rnbqkbnr/pp1ppppp/8/8/3p4/N7/PPP1PPPP/R1BQKBNR w KQkq - 0 3";
		int move = ParseUtils.getMove("d1d4");
		BoardUtils.initializeBoard(board, fen);
		assertEquals(100, see.evaluateMove(board, move, eval));
	}
	
	@Test
	public void testPosition2() {
		Evaluator eval = new SideToMoveEvaluator();
		See see = new See();
		EngineBoard board = new EBitBoard();
		String fen = "rnbqkbnr/ppp1pppp/8/3p4/6P1/7B/PPPPPP1P/RNBQK1NR b KQkq - 1 2";
		int move = ParseUtils.getMove("e8d7");
		BoardUtils.initializeBoard(board, fen);
		assertEquals(0, see.evaluateMove(board, move, eval));
	}
	
	@Test
	public void testPosition3() {
		Evaluator eval = new SideToMoveEvaluator();
		See see = new See();
		EngineBoard board = new EBitBoard();
		String fen = "r1bqkbnr/pp1ppppp/n7/2p5/2P1P1K1/8/PP1P1PPP/RNBQ1BNR b kq - 6 5";
		int move = ParseUtils.getMove("h7h5");
		BoardUtils.initializeBoard(board, fen);
		assertEquals(0, see.evaluateMove(board, move, eval));
	}
}
