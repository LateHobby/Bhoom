package sc.bboard;

import static org.junit.Assert.*;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.encodings.Castling;
import sc.encodings.Encodings;
import sc.util.BoardUtils;
import sc.util.FENInfo;
import sc.util.ParseUtils;
import sc.util.PrintUtils;

public class TestEBitBoard {

	
	@Test
	public void testInitialize() {
		EBitBoard cb = new EBitBoard();
		cb.initializeStandard();
		String startfen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		assertEquals(startfen, BoardUtils.getFen(cb));
	}
	
	@Test
	public void testGameState() {
		EBitBoard cb = new EBitBoard();
		assertEquals(0, cb.getFullMoveNumber());
		cb.initializeStandard();
		assertEquals(1, cb.getFullMoveNumber());
	}

	@Test
	public void testFEN() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		assertEquals(1, fi.fullMoveNumber);

		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);

		String rfen = BoardUtils.getFen(cb);
		assertEquals(fen, rfen);

	}

	@Test
	public void testUndo() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int[] moves = new int[128];
		int num = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		cb.makeMove(moves[0], false);
		cb.undoLastMove();

		String rfen = BoardUtils.getFen(cb);
		assertEquals(fen, rfen);

	}

	@Test
	public void testHashComputation() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		long zobrist = cb.zobristKey;
		int[] moves = new int[128];
		int num = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		cb.makeMove(moves[0], false);
		cb.undoLastMove();

		assertEquals(zobrist, cb.zobristKey);
		String rfen = BoardUtils.getFen(cb);
		assertEquals(fen, rfen);

	}

	@Test
	public void testPinningMove() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/1ppppppp/8/p7/Q7/2P5/PP1PPPPP/RNB1KBNR b KQkq - 1 2";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		
		int secondMove = BoardUtils.encodeMove(cb, "d7d5");
		int[] moves = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		assertFalse(cb.getMoveGenerator().isMoveLegal(secondMove));

	}
	
	@Test
	public void testPinningMove2() {
		EBitBoard cb = new EBitBoard();
		String fen = "1n1b4/1PRP4/1KP5/8/8/6k1/8/8 w - - 1 2 ";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		
		int secondMove = BoardUtils.encodeMove(cb, "c7c8");
		int[] moves = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		assertFalse(cb.getMoveGenerator().isMoveLegal(secondMove));

	}

//	1n1b4/1PRP4/1K6/8/8/6k1/8/8 w - - 1 2 
	
	@Test
	public void testCheckBlockingMoves() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "a2a3");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "e7e5");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "d2d3");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "f8b4");
		cb.makeMove(move, false);
		// the only moves that remove the check
		int failingMove;
		failingMove = BoardUtils.encodeMove(cb, "a3b4");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));
		failingMove = BoardUtils.encodeMove(cb, "c2c3");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));
		failingMove = BoardUtils.encodeMove(cb, "c1d2");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));
		failingMove = BoardUtils.encodeMove(cb, "d1d2");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));
		failingMove = BoardUtils.encodeMove(cb, "b1c3");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));
		failingMove = BoardUtils.encodeMove(cb, "b1d2");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));

		int numMoves = cb.getMoveGenerator().fillLegalMoves(new int[128], 0);
		assertEquals(6, numMoves);
	}

	@Test
	public void testCheckBlockingMoves2() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/ppppp1pp/5p2/7Q/8/4P3/PPPP1PPP/RNB1KBNR b KQkq - 1 2";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);

		// the only moves that remove the check
		int failingMove;
		failingMove = BoardUtils.encodeMove(cb, "g7g6");
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));

		int numMoves = cb.getMoveGenerator().fillLegalMoves(new int[128], 0);
		assertEquals(1, numMoves);

	}

	@Test
	public void testCheckEvasion() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "d2d3");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "g8f6");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "e1d2");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "f6e4");
		cb.makeMove(move, false);
		int failingMove = BoardUtils.encodeMove(cb, "a2a3");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));

	}

	@Test
	public void testCheckEvasion2() {
		EBitBoard cb = new EBitBoard();
		String startFen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(startFen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = ParseUtils.getMove("e1g1", false, true);
		cb.makeMove(move, false);
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R4RK1 b kq - 1 1";
		assertEquals(fen, BoardUtils.getFen(cb));
		move = ParseUtils.getMove("e8c8", false, true);
		cb.makeMove(move, false);
		fen = "2kr3r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R4RK1 w - - 2 2";
		assertEquals(fen, BoardUtils.getFen(cb));
		move = BoardUtils.encodeMove(cb, "e2a6");
		cb.makeMove(move, false);
		fen = "2kr3r/p1ppqpb1/Bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPB1PPP/R4RK1 b - - 0 2";
		assertEquals(fen, BoardUtils.getFen(cb));
		
		int failingMove = BoardUtils.encodeMove(cb, "d8e8");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));

	}
	
	@Test
	public void testCheckEvasion3() {
		EBitBoard cb = new EBitBoard();
		String startFen = "8/7p/p5pb/4k3/P3n3/3p4/P5PP/Rr4K1 w - - 2 31";
		FENInfo fi = FENInfo.parse(startFen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int failingMove = BoardUtils.encodeMove(cb, "g1h1");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));
	}
	
	@Test
	public void testCapturingCheckingPiece() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/1n2pnp1/3PN3/1p2P3/2N2Q1p/PPPBbPPP/R2K3R w kq - 0 2";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		
		int failingMove = BoardUtils.encodeMove(cb, "e5g4");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));
		int move = BoardUtils.encodeMove(cb, "c3e2");
		
		assertTrue(cb.getMoveGenerator().isMoveLegal(move));
	}
	
	@Test
	public void testKingCapturingCheckingPiece() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q2/PPPBBPpP/R4K1R w kq - 0 2";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		
		int move = BoardUtils.encodeMove(cb, "f3g2");
		assertTrue(cb.getMoveGenerator().isMoveLegal(move));
	}
	
	@Test
	public void testBlackQueensideCastling() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "a1b1");
		cb.makeMove(move, false);

		int failingMove = BoardUtils.encodeMove(cb, "e8c8");
		// add castling
		int actualMove = Encodings.encodeMove(
				Encodings.getFromSquare(failingMove),
				Encodings.getToSquare(failingMove), Encodings.EMPTY, false,
				true);
		assertTrue(cb.getMoveGenerator().isMoveLegal(actualMove));

	}

	@Test
	public void testCastlingOverAttackedSquares() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "e5c6");
		cb.makeMove(move, false);

		int failingMove = BoardUtils.encodeMove(cb, "e8c8");
		// add castling
		int actualMove = Encodings.encodeMove(
				Encodings.getFromSquare(failingMove),
				Encodings.getToSquare(failingMove), Encodings.EMPTY, false,
				true);
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));

	}

	@Test
	public void testCastlingOverOccupiedSquares() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/1n2pnp1/3PN3/1p2P3/2N2Q1p/PPPB1PPP/R2BKb1R w KQkq - 2 2";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		

		int failingMove = BoardUtils.encodeMove(cb, "e1g1");
		// add castling
		int actualMove = Encodings.encodeMove(
				Encodings.getFromSquare(failingMove),
				Encodings.getToSquare(failingMove), Encodings.EMPTY, false,
				true);
		int[] moves = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));

	}
	@Test
	public void testCastlingOverUnattackedSquares() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "e5d7");
		cb.makeMove(move, false);

		int failingMove = BoardUtils.encodeMove(cb, "e8c8");
		// add castling
		int actualMove = Encodings.encodeMove(
				Encodings.getFromSquare(failingMove),
				Encodings.getToSquare(failingMove), Encodings.EMPTY, false,
				true);
		assertTrue(cb.getMoveGenerator().isMoveLegal(actualMove));

	}

	@Test
	public void testPositionAfterEnPassantCapture() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "a2a4");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "b4a3");
		move = Encodings.encodeMove(Encodings.getFromSquare(move),
				Encodings.getToSquare(move), Encodings.EMPTY, true, false);
		cb.makeMove(move, false);
		String targetFEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/4P3/p1N2Q1p/1PPBBPPP/R3K2R w KQkq - 0 2";
		String cfen = BoardUtils.getFen(cb);
		assertEquals(targetFEN, cfen);

	}

	@Test
	public void testPromotion() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "a1b1");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "f6e4");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "f3f7");
		cb.makeMove(move, false);

		int failingMove = BoardUtils.encodeMove(cb, "e8c8");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));

	}

	@Test
	public void testCastlingRights() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "f3h3");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "b4b3");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "h3h8");
		cb.makeMove(move, false);

		String tfen = "r3k2Q/p1ppqpb1/bn2pnp1/3PN3/4P3/1pN5/PPPBBPPP/R3K2R b KQq - 0 2";

		String cfen = BoardUtils.getFen(cb);
		assertEquals(tfen, cfen);

	}

	@Test
	public void testCastlingRights2() {
		EBitBoard cb = new EBitBoard();
		String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int[] marr = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(marr, 0);
		int move = 0;

		for (int i = 0; i < numMoves; i++) {
			if (PrintUtils.notation(marr[i]).equals("e1c1")) {
				move = marr[i];
			}
		}
		cb.makeMove(move, false);

		String mfen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/2KR3R b kq - 1 1";
		String cfen = BoardUtils.getFen(cb);
		assertEquals(mfen, cfen);

		assertTrue(cb.hasCastlingRights(Castling.B_KING));
		assertTrue(cb.hasCastlingRights(Castling.B_QUEEN));
	}


	@Test
	public void testEnpassantCapture() {
		EBitBoard cb = new EBitBoard();
		String fen = "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);

		assertEquals(Encodings.EMPTY, cb.getPiece((short) 23));
		String cfen = BoardUtils.getFen(cb);
		assertEquals(fen, cfen);

		int failingMove = BoardUtils.encodeMove(cb, "c4d3");
		// add en-passant property
		int actualMove = Encodings.encodeMove(
				Encodings.getFromSquare(failingMove),
				Encodings.getToSquare(failingMove), Encodings.EMPTY, true,
				false);
		assertTrue(cb.getMoveGenerator().isMoveLegal(actualMove));
	}

	@Test
	public void testEnpassantCaptureCausesCheck() {
		EBitBoard cb = new EBitBoard();
		String fen = "8/2p5/3p4/KP6/1R2Pp1k/8/6P1/2r5 b - e3 0 3";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);

		int[] marr = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(marr, 0);

		for (int i = 0; i < numMoves; i++) {
			if (PrintUtils.notation(marr[i]).equals("f4e3")) {
				System.out.println("Found");
			}
		}
		
		int failingMove = ParseUtils.getMove("f4e3", true, false);
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));
	}
	
	@Test
	public void testPromotionMove() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int move = BoardUtils.encodeMove(cb, "e5e6");
		assertTrue("BoardUtils returned " + move, ParseUtils.getMove("e5e6", false, false) == move);
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "f5f4");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "e6d7");
		cb.makeMove(move, false);
		move = BoardUtils.encodeMove(cb, "e8f7");
		cb.makeMove(move, false);

		int failingMove = BoardUtils.encodeMove(cb, "d7c8Q");
		assertTrue("BoardUtils returned " + failingMove, ParseUtils.getMove("d7c8Q", false, false) == failingMove);
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));

	}

	@Test
	public void testFirstMove() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		int failingMove = BoardUtils.encodeMove(cb, "a2a3");
		int[] moves = new int[128];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moves, 0);
		assertTrue(cb.getMoveGenerator().isMoveLegal(failingMove));

	}

	@Test
	public void testCheckMate() {
		EBitBoard cb = new EBitBoard();
		String fen = "rnbqkbnr/ppppp2p/5p2/6pQ/8/4P2P/PPPP1PP1/RNB1KBNR b KQkq - 1 3";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);

		int[] moveArr = new int[100];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moveArr, 0);
		if (numMoves > 0) {
			PrintUtils.printMoves(moveArr, 10);
		}
		assertEquals(0, numMoves);
	}

	@Test
	public void testKingCapturingProtectedPiece1() {
		EBitBoard cb = new EBitBoard();
		cb.initializeStandard();
		makeMoves(cb, "d2d4", "c7c5", "d1d2", "d8a5", "f2f3", "e8d8", "d2a5",
				"d8e8", "e1d2", "b8a6", "d4d5", "a8b8", "d5d6", "b8a8", "a5a3",
				"e8d8", "d6e7", "d8c7", "e7f8Q", "c7b6", "a3c5");

		int failingMove = BoardUtils.encodeMove(cb, "b6c5");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));
	}

	@Test
	public void testKingCapturingProtectedPiece2() {
		EBitBoard cb = new EBitBoard();
		cb.initializeStandard();
		makeMoves(cb, "d2d4", "g8h6", "c1g5", "h8g8", "d1d3", "g8h8", "d3e3",
				"h8g8", "g5e7");

		int failingMove = BoardUtils.encodeMove(cb, "e8e7");
		assertFalse(cb.getMoveGenerator().isMoveLegal(failingMove));
	}

	@Test
	public void testStalemate() {
		EBitBoard cb = new EBitBoard();
		String fen = "7k/5K2/6Q1/8/8/8/8/8 b - - 7 1";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		 
		int[] moveArr = new int[100];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moveArr, 0);
		if (numMoves > 0) {
			PrintUtils.printMoves(moveArr, 10);
		}
		assertEquals(0, numMoves);
		

	}
// 
	
	@Test
	public void testNoStalemate() {
		EBitBoard cb = new EBitBoard();
		String fen = "2q1r1k1/1ppb4/r2p1Pp1/p4n1p/2P1n3/5NPP/PP3Q1K/2BRRB2 w - - 0 0";
		FENInfo fi = FENInfo.parse(fen);
		cb.initialize(fi.locatedPieces, fi.whiteToMove, fi.enPassantSquare, fi.halfMoveClock, fi.fullMoveNumber, fi.cwKing, fi.cwQueen, fi.cbKing, fi.cbQueen);
		 
		int[] moveArr = new int[100];
		int numMoves = cb.getMoveGenerator().fillLegalMoves(moveArr, 0);
		if (numMoves > 0) {
			PrintUtils.printMoves(moveArr, 10);
		}
		assert(0 != numMoves);
		

	}
	
	@Test
	public void testQueenCapture() {
		EBitBoard cb = new EBitBoard();
		String fen = "r1bqk2r/pp1n1pp1/2pbp1Qp/8/2pP2n1/2N1PN2/PP1B1P1P/1R2KB1R b Kkq - 1 10";
		BoardUtils.initializeBoard(cb, fen);
		int move = BoardUtils.encodedForm(cb, "f7g6");
		boolean isLegal = cb.makeMove(move, true);
		assertTrue(isLegal);

	}
	
	@Test
	public void test3MoveDrawRecognition() {
		EBitBoard board = new EBitBoard();
		String fen = "6k1/2p5/2n1p2p/4P1p1/1p3nP1/3pKP2/rB6/3N4 b - - 0 35";
		BoardUtils.initializeBoard(board, fen);
		String[] moves = {"b4b3", "e3e4", "a2a4", "e4e3", "a4a2", "e3e4", "a2a4", "e4e3", "a4a2"};
		int i = 0;
		for (String moveS : moves) {
			int move = BoardUtils.encodedForm(board, moveS);
			assertTrue("Failed on " + i + ". " + moveS , board.makeMove(move, true));
//			System.out.println(BoardUtils.getFen(board));
//			System.out.println("" + i + ". " + board.getZobristKey());
			i++;
		}
		
		assertTrue("Draw Not recognized" , board.drawByRepetition());

	}
	
	@Test
	public void testInsufficientMaterialDrawRecognition() {
		EBitBoard board = new EBitBoard();
		String fen = "8/8/2k2K2/8/8/8/8/8 b - - 0 53";
		BoardUtils.initializeBoard(board, fen);
		
		
		assertTrue("Draw Not recognized" , board.drawByInsufficientMaterial());

	}
	
	private void makeMoves(EBitBoard cb, String... moves) {
		for (String ms : moves) {
			int move = BoardUtils.encodeMove(cb, ms);
			cb.makeMove(move, false);
		}

	}

}
