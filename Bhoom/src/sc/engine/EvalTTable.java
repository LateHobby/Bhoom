package sc.engine;

import java.util.Arrays;
import java.util.Random;

import sc.util.BitManipulation;

public class EvalTTable {
	
	public long numCalls;
	public long numMatches;
	public long numHits;
	public long numCollisions;
	public long numReplacements;
	
	public static final int NO_IEVAL = ~0;
	private static final byte UNINIT = -1;
	public static final byte FLAG_AT_MOST = -1;
	public static final byte FLAG_AT_LEAST = 1;
	public static final byte FLAG_EXACT = 0;
	
	private int[] tmove;
	private byte[] tflag;
	private int[] teval;
	private byte[] tdepthLeft;
	private long[] tkey;
	private boolean[] tmaximizing;
//	private String[] fen;
	
	private int NUM_BITS;
	private int TABLE_SIZE;
	private long TABLE_MASK;
	
	public EvalTTable() {
		this(24);
	}
	
	public EvalTTable(int numBits) {
		NUM_BITS = numBits;
		TABLE_SIZE = (1 << NUM_BITS);
		TABLE_MASK = BitManipulation.long_msb_masks[NUM_BITS];

		tmove = new int[TABLE_SIZE];
		teval = new int[TABLE_SIZE];
		tdepthLeft = new byte[TABLE_SIZE];
		tkey = new long[TABLE_SIZE];
		tmaximizing = new boolean[TABLE_SIZE];
		tflag = new byte[TABLE_SIZE];
		
		Arrays.fill(tdepthLeft, UNINIT);
		
	}
	
	public boolean hasPosition(long zKey) {
		numCalls++;
		int index = (int) (zKey & TABLE_MASK);
		if (tdepthLeft[index] == UNINIT) {
			return false;
		}
		if (tkey[index] != zKey) {
			numCollisions++;
			return false;
		}
		numMatches++;
		return true;
	}
	
	
	public int getEval(long zKey, int alpha, int beta, int depthLeft, boolean maximizingPlayer) {
		int index = (int) (zKey & TABLE_MASK);
		if (depthLeft <= tdepthLeft[index]) {
			boolean flip = (maximizingPlayer != tmaximizing[index]);
			int seval = flip ? -teval[index] : teval[index];
			byte flag = (byte) (flip ? -tflag[index] : tflag[index]);
			if (flag == FLAG_EXACT) {
				numHits++;
				return seval;
			}
			if (flag == FLAG_AT_LEAST && beta <= seval) {
				numHits++;
				return beta;
			}
			if (flag == FLAG_AT_MOST && alpha >= seval)  {
				numHits++;
				return alpha;
			}
					
		} 
		return NO_IEVAL;
	}
	
	
	public void storePosition(long zKey, int eval, int alpha, int beta, int depthLeft, int move, boolean maximizingPlayer) {
		int index = (int) (zKey & TABLE_MASK);
		if (tdepthLeft[index] != UNINIT) {
			numReplacements++;
		}
		tdepthLeft[index] = (byte) depthLeft;
		tkey[index] = zKey;
		tmove[index] = move;
		tmaximizing[index] = maximizingPlayer;
		byte flag = FLAG_EXACT;
		if (alpha > eval) {
			flag = FLAG_AT_MOST;
		}
		if (eval >= beta) {
			flag = FLAG_AT_LEAST;
		}
		tflag[index] = flag;
		if (flag == FLAG_EXACT) {
			teval[index] = eval;
		}
		if (flag == FLAG_AT_MOST) {
			teval[index] = alpha;
		}
		if (flag == FLAG_AT_LEAST) {
			teval[index] = beta;
		}
	}
	
//	public void storeFen(long zKey, String sfen) {
//		int index = (int) (zKey & TABLE_MASK);
//		fen[index] = sfen;
//	}
//	
//	public String getFen(long zKey) {
//		int index = (int) (zKey & TABLE_MASK);
//		return fen[index];
//	}
	
	public int getMove(long zKey) {
		int index = (int) (zKey & TABLE_MASK);
		return tmove[index];
	}
	
	public byte getFlag(long zKey, boolean maximizingPlayer) {
		int index = (int) (zKey & TABLE_MASK);
		boolean flip = (maximizingPlayer != tmaximizing[index]);
		byte flag = (byte) (flip ? -tflag[index] : tflag[index]);
		return flag;
					
	}

	public long getKey(long zKey) {
		int index = (int) (zKey & TABLE_MASK);
		return tkey[index];
	}
	
	public static void main(String[] args) {
		int NUM_BITS = 16;
		int TABLE_SIZE = (1 << NUM_BITS);
		long TABLE_MASK = BitManipulation.long_msb_masks[NUM_BITS];

		Random r = new Random();
		for (int i = 0; i < 10000000; i++) {
			long l = r.nextLong();
			if (l < 0L) {
				l = -l;
			}
			int a = (int) (l & TABLE_MASK);
			int b = (int) (l % TABLE_SIZE);
			if (a != b) {
				System.out.println("l=" + l + " a=" + a + " b=" + b);
			}
		}
	}
}
