package sc.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import sc.encodings.EConstants;

public class Magic {

	private static final long int_mask = ~(~0L << 32);

	public static final int magicHash(long magic, long value, int numBits) {
		long hash = (value * magic);
		int ihash = (int) (hash >>> (64 - numBits));
		return ihash & ~(~0 << numBits);
	}

	public static long findMagic(int[] bitsToCompress) {
		int[] targetBits = new int[bitsToCompress.length];
		for (int i = 0; i < bitsToCompress.length; i++) {
			targetBits[i] = 63 - i;
		}
		return findMagic(bitsToCompress, targetBits, 0);
	}

	private static long findMagic(int[] bits, int[] mapping, int index) {
		if (index == mapping.length - 1) {
			long magic = checkMapping(bits, mapping);
			return magic;
		}
		for (int i = index; i < mapping.length; i++) {
			int t = mapping[index];
			mapping[index] = mapping[i];
			mapping[i] = t;

			long magic = findMagic(bits, mapping, index + 1);
			if (magic != 0L) {
				return magic;
			}
			// restore the array
			t = mapping[index];
			mapping[index] = mapping[i];
			mapping[i] = t;

		}
		return 0L;
	}

	private static long checkMapping(int[] bits, int[] mapping) {
		// System.out.println("Checking:");
		// PrintUtils.printAsArray(bits);
		// PrintUtils.printAsArray(mapping);
		int[] shifts = new int[bits.length];
		for (int i = 0; i < bits.length; i++) {
			shifts[i] = mapping[i] - bits[i];
			if (shifts[i] < 0) {
				// System.out.println("Negative shift.");
				return 0L; // cannot right-shift bits
			}
		}
		// System.out.print("Shifts:");
		// PrintUtils.printAsArray(shifts);
		long magic = 0L;
		for (int shift : shifts) {
			magic |= 1L << shift;
		}
		// System.out.println("Magic:" +
		// PrintUtils.binaryRepresentationLong(magic));

		boolean failed = false;

		Set<Integer> used = new HashSet<Integer>();
		long[] configs = BitManipulation.allLongsForBits(bits);

		for (long config : configs) {

			int ihash = magicHash(magic, config, bits.length);
			// System.out.println("Config:" +
			// PrintUtils.binaryRepresentationLong(config));
			// System.out.println("IHash:" +
			// PrintUtils.binaryRepresentationInt(ihash));

			if (used.contains(ihash)) {
				failed = true;
				// System.out.println("Failed");
				break;
			} else {
				used.add(ihash);
			}

		}
		if (failed) {
			return 0L;
		} else {
			return magic;
		}

	}

	/**
	 * Returns a list of encodings of the squares in the diagonals through the
	 * given square. Does not include edge squares, or the square itself.
	 * 
	 * @param square
	 * @return
	 */
	private static int[] getDiagonalSquares(int square) {
		long diag45 = EConstants.diag45[square];
		long diag135 = EConstants.diag135[square];

		long diag = diag45 | diag135;
		// 1 everywhere except edges
		long edge_mask = ~(EConstants.ranks[0] | EConstants.ranks[63]
				| EConstants.files[0] | EConstants.files[63]);
		// remove edge squares
		diag &= edge_mask;
		// remove self-occupied square
		diag &= ~BitManipulation.bit_masks[square];

		return BitManipulation.getSquares(square, diag);

	}

	private static int[] getRookSquares(int square) {
		long filea = EConstants.files[square];
		// 1 everywhere except the top and bottom edges
		long edge_mask = ~(EConstants.ranks[0] | EConstants.ranks[63]);
		// remove end squares
		filea &= edge_mask;
		// remove self-occupied square
		filea &= ~BitManipulation.bit_masks[square];

		long ranka = EConstants.ranks[square];
		// 1 everywhere except the left and right edges
		edge_mask = ~(EConstants.files[0] | EConstants.files[63]);
		// remove end squares
		ranka &= edge_mask;
		// remove self-occupied square
		ranka &= ~BitManipulation.bit_masks[square];

		return BitManipulation.getSquares(square, ranka | filea);
	}

	private static int[] getFileSquares(int square) {
		long filea = EConstants.files[square];
		// 1 everywhere except the top and bottom edges
		long edge_mask = ~(EConstants.ranks[0] | EConstants.ranks[63]);
		// remove end squares
		filea &= edge_mask;
		// remove self-occupied square
		filea &= ~BitManipulation.bit_masks[square];

		return BitManipulation.getSquares(square, filea);
	}

	private static int[] getRankSquares(int square) {
		long ranka = EConstants.ranks[square];
		// 1 everywhere except the left and right edges
		long edge_mask = ~(EConstants.files[0] | EConstants.files[63]);
		// remove end squares
		ranka &= edge_mask;
		// remove self-occupied square
		ranka &= ~BitManipulation.bit_masks[square];
		return BitManipulation.getSquares(square, ranka);
	}

	/**
	 * Prints out 64 magic numbers for diagonals (with edge squares not
	 * included), and the number of bits for each of the diagonals.
	 */
	private static void printBishopMagics() {
		StringBuilder mb = new StringBuilder();
		StringBuilder bb = new StringBuilder();

		for (int square = 0; square < 64; square++) {
			int[] diagonalSquares = getDiagonalSquares(square);
			long magic = findMagic(diagonalSquares);
			if (magic == 0) {
				System.out.println("Failed on " + square);
				System.exit(0);
			}
			mb.append(magic);
			mb.append("L, ");
			bb.append(diagonalSquares.length);
			bb.append(", ");

		}
		System.out.println(mb.toString());
		System.out.println(bb.toString());
	}

	private static void printRookFileMagics() {
		StringBuilder mb = new StringBuilder();
		StringBuilder bb = new StringBuilder();

		for (int square = 0; square < 64; square++) {
			int[] fileSquares = getFileSquares(square);
			long magic = findMagic(fileSquares);
			if (magic == 0) {
				System.out.println("Failed on " + square);
				System.exit(0);
			}
			mb.append(magic);
			mb.append("L, ");
			bb.append(fileSquares.length);
			bb.append(", ");

		}
		System.out.println(mb.toString());
		System.out.println(bb.toString());
	}

	private static void printRookRankMagics() {
		StringBuilder mb = new StringBuilder();
		StringBuilder bb = new StringBuilder();

		for (int square = 0; square < 64; square++) {
			int[] rankSquares = getRankSquares(square);
			long magic = findMagic(rankSquares);
			if (magic == 0) {
				System.out.println("Failed on " + square);
				System.exit(0);
			}
			mb.append(magic);
			mb.append("L, ");
			bb.append(rankSquares.length);
			bb.append(", ");
		}
		System.out.println(mb.toString());
		System.out.println(bb.toString());
	}

	private static void printRookMagics() {

		Executor exec = new ScheduledThreadPoolExecutor(8);
		int numTasks = 8;
		for (int i = 0; i < numTasks; i++) {
			final int start = i * numTasks;
			final int end = start + 64 / numTasks;
			Runnable r = new Runnable() {
				public void run() {
					StringBuilder mb = new StringBuilder();
					StringBuilder bb = new StringBuilder();

					for (int square = start; square < end; square++) {
						int[] squares = getRookSquares(square);
						long magic = findMagic(squares);
						if (magic == 0) {
							System.out.println("Failed on " + square);
							System.exit(0);
						}
						mb.append(magic);
						mb.append("L, ");
						bb.append(squares.length);
						bb.append(", ");
						System.out.println(square);
					}
					System.out.println(mb.toString());
					System.out.println(bb.toString());
				}
			};
			exec.execute(r);
		}
	}

	// Magics that work for files and diagonals
	// file
	// int[] bits = new int[] {63, 55, 47, 39, 31, 23, 15, 7};
	// int[] shifts = new int[] {55, 46, 37, 28, 19, 10, 1, 0};

	// diag 45
	// int[] bits = new int[] {63, 54, 45, 36, 27, 18, 9, 0};
	// int[] shifts = new int[] {62, 52, 42, 32, 22, 12, 2, 0};

	// diag 135
	// int[] bits = new int[] {56, 49, 42, 35, 28, 21, 14, 7};
	// int[] shifts = new int[] {56, 48, 40, 32, 24, 16, 8, 0};

	public static void main(String[] args) {
		// center diagonals
		// System.out.println(findMagic(new int[]{9, 18, 27, 36, 45, 54, 14, 21,
		// 28, 35, 42, 49}));

		printRookMagics();
	}

}
