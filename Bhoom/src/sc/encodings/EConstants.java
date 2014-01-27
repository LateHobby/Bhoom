package sc.encodings;

import sc.util.BitManipulation;


public class EConstants {

	
	static public final short[] queens = new short[]{Encodings.WQUEEN, Encodings.BQUEEN};
	static public final short[] rooks = new short[]{Encodings.WROOK, Encodings.BROOK};
	static public final short[] bishops = new short[]{Encodings.WBISHOP, Encodings.BBISHOP};
	static public final short[] knights = new short[]{Encodings.WKNIGHT, Encodings.BKNIGHT};
	
	static public final byte[] w_promotions = new byte[] {Encodings.WQUEEN, Encodings.WROOK,
		Encodings.WBISHOP, Encodings.WKNIGHT }; 
	static public final byte[] b_promotions = new byte[] {Encodings.BQUEEN, Encodings.BROOK,
			Encodings.BBISHOP, Encodings.BKNIGHT };
	
	static public short w_king_castle_from = Encodings.encodeSquare(4, 0);
	static public short w_king_castle_to = Encodings.encodeSquare(6, 0);
	static public short w_king_castle_rook_from = Encodings.encodeSquare(7, 0);
	static public short w_king_castle_rook_to = Encodings.encodeSquare(5, 0);
	
	static public short w_queen_castle_from = Encodings.encodeSquare(4, 0);
	static public short w_queen_castle_to = Encodings.encodeSquare(2, 0);
	static public short w_queen_castle_rook_from = Encodings.encodeSquare(0, 0);
	static public short w_queen_castle_rook_to = Encodings.encodeSquare(3, 0);
	
	static public short b_king_castle_from = Encodings.encodeSquare(4, 7);
	static public short b_king_castle_to = Encodings.encodeSquare(6, 7);
	static public short b_king_castle_rook_from = Encodings.encodeSquare(7, 7);
	static public short b_king_castle_rook_to = Encodings.encodeSquare(5, 7);
	
	static public short b_queen_castle_from = Encodings.encodeSquare(4, 7);
	static public short b_queen_castle_to = Encodings.encodeSquare(2, 7);
	static public short b_queen_castle_rook_from = Encodings.encodeSquare(0, 7);
	static public short b_queen_castle_rook_to = Encodings.encodeSquare(3, 7);
	
	// The squares that should be check-free and unoccupied for castling to be possible
	static public long w_kingside_castle_unoccupied;
	static public long w_queenside_castle_unoccupied;
	static public long b_kingside_castle_unoccupied;
	static public long b_queenside_castle_unoccupied;
	
	static public long w_kingside_castle_checkfree;
	static public long w_queenside_castle_checkfree;
	static public long b_kingside_castle_checkfree;
	static public long b_queenside_castle_checkfree;

	
	static public long[] ranks = new long[64];
	static public long[] files = new long[64];
	static public long[] diag45 = new long[64];
	static public long[] diag135 = new long[64];

	static public long[] queen_x_ray_attacks = new long[64];
	static public long[] rook_x_ray_attacks = new long[64];
	static public long[] bishop_x_ray_attacks = new long[64];
	
	static public char[] starting_position = new char[32];
	
	// [piece_pos] [occupancy]
	static public byte[][] linearAttacks = new byte [64][];
	
	static {
		generateRanksFilesDiagonals();
		generateXRayAttacks();
		generateLinearAttacks();
		generateStartingPosition();
		generateCastlingData();
	}
	
	private static void generateStartingPosition() {
		byte[] wpieces = new byte[]{
				Encodings.WROOK,
				Encodings.WKNIGHT,
				Encodings.WBISHOP,
				Encodings.WQUEEN,
				Encodings.WKING,
				Encodings.WBISHOP,
				Encodings.WKNIGHT,
				Encodings.WROOK
		};
		byte[] bpieces = new byte[]{
				Encodings.BROOK,
				Encodings.BKNIGHT,
				Encodings.BBISHOP,
				Encodings.BQUEEN,
				Encodings.BKING,
				Encodings.BBISHOP,
				Encodings.BKNIGHT,
				Encodings.BROOK
		};
		
		int index = 0;
		for (int file = 0; file < 8; file++) {
			short square = Encodings.encodeSquare(file, 0);
			char lp = Encodings.encodeLocatedPiece(wpieces[file], square);
			starting_position[index++] = lp;
			square = Encodings.encodeSquare(file, 1);
			lp = Encodings.encodeLocatedPiece(Encodings.WPAWN, square);
			starting_position[index++] = lp;
		}
		for (int file = 0; file < 8; file++) {
			short square = Encodings.encodeSquare(file, 7);
			char lp = Encodings.encodeLocatedPiece(bpieces[file], square);
			starting_position[index++] = lp;
			square = Encodings.encodeSquare(file, 6);
			lp = Encodings.encodeLocatedPiece(Encodings.BPAWN, square);
			starting_position[index++] = lp;
		}
		
	}

	private static void generateXRayAttacks() {
		for (int i = 0; i < 64; i++) {
			long loc = BitManipulation.bit_masks[i];
			rook_x_ray_attacks[i] = (ranks[i] | files[i]) & ~loc;
			bishop_x_ray_attacks[i] = (diag45[i] | diag135[i]) & ~loc;
			queen_x_ray_attacks[i] = rook_x_ray_attacks[i] | bishop_x_ray_attacks[i];
		}
		
	}

	private static void generateCastlingData() {
		w_kingside_castle_unoccupied = 0L;
		w_kingside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(5, 0), w_kingside_castle_unoccupied);
		w_kingside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(6, 0), w_kingside_castle_unoccupied);
		w_queenside_castle_unoccupied = 0L;
		w_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(1, 0), w_queenside_castle_unoccupied);
		w_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(2, 0), w_queenside_castle_unoccupied);
		w_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(3, 0), w_queenside_castle_unoccupied);
		b_kingside_castle_unoccupied = 0L;
		b_kingside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(5, 7), b_kingside_castle_unoccupied);
		b_kingside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(6, 7), b_kingside_castle_unoccupied);
		b_queenside_castle_unoccupied = 0L;
		b_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(1, 7), b_queenside_castle_unoccupied);
		b_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(2, 7), b_queenside_castle_unoccupied);
		b_queenside_castle_unoccupied = BitManipulation.set(Encodings.encodeSquare(3, 7), b_queenside_castle_unoccupied);

		w_kingside_castle_checkfree = 0L;
		w_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(4, 0), w_kingside_castle_checkfree);
		w_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(5, 0), w_kingside_castle_checkfree);
		w_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(6, 0), w_kingside_castle_checkfree);
		w_queenside_castle_checkfree = 0L;
		w_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(2, 0), w_queenside_castle_checkfree);
		w_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(3, 0), w_queenside_castle_checkfree);
		w_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(4, 0), w_queenside_castle_checkfree);
		b_kingside_castle_checkfree = 0L;
		b_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(4, 7), b_kingside_castle_checkfree);
		b_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(5, 7), b_kingside_castle_checkfree);
		b_kingside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(6, 7), b_kingside_castle_checkfree);
		b_queenside_castle_checkfree = 0L;
		b_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(2, 7), b_queenside_castle_checkfree);
		b_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(3, 7), b_queenside_castle_checkfree);
		b_queenside_castle_checkfree = BitManipulation.set(Encodings.encodeSquare(4, 7), b_queenside_castle_checkfree);
	}
		

	static private void generateLinearAttacks() {
		linearAttacks = generateLinearAttacksOfSize(8);
	}
	
	
	


	static private byte[][] generateLinearAttacksOfSize(int array_size) {
		byte[][] attacks = new byte[array_size][];
		for (int i = 0; i < array_size; i++) {
			attacks[i] = generateLinearAttacksOfSizeAndLocation(array_size, i);
		}
		return attacks;
	}

	static private byte[] generateLinearAttacksOfSizeAndLocation(int array_size, int piece_pos) {
		int num_configs = (int) Math.pow(2, array_size);
		byte[] attacks = new byte[num_configs];
		for (int i = 0; i < num_configs; i++) {
			int iattack = computeAttacks(i,piece_pos, array_size);
			attacks[i] = (byte) iattack;
		}
		return attacks;
	}

	static private int computeAttacks(int occupancy, int piece_pos, int array_size) {
		// do the computation with longs because the masks etc are defined for longs
		long attack = 0L;
		long locc = occupancy;
		int index = piece_pos;
		boolean noPieceEncountered = true;
		index--;
		while (index >= 0 && noPieceEncountered) {
			if (BitManipulation.isSet(index, locc)) {
				noPieceEncountered = false;
			}
			attack = BitManipulation.set(index, attack);
			index--;
		}
		noPieceEncountered = true;
		index = piece_pos;
		index++;
		while (index < array_size && noPieceEncountered) {
			if (BitManipulation.isSet(index, locc)) {
				noPieceEncountered = false;
			}
			attack = BitManipulation.set(index, attack);
			index++;
		}
		return (int) attack;
	}

	static private void generateRanksFilesDiagonals() {
		long zero = 0L;
		
		
		for (short square = 0; square < 64; square++) {
			int file = Encodings.getFile(square);
			int rank = Encodings.getRank(square);
			ranks[square] = files[square] = diag45[square] = diag135[square] = 0L;
			for (int i = 0; i < 8; i++) {
				ranks[square] = BitManipulation.set(Encodings.encodeSquare(i, rank), ranks[square]);
				files[square] = BitManipulation.set(Encodings.encodeSquare(file, i), files[square]);
				for (int j = 0; j < 8; j++) {
					if (i - file == j - rank) {
						diag45[square] = BitManipulation.set(Encodings.encodeSquare(i, j), diag45[square]);
					}
					if (- (i - file) == j - rank) {
						diag135[square] = BitManipulation.set(Encodings.encodeSquare(i, j), diag135[square]);
					}
							
				}
				
			}
//			PrintUtils.printAsBoards(new long[]{ranks[square], files[square], diag45[square], diag135[square]});
//			System.out.println();
		}
		
	}

	public static void main(String[] args) {
		
	}
}
