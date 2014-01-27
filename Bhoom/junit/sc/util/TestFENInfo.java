package sc.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.util.FENInfo;

public class TestFENInfo {
	
	@Test
	public void testReadingFENSimple() {
		EBitBoard cb = new EBitBoard();
		String fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
		FENInfo fi = FENInfo.parse(fen);
		String rfen = fi.toFEN();
		
		assertEquals(fen, rfen);
		
	}
	
	@Test
	public void testReadingFENSimple2() {
		EBitBoard cb = new EBitBoard();
		String fen = "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28";
		FENInfo fi = FENInfo.parse(fen);
		String rfen = fi.toFEN();
		
		assertEquals(fen, rfen);
		
	}
	
}
