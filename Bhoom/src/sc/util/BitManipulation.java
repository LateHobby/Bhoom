package sc.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BitManipulation {

	/**
	 * bit_masks[i] has the bit corresponding to the i-th square on the
	 * chessboard set.
	 */
	public static final long[] bit_masks = new long[64];
	/**
	 * int_bit_masks[i] has the bit corresponding to the i-th square on the
	 * chessboard set.
	 */
	public static final int[] int_bit_masks = new int[32];
	/**
	 * bit_templates[i] has the bit corresponding to the i-th square on the
	 * chessboard cleared and every other bit set.
	 */
	public static final long[] bit_templates = new long[64];

	/**
	 * An array of 32 masks; the i-th element of the array has 1 in the
	 * rightmost i bits.
	 */
	public static int[] int_msb_masks = new int[64];
	/**
	 * An array of 64 masks; the i-th element of the array has 1 in the
	 * rightmost i bits.
	 */
	public static long[] long_msb_masks = new long[64];
	static {
		long ONE = 1L;
		long lzero = 0L;
		long lmask = ~lzero;

		for (int i = 63; i >= 0; i--) {
			bit_masks[i] = (ONE << i);
			bit_templates[i] = ~bit_masks[i];
			long_msb_masks[i] = lmask >>> (64 - i);
		}
		int zero = 0;
		int iONE = 1;
		int mask = ~zero;
		for (int i = 31; i >= 0; i--) {
			int_bit_masks[i] = (iONE << i);
			int_msb_masks[i] = mask >>> (32 - i);
		}
	}

	/**
	 * Returns a new long with the same value as the given long but with the
	 * index-th bit set.
	 * 
	 * @param index
	 * @param val
	 * @return
	 */
	public static final long set(int index, long val) {
		return (val | bit_masks[index]);
	}

	/**
	 * Returns a new long with the same value as the given long but with the
	 * index-th bit cleared.
	 * 
	 * @param index
	 * @param val
	 * @return
	 */
	public static final long clear(int index, long val) {
		return (val & bit_templates[index]);
	}

	public static boolean isSet(int index, long val) {
		return ((long) (val & bit_masks[index])) != 0L;
	}

	/**
	 * Returns a list of the encodings of the squares encoded in bits. Does not
	 * include the square itself
	 * 
	 * @param square
	 * @param bits
	 * @return
	 */
	public static int[] getSquares(int square, long bits) {
		List<Integer> sqlist = new ArrayList<Integer>();
		for (short sq = 0; sq < 64; sq++) {
			if (sq == square) {
				continue;
			}

			if (BitManipulation.isSet(sq, bits)) {
				sqlist.add((int) sq);
			}

		}
		int[] ia = new int[sqlist.size()];
		for (int i = 0; i < ia.length; i++) {
			ia[i] = sqlist.get(i);
		}
		return ia;
	}

	/**
	 * Returns an array of longs, obtained by setting the specified bits to all
	 * their possible values.
	 * 
	 * @param squares
	 * @return
	 */
	public static long[] allLongsForBits(int[] squares) {
		
		int numConfigs = 1 << squares.length;
		long[] rv = new long[numConfigs];
		for (int i = 0; i < numConfigs; i++) {
			long num = 0L;
			for (int bit = 0; bit < squares.length; bit++) {
				int mask = 1 << bit;
				if ((i & mask) != 0) {
					num = set(squares[bit], num);
				}
			}
			rv[i] = num;
			
		}
		return rv;
	}

}
