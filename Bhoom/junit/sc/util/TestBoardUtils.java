package sc.util;

import static org.junit.Assert.*;

import org.junit.Test;

import sc.bboard.EBitBoard;

public class TestBoardUtils {

	@Test
	public void testMoveEncoding() {
		EBitBoard bb = new EBitBoard();
		String fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
		BoardUtils.initializeBoard(bb, fen);
		int move = BoardUtils.encodedForm(bb, "d7d5");
		assertEquals("d7d5", PrintUtils.notation(move));
	}
}
