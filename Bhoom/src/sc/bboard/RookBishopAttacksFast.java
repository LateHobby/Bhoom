package sc.bboard;

import java.util.Arrays;

import sc.encodings.EConstants;
import sc.util.BitManipulation;
import sc.util.Magic;

public class RookBishopAttacksFast {

	static private long[] rookRankMagics;
	static private int[] rookRankBits;
	static private long[] rookFileMagics;
	static private int[] rookFileBits;
	static private long[] bishopMagics;
	static private int[] bishopBits;

	// [rook-pos][hashindex]
	static private long[][] rookFileAttacks;
	// [rook-pos][hashindex]
	static private long[][] rookRankAttacks;
	// [bishop-pos][hashindex]
	static private long[][] bishopAttacks;

	/**
	 * 1's everywhere except edge squares
	 */
	static private long bishop_edge_mask = ~(EConstants.ranks[0]
			| EConstants.files[0] | EConstants.ranks[63] | EConstants.files[63]);

	/**
	 * 1's everywhere except left and right edges
	 */
	static private long rank_edge_mask = ~(EConstants.files[0] | EConstants.files[63]);
	/**
	 * 1's everywhere except top and bottom edges
	 */
	static private long file_edge_mask = ~(EConstants.ranks[0] | EConstants.ranks[63]);

	/**
	 * Returns the squares that the rook on square can attack. INCLUDES squares
	 * occupied by friendly pieces, and does not include the square that the
	 * piece is on.
	 * 
	 * This implementation uses a perfect hashing function to map the occupancy
	 * bits in the rook's file to an index of a precomputed table of attacks. It
	 * also uses the optimization that the outermost bits of the file have no
	 * effect on the result to reduce the size of the lookup table.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long getRookAttacks(short square, long enemyOcc, long selfOcc) {
		long occ = enemyOcc | selfOcc;
		// clear self-occupied square
		occ &= ~BitManipulation.bit_masks[square];
		long rankocc = occ & EConstants.ranks[square];
		long fileocc = occ & EConstants.files[square];
		// remove edge squares
		rankocc &= rank_edge_mask;
		fileocc &= file_edge_mask;

		int rankhashindex = Magic.magicHash(rookRankMagics[square], rankocc, rookRankBits[square]);
		int filehashindex = Magic.magicHash(rookFileMagics[square], fileocc, rookFileBits[square]);
		
		return rookRankAttacks[square][rankhashindex]
				| rookFileAttacks[square][filehashindex];
		// return RookBishopAttacksSimple.getRookRankAttacks(square, enemyOcc,
		// selfOcc) |
		// rookFileAttacks[square][filehashindex];

	}

	/**
	 * Returns the squares that the bishop on square can attack. INCLUDES
	 * squares occupied by friendly pieces, and does not include the square that
	 * the piece is on.
	 * 
	 * This implementation uses a perfect hashing function to map the occupancy
	 * bits in the bishop's diagonals to an index in a precomputed table of
	 * attacks.
	 * 
	 * @param square
	 *            Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long getBishopAttacks(short square, long enemyOcc,
			long selfOcc) {
		long occ = enemyOcc | selfOcc;
		long diags = EConstants.diag45[square] | EConstants.diag135[square];
		occ &= diags;
		// clear self-occupied square
		occ &= ~BitManipulation.bit_masks[square];
		// remove edge squares
		occ &= bishop_edge_mask;

		int hashindex = Magic.magicHash(bishopMagics[square], occ, bishopBits[square]);
		
		return bishopAttacks[square][hashindex];

	}

	private static long[][] generateRookFileAttacks() {
		long[][] attacks = new long[64][];
		
		for (short square = 0; square < 64; square++) {
			int numBits = rookFileBits[square];
			attacks[square] = new long[1 << numBits];
			Arrays.fill(attacks[square], ~0L); // for checking later
			long file = EConstants.files[square];
			// remove edge squares
			file &= file_edge_mask;
			// remove self-occupied square
			file &= ~BitManipulation.bit_masks[square];
			int[] squares = BitManipulation.getSquares(square, file);
			
			long[] configs = BitManipulation.allLongsForBits(squares);

			for (long config: configs) {
				
				int hashindex = Magic.magicHash(rookFileMagics[square], config, numBits);
				long att = RookBishopAttacksSimple.getRookAttacks(square,
						config, 0L);
				
				attacks[square][hashindex] = att & EConstants.files[square];
			}
			// check that the array is completely filled
			for (int i = 0; i < attacks[square].length; i++) {
				if (attacks[square][i] == ~(0L)) {
					throw new RuntimeException("Array not completely filled:");
				}
			}

		}

		return attacks;

	}

	private static long[][] generateRookRankAttacks() {
		long[][] attacks = new long[64][];
		

		for (short square = 0; square < 64; square++) {
			int numBits = rookRankBits[square];
			attacks[square] = new long[1 << numBits];
			Arrays.fill(attacks[square], ~0L); // for checking later
			long rank = EConstants.ranks[square];
			// remove edge squares
			rank &= rank_edge_mask;
			// remove self-occupied square
			rank &= ~BitManipulation.bit_masks[square];
			int[] squares = BitManipulation.getSquares(square, rank);
			
			long[] configs = BitManipulation.allLongsForBits(squares);

			for (long config : configs) {
				
				int hashindex = Magic.magicHash(rookRankMagics[square], config, numBits);
				long att = RookBishopAttacksSimple.getRookAttacks(square,
						config, 0L);

				attacks[square][hashindex] = att & EConstants.ranks[square];
			}
			// check that the array is completely filled
			for (int i = 0; i < attacks[square].length; i++) {
				if (attacks[square][i] == (~0L)) {
					throw new RuntimeException("Array not completely filled: " + i);
				}
			}

		}

		return attacks;
	}

	private static long[][] generateBishopAttacks() {
		long[][] attacks = new long[64][];

		for (short square = 0; square < 64; square++) {
			int numBits = bishopBits[square];
			attacks[square] = new long[1 << numBits];
			Arrays.fill(attacks[square], ~0L); // for checking later
			long diags = EConstants.diag45[square] | EConstants.diag135[square];
			// remove edge squares
			diags &= bishop_edge_mask;
			// remove self-occupied square
			diags &= ~BitManipulation.bit_masks[square];
			int[] squares = BitManipulation.getSquares(square, diags);

			long[] configs = BitManipulation.allLongsForBits(squares);

			for (long config : configs) {
				
				int hashindex = Magic.magicHash(bishopMagics[square], config, numBits);
				long att = RookBishopAttacksSimple.getBishopAttacks(square,
						config, 0L);

				attacks[square][hashindex] = att;
				
			}
			// check that the array is completely filled
			for (int i = 0; i < attacks[square].length; i++) {
				if (attacks[square][i] == ~0L) {
					System.out.flush();
					throw new RuntimeException("Array not completely filled: square:" + square + " index:" + i);
				}
			}
		}
		return attacks;
	}

	static {
		/**
		 * Generated by the method printRookRankMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		rookRankMagics = new long[] { 4683743612465315840L, 2377900603251621888L, 4683743612465315840L, 2341871806232657920L, 1170935903116328960L, 2449958197289549824L, 4755801206503243776L, 4683743612465315840L, 18295873486192640L, 9288674231451648L, 18295873486192640L, 9147936743096320L, 4573968371548160L, 9570149208162304L, 18577348462903296L, 18295873486192640L, 71468255805440L, 36283883716608L, 71468255805440L, 35734127902720L, 17867063951360L, 37383395344384L, 72567767433216L, 71468255805440L, 279172874240L, 141733920768L, 279172874240L, 139586437120L, 69793218560L, 146028888064L, 283467841536L, 279172874240L, 1090519040L, 553648128L, 1090519040L, 545259520L, 272629760L, 570425344L, 1107296256L, 1090519040L, 4259840L, 2162688L, 4259840L, 2129920L, 1064960L, 2228224L, 4325376L, 4259840L, 16640L, 8448L, 16640L, 8320L, 4160L, 8704L, 16896L, 16640L, 65L, 33L, 65L, 10L, 18L, 34L, 66L, 65L };
		/**
		 * Generated by the method printRookFileMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		rookFileMagics = new long[] { 36099303471055872L, 18049651735527936L, 9024825867763968L, 4512412933881984L, 2256206466940992L, 1128103233470496L, 564051616735248L, 282025808367624L, 141012904183808L, 70506452091904L, 35253226045952L, 17626613022976L, 8813306511488L, 4406653255744L, 2203326627872L, 1101663313936L, 36029072434792448L, 18014536217396224L, 9007268108698112L, 4503634054349056L, 2251817027174528L, 1125908513587264L, 562954256793632L, 281477128396816L, 36099166301063168L, 18049583150531584L, 9024791575265792L, 4512395787632896L, 2256197893816448L, 1128098946908224L, 564049473454112L, 282024736727056L, 36099303203145728L, 18049651601572864L, 9024825800786432L, 4512412900393216L, 2256206450196608L, 1128103225098304L, 564051612549152L, 282025806274576L, 36099303470532608L, 18049651735266304L, 9024825867633152L, 4512412933816576L, 2256206466908288L, 1128103233454144L, 564051616727072L, 282025808363536L, 36099303471054848L, 18049651735527424L, 9024825867763712L, 4512412933881856L, 2256206466940928L, 1128103233470464L, 564051616735232L, 282025808367616L, 36099303471055872L, 18049651735527936L, 9024825867763968L, 4512412933881984L, 2256206466940992L, 1128103233470496L, 564051616735248L, 282025808367624L };
		/**
		 * Generated by the method printBishopMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		bishopMagics = new long[] { 18032007892189200L, 9016003946094592L, 18579556078190592L, 9306545590370304L, 4653270647701504L, 2326635390959616L, 1130315200593920L, 565157600297472L, 70437530828864L, 35218765414432L, 72576390930432L, 36353693712384L, 18176838467584L, 9088419495936L, 4415293752320L, 2207646876160L, 18014536082784384L, 9007268041392192L, 18155205799575616L, 9077585246978048L, 4521200437952512L, 2269400724015104L, 1125908530463232L, 562954265231616L, 9042383895527680L, 4521191947763840L, 2269667011870784L, 18050132704527488L, 9017094892962304L, 4508066401878144L, 2253998853783808L, 1126999426891904L, 4521260533416448L, 2260630266708224L, 1130333387096128L, 564050539053120L, 18032059683504192L, 9016004206141952L, 4508001968881792L, 2254000984440896L, 2260630400664576L, 1130315200332288L, 565157637915136L, 2207682396672L, 70438554173584L, 18032007893221512L, 9016003946086464L, 4508001973043232L, 1130315200593920L, 565157600296960L, 2207647334400L, 8862826496L, 275154878464L, 70437530836992L, 18032007892189184L, 9016003946094592L, 565157600297472L, 2207646876160L, 8623622400L, 34620416L, 1074823744L, 275146604832L, 70437530828864L, 18032007892189200L };

		/**
		 * The number of bits in the diagonal squares for each square.
		 * 
		 * Generated by the method printBishopMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		bishopBits = new int[] {6, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 5, 5, 5, 5, 7, 9, 9, 7, 5, 5, 5, 5, 7, 9, 9, 7, 5, 5, 5, 5, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 5, 5, 6 };

		rookFileBits = new int[] { 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6 };

		rookRankBits = new int[] { 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 6 };

		bishopAttacks = generateBishopAttacks();
		rookFileAttacks = generateRookFileAttacks();
		rookRankAttacks = generateRookRankAttacks();

	}

	public static void main(String[] args) {
		
	}
}
