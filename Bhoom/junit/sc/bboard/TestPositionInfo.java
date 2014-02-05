package sc.bboard;

import static org.junit.Assert.*;

import org.junit.Test;

import sc.util.BitManipulation;
import sc.util.BoardUtils;
import sc.util.ParseUtils;

public class TestPositionInfo {

	@Test
	public void testPin() {
		EBitBoard board = new EBitBoard();
		String fen = "r1bqk2r/pp1n1pp1/2pbp1Qp/8/2pP2n1/2N1PN2/PP1B1P1P/1R2KB1R b Kkq - 1 10";
		BoardUtils.initializeBoard(board, fen);
		PositionInfo posInfo = board.getPositionInfo();
		int from = ParseUtils.getSquare("f7".toCharArray(), 0);
		int to = ParseUtils.getSquare("g6".toCharArray(), 0);
		long fromLoc = BitManipulation.bit_masks[from];
		long toLoc = BitManipulation.bit_masks[to];
		assertTrue((posInfo.bConfig.is_pinned & fromLoc) != 0L);
		assertTrue((posInfo.bConfig.pin_blocking_squares[from] & toLoc) != 0L);
		
	}
}
