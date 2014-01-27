package sc.util;

import sc.util.BitManipulation;
import sc.util.ParseUtils;

public class TestUtils {

	public static long setSquares(long val, String...squares) {
		for (String s : squares) {
			short sq = ParseUtils.getSquare(s.toCharArray(), 0);
			val = BitManipulation.set(sq, val);
		}
		return val;
	}
}
