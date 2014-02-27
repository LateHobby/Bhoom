package sc.bboard;

import java.util.Arrays;

import sc.encodings.EConstants;
import sc.util.BitManipulation;
import sc.util.Magic;

public class RookBishopAttacksFaster {

	public static long[] rookMagic = new long[64];
	public static long[] bishopMagic = new long[64];
	
	// Occupancy masks for rooks, exclude edge squares and self-occupied square
	public static long[] rookMasks = new long[64];
	// Occupancy masks for bishops, exclude edge squares and self-occupied square
	public static long[] bishopMasks = new long[64];
	
	//The number of diagonal squares for each square.
	static private int[] bishopBits = new int[64];
	
	//The number of rank or file squares for each square.
	static private int[] rookBits = new int[64];
	
	
	// [bishop-pos][hashindex]
	static private long[][] bishopAttacks;
	
	// [rook-pos][hashindex]
	static private long[][] rookAttacks;
	
	
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
		long maskedOcc = occ & rookMasks[square];
		
		int hashindex = Magic.magicHash(rookMagic[square], maskedOcc, rookBits[square]);
		return rookAttacks[square][hashindex];
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
		long maskedOcc = occ & bishopMasks[square];
		
		int hashindex = Magic.magicHash(bishopMagic[square], maskedOcc, bishopBits[square]);
		return bishopAttacks[square][hashindex];
		
	}
	
	
	
	static {
		initRookMagic();
		computeRookBitsAndMasks();
		generateRookAttacks();
		initBishopMagic();
		computeBishopBitsAndMasks();
		generateBishopAttacks();
	}



	public static void initRookMagic() {
		/**
		 * Generated by the method printRookMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		rookMagic[ 0 ]  = 144133881925214720L;
		rookMagic[ 1 ]  = 144151474111381520L;
		rookMagic[ 2 ]  = 144150923311058952L;
		rookMagic[ 3 ]  = 144133055710234628L;
		rookMagic[ 4 ]  = 1153203117291503680L;
		rookMagic[ 5 ]  = 576605956694082112L;
		rookMagic[ 6 ]  = 288248002764734593L;
		rookMagic[ 7 ]  = 144119594720706593L;
		rookMagic[ 8 ]  = 563091695747584L;
		rookMagic[ 9 ]  = 563229134815264L;
		rookMagic[ 10 ]  = 563227012374544L;
		rookMagic[ 11 ]  = 9011598391902336L;
		rookMagic[ 12 ]  = 563095983360004L;
		rookMagic[ 13 ]  = 2253037033227328L;
		rookMagic[ 14 ]  = 1125968760799745L;
		rookMagic[ 15 ]  = 562967166877761L;
		rookMagic[ 16 ]  = 36029897075867904L;
		rookMagic[ 17 ]  = 18015499103305760L;
		rookMagic[ 18 ]  = 9008299856896016L;
		rookMagic[ 19 ]  = 4504699692650504L;
		rookMagic[ 20 ]  = 2252899610527748L;
		rookMagic[ 21 ]  = 1126451810402816L;
		rookMagic[ 22 ]  = 563501856850176L;
		rookMagic[ 23 ]  = 2199090397249L;
		rookMagic[ 24 ]  = 36029900827672704L;
		rookMagic[ 25 ]  = 18031991773470720L;
		rookMagic[ 26 ]  = 9015996425703424L;
		rookMagic[ 27 ]  = 4507998751819776L;
		rookMagic[ 28 ]  = 2253999914877952L;
		rookMagic[ 29 ]  = 1127000496407040L;
		rookMagic[ 30 ]  = 563500787171584L;
		rookMagic[ 31 ]  = 281479271710786L;
		rookMagic[ 32 ]  = 36029898694860864L;
		rookMagic[ 33 ]  = 18032025057361952L;
		rookMagic[ 34 ]  = 9015996425703424L;
		rookMagic[ 35 ]  = 4507998751819776L;
		rookMagic[ 36 ]  = 2253999914877952L;
		rookMagic[ 37 ]  = 1127000496407040L;
		rookMagic[ 38 ]  = 562984346715137L;
		rookMagic[ 39 ]  = 281477140971586L;
		rookMagic[ 40 ]  = 36029898682335264L;
		rookMagic[ 41 ]  = 18032025057370112L;
		rookMagic[ 42 ]  = 9015995616215104L;
		rookMagic[ 43 ]  = 4507997808115776L;
		rookMagic[ 44 ]  = 2253998904066112L;
		rookMagic[ 45 ]  = 1126999452041280L;
		rookMagic[ 46 ]  = 563499726028864L;
		rookMagic[ 47 ]  = 281477128454146L;
		rookMagic[ 48 ]  = 36029898682278144L;
		rookMagic[ 49 ]  = 18032025057361952L;
		rookMagic[ 50 ]  = 9015996422553664L;
		rookMagic[ 51 ]  = 4507998748147776L;
		rookMagic[ 52 ]  = 2253999910944832L;
		rookMagic[ 53 ]  = 1127000492343360L;
		rookMagic[ 54 ]  = 562984380334592L;
		rookMagic[ 55 ]  = 281483575050496L;
		rookMagic[ 56 ]  = 36029898682277905L;
		rookMagic[ 57 ]  = 18015500172795921L;
		rookMagic[ 58 ]  = 9008299842211849L;
		rookMagic[ 59 ]  = 4504699676919813L;
		rookMagic[ 60 ]  = 563087661335554L;
		rookMagic[ 61 ]  = 563018807378178L;
		rookMagic[ 62 ]  = 562984380334210L;
		rookMagic[ 63 ]  = 281483575050273L;
	}

	private static void generateRookAttacks() {
		rookAttacks = new long[64][];
		long[][] attacks = rookAttacks;

		for (short square = 0; square < 64; square++) {
			int numBits = rookBits[square];
			attacks[square] = new long[1 << numBits];
			Arrays.fill(attacks[square], ~0L); // for checking later
			long diags = rookMasks[square];
			int[] squares = BitManipulation.getSquares(square, diags);

			long[] configs = BitManipulation.allLongsForBits(squares);

			for (long config : configs) {
				
				int hashindex = Magic.magicHash(rookMagic[square], config, numBits);
				long att = RookBishopAttacksSimple.getRookAttacks(square,
						config, 0L);

				attacks[square][hashindex] = att;
				
			}
			// check that the array is completely filled
			for (int i = 0; i < attacks[square].length; i++) {
				if (attacks[square][i] == ~0L) {
					System.out.flush();
					throw new RuntimeException("Rook Array not completely filled: square:" + square + " index:" + i);
				}
			}
		}
		
	}
	
	private static void computeRookBitsAndMasks() {
		for (short square = 0; square < 64; square++) {
			long bits = Magic.getRankAndFileBits(square);
			rookBits[square] = Long.bitCount(bits);
			rookMasks[square] = bits;
		}
		
	}

	private static void initBishopMagic() {
		/**
		 * Generated by the method printBishopMagics() in class
		 * chess.util.Magic.
		 * 
		 */
		bishopMagic = new long[] { 18032007892189200L, 9016003946094592L, 18579556078190592L, 9306545590370304L, 4653270647701504L, 2326635390959616L, 1130315200593920L, 565157600297472L, 70437530828864L, 35218765414432L, 72576390930432L, 36353693712384L, 18176838467584L, 9088419495936L, 4415293752320L, 2207646876160L, 18014536082784384L, 9007268041392192L, 18155205799575616L, 9077585246978048L, 4521200437952512L, 2269400724015104L, 1125908530463232L, 562954265231616L, 9042383895527680L, 4521191947763840L, 2269667011870784L, 18050132704527488L, 9017094892962304L, 4508066401878144L, 2253998853783808L, 1126999426891904L, 4521260533416448L, 2260630266708224L, 1130333387096128L, 564050539053120L, 18032059683504192L, 9016004206141952L, 4508001968881792L, 2254000984440896L, 2260630400664576L, 1130315200332288L, 565157637915136L, 2207682396672L, 70438554173584L, 18032007893221512L, 9016003946086464L, 4508001973043232L, 1130315200593920L, 565157600296960L, 2207647334400L, 8862826496L, 275154878464L, 70437530836992L, 18032007892189184L, 9016003946094592L, 565157600297472L, 2207646876160L, 8623622400L, 34620416L, 1074823744L, 275146604832L, 70437530828864L, 18032007892189200L };

		
	}

	private static void generateBishopAttacks() {
		bishopAttacks = new long[64][];
		long[][] attacks = bishopAttacks;

		for (short square = 0; square < 64; square++) {
			int numBits = bishopBits[square];
			attacks[square] = new long[1 << numBits];
			Arrays.fill(attacks[square], ~0L); // for checking later
			long diags = bishopMasks[square];
			int[] squares = BitManipulation.getSquares(square, diags);

			long[] configs = BitManipulation.allLongsForBits(squares);

			for (long config : configs) {
				
				int hashindex = Magic.magicHash(bishopMagic[square], config, numBits);
				long att = RookBishopAttacksSimple.getBishopAttacks(square,
						config, 0L);

				attacks[square][hashindex] = att;
				
			}
			// check that the array is completely filled
			for (int i = 0; i < attacks[square].length; i++) {
				if (attacks[square][i] == ~0L) {
					System.out.flush();
					throw new RuntimeException("Bishop Array not completely filled: square:" + square + " index:" + i);
				}
			}
		}

		
	}

	private static void computeBishopBitsAndMasks() {
		for (short square = 0; square < 64; square++) {
			long bits = Magic.getDiagonalBits(square);
			bishopBits[square] = Long.bitCount(bits);
			bishopMasks[square] = bits;
		}
		
	}

}