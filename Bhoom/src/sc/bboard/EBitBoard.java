package sc.bboard;

import sc.SPCBoard;
import sc.encodings.Castling;
import sc.encodings.EConstants;
import sc.encodings.EZobrist;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.util.BitManipulation;
import sc.util.IntStack;
import sc.util.LongStack;
import sc.util.ObjStack;
import sc.util.ObjectPool;
import sc.util.ObjectPool.Factory;
import sc.util.Poolable;

public class EBitBoard implements EngineBoard {
	
	public PositionInfo posInfo = new PositionInfo();
	
//	public OneSidePositionInfo wConfig = new OneSidePositionInfo(true);
//	public OneSidePositionInfo bConfig = new OneSidePositionInfo(false);
	
	byte[] pieces = new byte[64];
	
	public long zobristKey = 0L;
	public long wpawnzobristkey = 0L;
	public long bpawnzobristkey = 0L;
	
	EMoveGenerator moveGen = new EMoveGenerator(this);
	
	GameState gameState;
	
	private IntStack moveHistory = new IntStack(2048);
	private ObjStack<GameState> stateHistory = new ObjStack<GameState>(512);
	private LongStack hashHistory = new LongStack(512);
	private LongStack pawnHashHistory = new LongStack(1024);
	
	private ObjectPool<GameState> statePool;
	private boolean positionInfoUpToDate;
	
	public EBitBoard() {
		zobristKey = 0L;
		wpawnzobristkey = 0L;
		bpawnzobristkey = 0L;
		statePool = new ObjectPool<GameState>(new Factory<GameState>() {

			@Override
			public GameState create() {
				return new GameState();
			}

			@Override
			public GameState[] getArray(int size) {
				return new GameState[size];
			} 
			
		}
		, 1024, "StatePool");
		gameState = statePool.allocate();
		
	}
	
	/**
	 * Sets the piece on the given square to be the given piece. Can be used to
	 * place a piece or remove a piece.
	 * 
	 * @param piece
	 * @param square
	 * @return The piece that was previously on that square
	 */
	private byte set(byte piece, short square, boolean saveHistory) {
		byte rv = pieces[square];
		pieces[square] = piece;
		if (rv != Encodings.EMPTY) {
			OneSidePositionInfo binfo = Encodings.isWhite(rv) ? posInfo.wConfig : posInfo.bConfig;
			binfo.remove(rv, square);
			long moveKey = EZobrist.getMoveKey(rv, square);
			zobristKey ^= moveKey;
			if (rv == Encodings.WPAWN) {
				wpawnzobristkey ^= moveKey;
			}
			if ( rv == Encodings.BPAWN) {
				bpawnzobristkey ^= moveKey;
			}
		}
		if (piece != Encodings.EMPTY) {
			OneSidePositionInfo binfo = Encodings.isWhite(piece) ? posInfo.wConfig : posInfo.bConfig;
			binfo.add(piece, square);
			long moveKey = EZobrist.getMoveKey(piece, square);
			zobristKey ^= moveKey;
			if (piece == Encodings.WPAWN) {
				wpawnzobristkey ^= moveKey;
			}
			if ( piece == Encodings.BPAWN) {
				bpawnzobristkey ^= moveKey;
			}
		}
		if (saveHistory) {
			int change = Change.encodeChange(rv, square);
			moveHistory.push(change);
		}
		return rv;
	}

	@Override
	public void initializeStandard() {
		initialize(EConstants.starting_position, 
				true, (short) 0, 0, 1, true, true, true, true);
	}

	@Override
	public void initialize(char[] locatedPieces,  boolean whiteToMove, short enPassantSquare,
			int halfMoveClock, int fullMoveNumber, 
			boolean castlingWKing, boolean castlingWQueen, boolean castlingBKing, boolean castlingBQueen) {
		for (int i = 0; i < 64; i++) {
			set(Encodings.EMPTY, (short) i, false);
		}
		
		moveHistory.clear();
		stateHistory.clear();
		hashHistory.clear();
		
		posInfo.wConfig.initialize();
		posInfo.bConfig.initialize();
		
		GameState oldGameState = gameState;
		gameState = statePool.allocate();
		gameState.whiteToMove = whiteToMove;
		gameState.enPassantSquare = enPassantSquare;
		gameState.halfMoveClock = halfMoveClock;
		gameState.fullMoveNumber = fullMoveNumber;
		gameState.cwKing = castlingWKing;
		gameState.cwQueen = castlingWQueen;
		gameState.cbKing = castlingBKing;
		gameState.cbQueen = castlingBQueen;
		gameState.whiteToMove = whiteToMove;
		
		updateGameStateHash(oldGameState, gameState);
		
		statePool.release(oldGameState);
		
		for (char c : locatedPieces) {
			byte piece = Encodings.getPiece(c);
			short square = Encodings.getSquare(c);
			set(piece, square, true);
		}
		
		updateDerivedBoards();
	}

	@Override
	public boolean makeMove(int move, boolean checkLegality) {
		
		if (checkLegality && !moveIsLegal(move)) {
			return false;
		}
//		ProfKey.makeMove.start();
		
		moveHistory.push(MOVEBORDER);
		stateHistory.push(gameState);
		hashHistory.push(zobristKey);
		pawnHashHistory.push(wpawnzobristkey);
		pawnHashHistory.push(bpawnzobristkey);
		
		short from = Encodings.getFromSquare(move);
		short to = Encodings.getToSquare(move);
		byte pieceToPromoteTo = Encodings.getPieceToPromoteTo(move);
		byte piece = pieces[from];
		byte captured = Encodings.EMPTY;
		boolean enPassantCapture = Encodings.isEnpassantCapture(move);
		boolean castling = Encodings.isCastling(move);
		
		if (castling) {
			makeCastlingMove(piece, from, to);
			
		} else if (enPassantCapture) {
			makeEnPassantMove(piece, from, to);
		} else { // regular move
			set(Encodings.EMPTY, from, true);
			captured = set(piece, to, true);
			if (Encodings.isPiece(pieceToPromoteTo)) {
				set(pieceToPromoteTo, to, true);
			}
		}
		if (captured == Encodings.WKING || captured == Encodings.BKING) {
			throw new RuntimeException("Can't capture king!");
		}

		updateGameState(from, to, piece, captured, castling);
		positionInfoUpToDate = false;
		
		return true;
	}


	@Override
	public void undoLastMove() {
		//		ProfKey.undoLastMove.start();
		int c = moveHistory.pop();
		while (c != MOVEBORDER) {
			byte piece = Change.getPiece(c);
			short square = Change.getSquare(c);
			set(piece, square, false);
			c = moveHistory.pop();
		} 

		statePool.release(gameState);
		
		gameState = stateHistory.pop();
		zobristKey = hashHistory.pop();
		bpawnzobristkey = pawnHashHistory.pop();
		wpawnzobristkey = pawnHashHistory.pop();
		

		positionInfoUpToDate = false;
		
		//		ProfKey.undoLastMove.stop();
	}

	@Override
	public void makeNullMove() {
		GameState oldGameState = statePool.allocate();
		oldGameState.setTo(gameState);
		gameState.whiteToMove = !gameState.whiteToMove;
		updateGameStateHash(oldGameState, gameState);
		statePool.release(oldGameState);
	}
	
	@Override
	public void undoNullMove() {
		makeNullMove();
	}

	

	@Override
	public boolean kingInCheck(boolean white) {
		updateDerivedBoards();
		return white ? posInfo.wConfig.kingInCheck : posInfo.bConfig.kingInCheck;
	}
	
	
	private void updateGameState(short from, short to, byte piece,
			byte capturedPiece, boolean castling) {
		// update state
		GameState oldGameState = gameState;
		gameState = statePool.allocate();
		boolean capture = Encodings.isPiece(capturedPiece);
		gameState.whiteToMove = !oldGameState.whiteToMove;
		if (piece == Encodings.WPAWN || piece == Encodings.BPAWN || capture) {
			gameState.halfMoveClock = 0;
		} else {
			gameState.halfMoveClock = oldGameState.halfMoveClock + 1;
		}
		if (!Encodings.isWhite(piece)) {
			gameState.fullMoveNumber = oldGameState.fullMoveNumber + 1;
		} else {
			gameState.fullMoveNumber = oldGameState.fullMoveNumber;
		}
		if ((piece == Encodings.WPAWN || piece == Encodings.BPAWN) && Math.abs(from - to) == 16) {
			// double push
			int sum = (int) from + to;
			gameState.enPassantSquare = (short) ((sum)/2);
		} else { 
			gameState.enPassantSquare = (short) 0;
		}
		gameState.cwKing = oldGameState.cwKing;
		gameState.cwQueen = oldGameState.cwQueen;
		gameState.cbKing = oldGameState.cbKing;
		gameState.cbQueen = oldGameState.cbQueen;
		
		if (piece == Encodings.WKING) {
			gameState.cwKing = gameState.cwQueen = false;
		}
		if (piece == Encodings.BKING) {
			gameState.cbKing = gameState.cbQueen = false;
		}
		
		if (piece == Encodings.WROOK) {
			if (from == EConstants.w_king_castle_rook_from) {
				gameState.cwKing = false;
			} else if (from == EConstants.w_queen_castle_rook_from) {
				gameState.cwQueen = false;
			}
		}
		if (piece == Encodings.BROOK) {
			if (from == EConstants.b_king_castle_rook_from) {
				gameState.cbKing = false;
			} else if (from == EConstants.b_queen_castle_rook_from) {
				gameState.cbQueen = false;
			}
		}
		if (capturedPiece == Encodings.WROOK) {
			if (to == EConstants.w_king_castle_rook_from) {
				gameState.cwKing = false;
			} else if (to == EConstants.w_queen_castle_rook_from) {
				gameState.cwQueen = false;
			}
		}
		if (capturedPiece == Encodings.BROOK) {
			if (to == EConstants.b_king_castle_rook_from) {
				gameState.cbKing = false;
			} else if (to == EConstants.b_queen_castle_rook_from) {
				gameState.cbQueen = false;
			}
		}
		
		updateGameStateHash(oldGameState, gameState);
	}

	private void updateGameStateHash(GameState olds, GameState news) {
		zobristKey ^= EZobrist.getStateKey(olds.whiteToMove, olds.enPassantSquare, olds.cwKing, olds.cwQueen, olds.cbKing, olds.cbQueen);
		zobristKey ^= EZobrist.getStateKey(news.whiteToMove, news.enPassantSquare, news.cwKing, news.cwQueen, news.cbKing, news.cbQueen);
	}

	private boolean makeEnPassantMove(byte piece, short from, short to) {
		short enpassantSquare = gameState.enPassantSquare;
		short capturePawnSquare = (short) (Encodings.isWhite(piece) ? enpassantSquare - 8 : 
			enpassantSquare + 8);
		set(Encodings.EMPTY, capturePawnSquare, true);
		set(Encodings.EMPTY, from, true);
		set(piece, to, true);
		return true;
		
	}

	private void makeCastlingMove(byte piece, short from, short to) {
		if (piece == Encodings.WKING) {
			if (from == EConstants.w_king_castle_from && to == EConstants.w_king_castle_to) {
				set(Encodings.EMPTY, EConstants.w_king_castle_from, true);
				set(Encodings.EMPTY, EConstants.w_king_castle_rook_from, true);
				set(Encodings.WKING, EConstants.w_king_castle_to, true);
				set(Encodings.WROOK, EConstants.w_king_castle_rook_to, true);
				return;
			} else if (from == EConstants.w_queen_castle_from && to == EConstants.w_queen_castle_to) {
				set(Encodings.EMPTY, EConstants.w_queen_castle_from, true);
				set(Encodings.EMPTY, EConstants.w_queen_castle_rook_from, true);
				set(Encodings.WKING, EConstants.w_queen_castle_to, true);
				set(Encodings.WROOK, EConstants.w_queen_castle_rook_to, true);
				return;
			}
		} else if (piece == Encodings.BKING) {
			if (from == EConstants.b_king_castle_from && to == EConstants.b_king_castle_to) {
				set(Encodings.EMPTY, EConstants.b_king_castle_from, true);
				set(Encodings.EMPTY, EConstants.b_king_castle_rook_from, true);
				set(Encodings.BKING, EConstants.b_king_castle_to, true);
				set(Encodings.BROOK, EConstants.b_king_castle_rook_to, true);
				return;
			} else if (from == EConstants.b_queen_castle_from && to == EConstants.b_queen_castle_to) {
				set(Encodings.EMPTY, EConstants.b_queen_castle_from, true);
				set(Encodings.EMPTY, EConstants.b_queen_castle_rook_from, true);
				set(Encodings.BKING, EConstants.b_queen_castle_to, true);
				set(Encodings.BROOK, EConstants.b_queen_castle_rook_to, true);
				return;
			}
		}
		
	}

	private boolean moveIsLegal(int move) {
		short from = Encodings.getFromSquare(move);
		byte piece = pieces[from];
		if (!Encodings.isWhite(piece) == gameState.whiteToMove) {
			return false;
		}
		return moveGen.isMoveLegal(move);
	}

	public void updateForEval() {
		// individual occupancies are already updated
		posInfo.updateForEval();
	}
	
	void updateDerivedBoards() {
		if (!positionInfoUpToDate) {
			posInfo.updateDerivedBoards();
			positionInfoUpToDate = true;
		}
	}

	@Override
	public MoveGenerator getMoveGenerator() {
		return moveGen;
	}


	@Override
	public boolean isWhiteSquare(short square) {
		int file = Encodings.getFile(square);
		int rank = Encodings.getRank(square);
		return (rank + file) % 2 == 1;
	}

	@Override
	public boolean isOffBoard(short square) {
		return Encodings.isOffBoard(square);
	}

	@Override
	public byte getPiece(short squareEncoding) {
		return pieces[squareEncoding];
	}

	@Override
	public PositionInfo getPositionInfo() {
		updateDerivedBoards();
		return posInfo;
	}
	
	@Override
	public long getZobristKey() {
		return zobristKey;
	}
	
	@Override
	public long getPawnZobristKey(boolean white) {
		return white ? wpawnzobristkey : bpawnzobristkey;
	}
	
	@Override
	public boolean getWhiteToMove() {
		return gameState.whiteToMove;
	}

	@Override
	public int getHalfMoveClock() {
		return gameState.halfMoveClock;
	}

	@Override
	public int getFullMoveNumber() {
		return gameState.fullMoveNumber;
	}

	@Override
	public short getEnPassantSquare() {
		return gameState.enPassantSquare;
	}

	@Override
	public boolean hasCastlingRights(int rights) {
		switch (rights) {
		case Castling.ALL:
			return (gameState.cwKing && gameState.cwQueen && gameState.cbKing && gameState.cbQueen);
		case Castling.W_KING:
			return gameState.cwKing;
		case Castling.W_QUEEN:
			return gameState.cwQueen;
		case Castling.B_KING:
			return gameState.cbKing;
		case Castling.B_QUEEN:
			return gameState.cbQueen;
			default:
				throw new RuntimeException("Unknown rights");
		}
	}

	@Override
	public SPCBoard getCopy() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * A change is encoded in an int (b3, b2, b1, b0) as follows:
	 * b0 = square
	 * b1 = piece
	 * @author Shiva
	 *
	 */
	private static class Change {
		static short getSquare(int change) {
			return (short) (change & BitManipulation.int_msb_masks[8]);
		}
		
		static byte getPiece(int change) {
			return (byte) ((change >>> 8) & BitManipulation.int_msb_masks[8]);
		}
		
		static int encodeChange(byte piece, short square) {
			int change = 0;
			change |= piece;
			change <<= 8;
			change |= square;
			return change;
		}
	}
	
	private static int MOVEBORDER = 0xFFFFFFFF;

	private static class GameState implements Poolable {
		short enPassantSquare;
		int halfMoveClock;
		int fullMoveNumber;
		boolean cwKing;
		boolean cwQueen;
		boolean cbKing;
		boolean cbQueen;
		boolean whiteToMove;
		
		@Override
		public void reset() {
			cwKing = cwQueen = cbKing = cbQueen = false;
			
		}
		public void setTo(GameState other) {
			enPassantSquare = other.enPassantSquare;
			halfMoveClock = other.halfMoveClock;
			fullMoveNumber = other.fullMoveNumber;
			cwKing = other.cwKing;
			cwQueen = other.cwQueen;
			cbKing = other.cbKing;
			cbQueen = other.cbQueen;
			whiteToMove = other.whiteToMove;
		}


	}

	@Override
	public boolean drawByRepetition() {
		return hashHistory.getCount(zobristKey) > 2;
	}

	
}
