package sc.engine.movesorter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import sc.bboard.EBitBoard;
import sc.util.BoardUtils;
import sc.util.PrintUtils;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;

public class AbstractMoveSorterTest {

	@Test
	public void testEquality() {
		String testFile = "testing/suites/Test100.EPD";
		List<EPDTest> tests;
		try {
			tests = EPDTestingUtils.getTestsFromFile(testFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		for (EPDTest test : tests) {
			testSortsEqual(test.fen);
		}
	}
	
	public void testSortsEqual(String fen) {
		EBitBoard board = new EBitBoard();
		BoardUtils.initializeBoard(board, fen);
		int[] moveArr1 = new int[128];
		int numMoves = board.getMoveGenerator().fillLegalMoves(moveArr1, 0);
		int[] moveArr2 = new int[128];
		System.arraycopy(moveArr1, 0, moveArr2, 0, numMoves);
		MvvLvaHashSorter sorter = new MvvLvaHashSorter(true, false);
		sorter.sortMoves(board, 5, 0, moveArr1, numMoves);
		MvvLvaHashSorter s2 = new MvvLvaHashSorter(false, false);
		s2.sortMoves(board, 5, 0, moveArr2, numMoves);
		
		if( !Arrays.equals(moveArr1, moveArr2)) {
			System.out.println(fen);
			PrintUtils.printAsArray(moveArr1, numMoves);
			PrintUtils.printAsArray(moveArr2, numMoves);
			fail();
		}

	}

//	public void testSortsEqual(String fen) {
//		EBitBoard board = new EBitBoard();
//		BoardUtils.initializeBoard(board, fen);
//		int[] moveArr1 = new int[128];
//		int numMoves = board.getMoveGenerator().fillLegalMoves(moveArr1, 0);
//		PrintUtils.printAsArray(moveArr1, numMoves);
//		int[] moveArr2 = new int[128];
//		System.arraycopy(moveArr1, 0, moveArr2, 0, numMoves);
//		MvvLvaHashSorter sorter = new MvvLvaHashSorter();
//		sorter.sortMoves(board, 5, 0, moveArr1, numMoves);
//		DualSorter s2 = new DualSorter();
//		s2.sortMoves(board, 5, 0, moveArr2, numMoves);
//		PrintUtils.printAsArray(moveArr1, numMoves);
//		PrintUtils.printAsArray(moveArr2, numMoves);
//		assertTrue(Arrays.equals(moveArr1, moveArr2));
//		
//	}
//	
//	@Test
//	public void simpleTest() {
//		long mvv = 0;
//		long ds = 0;
//		Random r = new Random();
//		int[] a = new int[50];
//		for (int j = 0; j < 10000; j++) {
//			for (int i = 0; i < a.length; i++) {
//				a[i] = a.length - i; //r.nextInt(5000);
//			}
//			int[] ar = Arrays.copyOf(a, a.length);
//			int[] b =  Arrays.copyOf(a, a.length);
//			int[] br =  Arrays.copyOf(a, a.length);
//			MvvLvaHashSorter s1 = new MvvLvaHashSorter();
//			long start = System.currentTimeMillis();
//			s1.isortDescending(a, a.length, ar);
//			mvv += (System.currentTimeMillis() - start);
//			DualSorter s2 = new DualSorter();
//			start = System.currentTimeMillis();
//			s2.isortDescending(b, b.length, br);
//			ds += (System.currentTimeMillis() - start);
////			PrintUtils.printAsArray(a, a.length);
////			PrintUtils.printAsArray(b, b.length);
//			assertTrue(Arrays.equals(a, b));
//		}
//		System.out.println("mvv:" + mvv);
//		System.out.println("ds:" + ds);
//	}
//	private class DualSorter extends MvvLvaHashSorter {
//
//		@Override
//		void isortDescending(int[] m, int numMoves, int[]... ranks) {
//
//			//			qsortDescending(m, 0, numMoves - 1, ranks);
//			int [] tr = new int[ranks.length];
//			isort2Descending(m, numMoves, tr, ranks);
//		}
//		
//		private void qsortDescending(int[] m, int lo, int hi, int[]... ranks) {
//			if (lo == hi) {
//				return;
//			}
//			int pivot = findPivot(m, lo, hi, ranks);
//			if (pivot > lo) {
//				qsortDescending(m, lo, pivot-1, ranks);
//			}
//			if (pivot < hi) {
//				qsortDescending(m, pivot+1, hi, ranks);
//			}
//		}
//		
//		private int findPivot(int[] m, int lo, int hi, int[]... ranks) {
//			boolean moveUpper = true;
//			while (lo < hi) {
//				while  (compare(lo, hi, ranks) >= 0) {
//					if (moveUpper) {
//						hi--; 
//					} else {
//						lo++;
//					}
//					if (lo >= hi) {
//						return lo;
//					}
//				}
//				swap(lo, hi, m, ranks);
////				if (moveUpper) {
////					lo++;
////				} else {
////					hi--; 
////				}
//				moveUpper = !moveUpper;
//			}
//			return lo;
//		}
//
//		private int compare(int left, int right, int[]...ranks) {
//			for (int j = 0; j < ranks.length; j++) {
//				if (ranks[j][left] < ranks[j][right]) {
//					return -1;
//				} else if (ranks[j][left] > ranks[j][right]){
//					return 1;
//				}
//			}
//			return 0;
//		}
//		
//		private void swap(int left, int right, int[] values, int[]...ranks) {
//			for (int j = 0; j < ranks.length; j++) {
//				int tr = ranks[j][left];
//				ranks[j][left] = ranks[j][right];
//				ranks[j][right] = tr;
//			}
//			int tr = values[left];
//			values[left] = values[right];
//			values[right] = tr;
//		}
//		
//		void isort2Descending(int[] m, int numMoves, int[] tr, int[]... ranks) {
//			int v;
//			for (int i = 1; i <numMoves; i++ ) {
//				v= m[i];
//				for (int k = 0; k < ranks.length; k++) {
//					tr[k] = ranks[k][i];
//				}
//				int j = i;
//				while (j > 0 && compare(tr, j-1, ranks) > 0) {
//					shiftRight(j-1, m, ranks);
//					j--;
//				}
//				m[j] = v;
//				for (int k = 0; k < ranks.length; k++) {
//					ranks[k][j] = tr[k];
//				}
//			}
//		}
//		
//		private int compare(int[] tr, int i, int[][] ranks) {
//			for (int j = 0; j < ranks.length; j++) {
//				if (tr[j] < ranks[j][i]) {
//					return -1;
//				} else if (tr[j] > ranks[j][i]){
//					return 1;
//				}
//			}
//			return 0;
//		}
//
//		private void shiftRight(int index, int[] values, int[]... ranks) {
//			values[index+1] = values[index];
//			for (int j = 0; j < ranks.length; j++) {
//				ranks[j][index+1] = ranks[j][index];
//			}
//		}
//		
//	}
}
