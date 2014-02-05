package sc.evaluators;

import sc.bboard.EBitBoard;
import sc.bboard.OneSidePositionInfo;
import sc.bboard.PositionInfo;
import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.util.BitManipulation;

/**
 * Always evaluates from white's perspective. Higher is better for white.
 * @author Shiva
 *
 */
public class WhiteEvaluator extends AbstractEvaluator {

	private static PositionInfo initPositionInfo;
	private static int[] staticPieceWeights = new int[]{800, 500, 300, 300, 100};
	private static int[] initPieceCount = new int[]{1, 2, 2, 2, 8};
	
	
	
	static {
		EBitBoard bb = new EBitBoard();
		bb.initializeStandard();
		initPositionInfo = bb.getPositionInfo();
	}
	
	int[] components = new int[evalNames.length];
	
	public void setWeights(int...weights) {
		int index = 0;
		for (int wt : weights) {
			fnWeights[index++] = wt;
		}
		
	}
	
	@Override
	public int[] evalComponents(EngineBoard board) {
		boolean whiteToMove = board.getWhiteToMove();
		PositionInfo posInfo = board.getPositionInfo();
		
		
		components[FN_STATIC_MATERIAL] = staticMaterialEval(posInfo);
		components[FN_DEVELOPMENT] = development(posInfo);
		components[FN_ATTACK_SQUARES] = attackSquareEval(posInfo.wConfig, posInfo.bConfig);
		components[FN_KING_MOVEMENT] = kingMovement(posInfo.wConfig, posInfo.bConfig, whiteToMove);
		components[FN_BISHOP_PAIR] = bishopPair(posInfo);
		components[FN_DOUBLED_PAWNS] = doubledPawnsSlow(posInfo.wConfig, posInfo.bConfig);
		components[FN_PASSED_PAWN] = passedPawnsSlow(posInfo.wConfig, posInfo.bConfig);
		return components;
	}
	
	
	// Evaluation has to be done from white's perspective
	@Override
	public int evaluate(EngineBoard board) {

		boolean whiteToMove = board.getWhiteToMove();
		
		PositionInfo posInfo = board.getPositionInfo();
		
		int eval = 
//				kingCheckWeight * kingCheckEval(friendly, enemy) +
				fnWeights[FN_STATIC_MATERIAL] * staticMaterialEval(posInfo) +
				fnWeights[FN_DEVELOPMENT] * development(posInfo) + 
				fnWeights[FN_ATTACK_SQUARES] * attackSquareEval(posInfo.wConfig, posInfo.bConfig) +
				fnWeights[FN_KING_MOVEMENT] * kingMovement(posInfo.wConfig, posInfo.bConfig, whiteToMove) +
				fnWeights[FN_BISHOP_PAIR] * bishopPair(posInfo) +
				fnWeights[FN_DOUBLED_PAWNS] * doubledPawnsSlow(posInfo.wConfig, posInfo.bConfig) +
				fnWeights[FN_PASSED_PAWN] * passedPawnsSlow(posInfo.wConfig, posInfo.bConfig)
				;
		
		
		return eval;	
	}

	
	/**
	 * Returns the static material difference. Don't consider king as king check can
	 * be considered separately.
	 * @param boards
	 * @param whiteToMove
	 * @return
	 */
	protected int staticMaterialEval(PositionInfo posInfo) {
		int w_material = getMaterial(posInfo.wConfig, Encodings.WQUEEN, Encodings.WPAWN);
		int b_material = getMaterial(posInfo.bConfig, Encodings.BQUEEN, Encodings.BPAWN);
		return  w_material - b_material;
		
	}

	/**
	 * Returns the number of attacked squares. 
	 * @param friendly
	 * @param enemy
	 * @return
	 */
	protected int attackSquareEval(OneSidePositionInfo wConfig, OneSidePositionInfo bConfig) {
		return 10 * (Long.bitCount(wConfig.all_attacks & ~wConfig.all_occ) - 
				Long.bitCount(bConfig.all_attacks & ~bConfig.all_occ));
	}
	
	/**
	 * If black's king's movement is restricted, that's good.
	 * @param wConfig
	 * @param bConfig
	 * @param whiteToMove
	 * @return
	 */
	protected int kingMovement(OneSidePositionInfo wConfig, OneSidePositionInfo bConfig, boolean whiteToMove) {
		int attackedBlackKingSquares = Long.bitCount(bConfig.figure_attacks[0]  & ~wConfig.all_attacks);
		int attackedWhiteKingSquares = Long.bitCount(wConfig.figure_attacks[0]  & ~bConfig.all_attacks);
		return 10 * (attackedBlackKingSquares - attackedWhiteKingSquares);
		
	}
	
	/**
	 * More development is better.
	 * @param posInfo.Config
	 * @return
	 */
	protected int development(PositionInfo posInfo) {
		int w_dev = getDevelopment(posInfo.wConfig, ~EConstants.ranks[0]);
		int b_dev = getDevelopment(posInfo.bConfig, ~EConstants.ranks[63]);
		return  (w_dev - b_dev);
	}
	
	protected int bishopPair(PositionInfo posInfo) {
		int wBishops = Long.bitCount(posInfo.wConfig.occ_boards[3]); // bishop
		int bBishops = Long.bitCount(posInfo.bConfig.occ_boards[3]);
		int wScore = wBishops == 2 ? 4 : 0;
		int bScore = bBishops == 2 ? 4 : 0;
		return 25 * (wScore - bScore);
	}
	
	protected int doubledPawnsSlow(OneSidePositionInfo wConfig, OneSidePositionInfo bConfig) {
		int wScore = 0;
		int bScore = 0;
		for (int i = 0; i < 8; i++) {
			long file = EConstants.files[i];
			int wPawns = Long.bitCount(wConfig.pawn_occ & file);
			wScore += wPawns >= 2 ? -2 : 0;
			int bPawns = Long.bitCount(bConfig.pawn_occ & file);
			bScore = bPawns >= 2 ? -2 : 0;
		}
		return 25 * (wScore - bScore);
	}
	
	
	//TODO: Improve speed
	protected int passedPawnsSlow(OneSidePositionInfo wConfig, OneSidePositionInfo bConfig) {
		int wscore = 0;
		int bscore = 0;
		for (int i = 0; i < 8; i++) {
			long file = EConstants.files[i];
			long w_file_occ = wConfig.pawn_occ & file;
			if (w_file_occ != 0L) {
				long leading_pawn_occ = Long.highestOneBit(w_file_occ);
				int square = Long.numberOfTrailingZeros(leading_pawn_occ);
				long squaresToPromotion = file & ~BitManipulation.long_msb_masks[square+1];
				if ((squaresToPromotion & bConfig.pawn_occ) == 0) {
					wscore += 6-Long.bitCount(squaresToPromotion);
				}
			}
			long b_file_occ = bConfig.pawn_occ & file;
			if (b_file_occ != 0L) {
				long leading_pawn_occ = Long.lowestOneBit(b_file_occ);
				int square = Long.numberOfTrailingZeros(leading_pawn_occ);
				long squaresToPromotion = file & BitManipulation.long_msb_masks[square-1];
				if ((squaresToPromotion & wConfig.pawn_occ) == 0) {
					bscore += 6-Long.bitCount(squaresToPromotion);
				}
			}
		}
		return 25 * (wscore - bscore);
	}
	
	
//	protected int connectedRooks(PieceConfig wConfig, PieceConfig bConfig) {
//		return connectedRooks(wConfig, Encodings.WROOK) - connectedRooks(bConfig, Encodings.BROOK);
//	}
//	
//	
//	private int[] rookIndices = new int[2];
//	private int connectedRooks(PieceConfig config, byte piece) {
//		int numRooks = 0;
//		for (int i = 0; i < config.num_figures; i++) {
//			if (piece == config.figure_pieces[i]) { 
//				rookIndices[numRooks++] = i;
//			}
//		}
//		if (numRooks < 2) {
//			return 0;
//		} 
//		if ((config.figure_attacks[rookIndices[0]] & config.figure_locs[rookIndices[1]]) != 0) {
//			return 2;
//		}
//		return 0;
//	}

	private int getDevelopment(OneSidePositionInfo wConfig, long mask) {
		int dev = 0;
		for (int i = 2; i <= 4; i++) { // 2 = Rook,  4 = knight
			int numDeveloped = Long.bitCount(wConfig.occ_boards[i] & mask);
			//Weight minor pieces more
			int pieceWeight = 2 * i;
			dev += numDeveloped * pieceWeight; 
		}
		return dev/20;
	}
	
	private int getMaterial(OneSidePositionInfo config, byte queen, byte pawn) {
		int material = 0;
		for (int i = 1; i <= 5; i++) { // 1=queen, 5= 
			material += Long.bitCount(config.occ_boards[i]) * staticPieceWeights[i - 1];
		}
		return material;
	}

	// initial function weights
	int[] fnWeights = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 0};
		
	private static final int FN_STATIC_MATERIAL = 0;
	private static final int FN_ATTACK_SQUARES = FN_STATIC_MATERIAL + 1;
	private static final int FN_KING_MOVEMENT = FN_ATTACK_SQUARES + 1;
	private static final int FN_DEVELOPMENT = FN_KING_MOVEMENT + 1;
	private static final int FN_BISHOP_PAIR = FN_DEVELOPMENT + 1;
	private static final int FN_PASSED_PAWN = FN_BISHOP_PAIR + 1;
	private static final int FN_DOUBLED_PAWNS = FN_PASSED_PAWN + 1;
	private static final int FN_KING_SAFETY = FN_DOUBLED_PAWNS + 1;
//	private static final int FN_CONNECTED_ROOKS = FN_BISHOP_PAIR + 1;
	
	private static String[] evalNames = new String[FN_KING_SAFETY + 1];
	
	static {
		evalNames[FN_STATIC_MATERIAL] = "Material";
		evalNames[FN_ATTACK_SQUARES] = "Attack squares";
		evalNames[FN_KING_MOVEMENT] = "King movement";
		evalNames[FN_DEVELOPMENT] = "Development";
		evalNames[FN_BISHOP_PAIR] = "Bishop pair";
		evalNames[FN_PASSED_PAWN] = "Passed pawn";
		evalNames[FN_DOUBLED_PAWNS] = "Doubled pawns";
		evalNames[FN_KING_SAFETY] = "King safety";
	}


	@Override
	public String[] evalNames() {
		return evalNames;
	}

	@Override
	public int pieceWeight(byte piece) {
		if (piece == Encodings.EMPTY) {
			return 0;
		}
		byte king = Encodings.isWhite(piece) ? Encodings.WKING : Encodings.BKING;
		if (piece == king) {
			return 1000;
		}
		return staticPieceWeights[piece - king - 1];
	}

	
	
}
