package sc.engine.engines;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.SearchEngine;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;

public class PositionalTests {

	public PositionalTests() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void testRepetitionDrawAcceptance() {
		String fen = "r1b5/ppqn2bk/3R2pp/2p2p2/2P1rN2/4BN2/PPQ2PPP/4R1K1 w - - 0 0";
		CTestEngine se = new CTestEngine("name", SearchMode.MTDF, new SideToMoveEvaluator(), new AlwaysReplace(), new SeeHashSorter());
		se.setFlags(true, true, true, true, true, true, true);
		EngineBoard board = new EBitBoard();
		BoardUtils.initializeBoard(board, fen);
		se.searchByDepth(board, 7);
	}
}
