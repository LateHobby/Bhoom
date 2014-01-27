package sc.encodings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sc.encodings.Castling;
import sc.encodings.Encodings;


public class TestEncodings {

	
	@Test
	public void testCastlingRights() {
		int move = Encodings.encodeMove((short) 2, (short) 4, Encodings.WKING, false, true);
		assertTrue(Encodings.isCastling(move));
		
	}
	
	@Test
	public void testEncodeMove() {
		// nonsense move, but will do for testing
		int move = Encodings.encodeMove((short) 63, (short) 61, Encodings.WQUEEN, false, true);
		assertEquals(63, Encodings.getFromSquare(move));
		assertEquals(61, Encodings.getToSquare(move));
		assertEquals(Encodings.WQUEEN, Encodings.getPieceToPromoteTo(move));
		assertFalse(Encodings.isEnpassantCapture(move));
		assertTrue(Encodings.isCastling(move));
		
	}
}
