package sc.performance;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import sc.bboard.RookBishopAttacksFast;
import sc.bboard.RookBishopAttacksSimple;
import sc.encodings.EConstants;
import sc.encodings.Encodings;
import sc.util.PrintUtils;

public class PerformanceTest {

	private static long[] fileAttacks;
	private static long[] rankAttacks;
	private static Map<Long, Long> attackMap = new HashMap<Long, Long>();
	
	
//	@Test
	public void testRookAttacksCorrectness() {
		Random r = new Random();
		
		int repeats = 500000;
		long total = 0;
		long newTotal = 0;
		for (int count = 0; count < repeats; count++) {
			long occ = (r.nextLong() & r.nextLong() & r.nextLong()); // don't have too many bits set
			for (int i = 0; i < 64; i++) {
				short square = (short) i;
				long att = RookBishopAttacksSimple.getRookAttacks(square, occ, 0L);
				long attNew = RookBishopAttacksFast.getRookAttacks(square, occ, 0L);;
				
				if (att != attNew) {
					int file = Encodings.getFile(square);
					int rank = Encodings.getRank(square);
					
					long rankFileOcc = (occ & EConstants.files[file]) |
							(occ & EConstants.ranks[rank]);
					PrintUtils.printAsBoards(new long[]{rankFileOcc, att, attNew});
					System.out.println("Square = " + square);
					fail();
				}
			}
			
		}
		System.out.println("Rook: NewTotal=" + newTotal + " OldTotal=" + total);
	}

//	@Test
	public void testBishopAttacksCorrectness() {
		Random r = new Random();
		
		int repeats = 500000;
		long total = 0;
		long newTotal = 0;
		for (int count = 0; count < repeats; count++) {
			long occ = (r.nextLong() & r.nextLong() & r.nextLong()); // don't have too many bits set
			for (int i = 0; i < 64; i++) {
				short square = (short) i;
				long att = RookBishopAttacksSimple.getBishopAttacks(square, occ, 0L);
				long attNew = RookBishopAttacksFast.getBishopAttacks(square, occ, 0L);;
				
				if (att != attNew) {
					int file = Encodings.getFile(square);
					int rank = Encodings.getRank(square);
					
					long rankFileOcc = (occ & EConstants.files[file]) |
							(occ & EConstants.ranks[rank]);
					System.out.println("Square = " + square);
					PrintUtils.printAsBoards(new long[]{rankFileOcc, att, attNew});
					fail();
				}
			}
			
		}
		System.out.println("Rook: NewTotal=" + newTotal + " OldTotal=" + total);
	}

	@Test
	public void testRookAttacksPerformance() {
		Random r = new Random();
		// initialize the class ?
		long l = RookBishopAttacksFast.getRookAttacks((short) 0, 0L, 0L);
		
		int repeats = 500000;
		long total = 0;
		long newTotal = 0;
		for (int count = 0; count < repeats; count++) {
			long occ = (r.nextLong() & r.nextLong() & r.nextLong()); // don't have too many bits set
			long start = System.currentTimeMillis();
			for (int i = 0; i < 64; i++) {
				RookBishopAttacksFast.getRookAttacks((short)i, occ, 0L);
			}
			newTotal += (System.currentTimeMillis() - start);
			
			start = System.currentTimeMillis();
			for (int i = 0; i < 64; i++) {
				RookBishopAttacksSimple.getRookAttacks((short)i, occ, 0L);
			}
			total += (System.currentTimeMillis() - start);
		}
		System.out.println("Rook: NewTotal=" + newTotal + " OldTotal=" + total);
	}
	
	@Test
	public void testBishopAttacksPerformance() {
		Random r = new Random();
		// initialize the class ?
		long l = RookBishopAttacksFast.getRookAttacks((short) 0, 0L, 0L);
		
		int repeats = 500000;
		long total = 0;
		long newTotal = 0;
		for (int count = 0; count < repeats; count++) {
			long occ = (r.nextLong() & r.nextLong() & r.nextLong()); // don't have too many bits set
			long start = System.currentTimeMillis();
			for (int i = 0; i < 64; i++) {
				RookBishopAttacksFast.getBishopAttacks((short)i, occ, 0L);
			}
			newTotal += (System.currentTimeMillis() - start);
			
			start = System.currentTimeMillis();
			for (int i = 0; i < 64; i++) {
				RookBishopAttacksSimple.getBishopAttacks((short)i, occ, 0L);
			}
			total += (System.currentTimeMillis() - start);
		}
		System.out.println("Bishop  NewTotal=" + newTotal + " OldTotal=" + total);
	}

	
	
	
}
