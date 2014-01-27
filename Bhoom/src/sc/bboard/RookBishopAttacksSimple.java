package sc.bboard;

import sc.encodings.Encodings;
import sc.util.BitManipulation;

public class RookBishopAttacksSimple {

	
	/**
	 * Returns the squares that the rook on square can attack. INCLUDES squares occupied 
	 * by friendly pieces, and does not include the square that the piece is on.
	 * 
	 * This implementation is simple and easy to debug, but slow. It is primarily useful 
	 * for checking the correctness of faster, but harder to debug implementations.
	 * 
	 * @param square Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long getRookAttacks(short square, long enemyOcc, long selfOcc) {
		return getRookRankAttacks(square, enemyOcc, selfOcc) |
				getRookFileAttacks(square, enemyOcc, selfOcc);
	}
	
	
	public static long getRookRankAttacks(short square, long enemyOcc, long selfOcc) {
		long occ = enemyOcc | selfOcc;
		int file = Encodings.getFile(square);
		int rank = Encodings.getRank(square);
		
		long attacks = 0L;
		for (int nf = file+1; nf < 8; nf++) {
			short tsq = Encodings.encodeSquare(nf, rank);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		for (int nf = file-1; nf >= 0; nf--) {
			short tsq = Encodings.encodeSquare(nf, rank);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		
		return attacks;
	}
	
	public static long getRookFileAttacks(short square, long enemyOcc, long selfOcc) {
		long occ = enemyOcc | selfOcc;
		int file = Encodings.getFile(square);
		int rank = Encodings.getRank(square);
		
		long attacks = 0L;
		for (int nr = rank+1; nr < 8; nr++) {
			short tsq = Encodings.encodeSquare(file, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		for (int nr = rank-1; nr >= 0; nr--) {
			short tsq = Encodings.encodeSquare(file, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		return attacks;
	}
	
	/**
	 * Returns the squares that the bishop on square can attack. INCLUDES squares occupied 
	 * by friendly pieces, and does not include the square that the piece is on.
	 * 
	 * This implementation is simple and easy to debug, but slow. It is primarily useful 
	 * for checking the correctness of faster, but harder to debug implementations.
	 * 
	 * @param square Position of the moving piece
	 * @param enemyOcc
	 * @param selfOcc
	 * @return
	 */
	public static long getBishopAttacks(short square, long enemyOcc, long selfOcc) {
		long occ = enemyOcc | selfOcc;
		int file = Encodings.getFile(square);
		int rank = Encodings.getRank(square);
		long attacks = 0L;
		// 45 diag - upwards
		for (int nf = file+1; nf < 8; nf++) {
			int nr = rank + nf - file;
			if (nr > 7) {
				break;
			}
			short tsq = Encodings.encodeSquare(nf, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		// 45 diag - downwards
		for (int nf = file-1; nf >= 0; nf--) {
			int nr = rank - (file - nf);
			if (nr < 0) {
				break;
			}
			short tsq = Encodings.encodeSquare(nf, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		// 135 diag - downwards
		for (int nf = file+1; nf < 8; nf++) {
			int nr = rank - (nf - file);
			if (nr < 0) {
				break;
			}
			short tsq = Encodings.encodeSquare(nf, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		// 135 diag - upwards
		for (int nf = file-1; nf >= 0; nf--) {
			int nr = rank + (file - nf);
			if (nr > 7) {
				break;
			}
			short tsq = Encodings.encodeSquare(nf, nr);
			attacks = BitManipulation.set(tsq, attacks);
			if (BitManipulation.isSet(tsq, occ)) {
				break;
			}
		}
		return attacks;
	}


}
