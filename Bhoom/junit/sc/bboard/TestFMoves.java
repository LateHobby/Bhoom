package sc.bboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sc.bboard.FMoves;
import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.util.BitManipulation;
import sc.util.TestUtils;

public class TestFMoves {

	@Test
	public void testPawnPushes() {
		
		long pawnOcc = EConstants.ranks[8];
		long occ = 0L;
		long pushes = FMoves.pawnPushes(pawnOcc, occ , true);
		
		long expected = EConstants.ranks[16] | EConstants.ranks[24];
		assertEquals(expected, pushes);
	}
	
	@Test
	public void testDoubledPawnPushes() {
		
		long pawnOcc = TestUtils.setSquares(0L, "h2", "h3");
		long pushes = FMoves.pawnPushes(pawnOcc, pawnOcc , true);
		
		long expected = TestUtils.setSquares(0L, "h4");
		assertEquals(expected, pushes);
	}
	
	@Test
	public void testPawnAttacks() {
		
		long pawnOcc = EConstants.ranks[8];
		long occ = 0L;
		long pushes = FMoves.pawnAttacks(pawnOcc, true);
		
		long expected = EConstants.ranks[16];
		assertEquals(expected, pushes);
	}
	
	@Test
	public void testPinComputation() {
		long selfOcc = 0L;
		short kingSquare = 60;
		long kingLoc = BitManipulation.bit_masks[kingSquare];
		char attacker = Encodings.encodeLocatedPiece(Encodings.WQUEEN, (short) 24);
		long pinPreservingSquares = FMoves.checkBlockingSquares(Encodings.WQUEEN, (short) 24, kingSquare, kingLoc, selfOcc);
		long expected = TestUtils.setSquares(0L, "b5", "c6", "d7");
		assertEquals(expected, pinPreservingSquares);
		selfOcc = TestUtils.setSquares(0L, "b5");
		pinPreservingSquares = FMoves.checkBlockingSquares(Encodings.WBISHOP, (short) 24, kingSquare, kingLoc, selfOcc);
		assertEquals(0L, pinPreservingSquares);
	}
	
	@Test
	public void testEnPassant() {
		assertTrue(FMoves.canBeCapturedEnPassant(Encodings.BPAWN, true, (short) 23, (short) 31)); 
		assertTrue(FMoves.canBeCapturedEnPassant(Encodings.WPAWN, false, (short) 31, (short) 23)); 
	}
}
