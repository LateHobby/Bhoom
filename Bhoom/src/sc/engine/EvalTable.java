package sc.engine;

import sc.util.ListNode;
import sc.util.LongHashTable;
import sc.util.ObjectPool;
import sc.util.ObjectPool.Factory;

public class EvalTable {

	private int numHits;
	private int numCalls;
	private int numOverwrites;
	private int depthRej;
	private int numMatches;
	
	public static final int NO_IEVAL = ~0;
	public static final byte FLAG_AT_MOST = -1;
	public static final byte FLAG_AT_LEAST = 1;
	public static final byte FLAG_EXACT = 0;

	
	public static class Eval extends ListNode {
		public int eval;
		public int move;
		public byte depthLeft;
		public byte flag;
		public boolean maximizing;
	}
	
	private LongHashTable<Eval> ttable;
	private Eval cached;
	private long cachedKey;
	
	private ObjectPool<Eval> evalPool;
	private boolean overWriteByDepth;
	
	public EvalTable(int numBits, int maxChain, boolean overwriteByDepth) {
		this.overWriteByDepth = overwriteByDepth;
		evalPool = new ObjectPool<Eval>(
				new Factory<Eval>() {

					@Override
					public Eval create() {
						return new Eval();
					}

					@Override
					public Eval[] getArray(int size) {
						return new Eval[size];
					} 
			
		}, (1<<numBits), "EvalPool");
		ttable = new LongHashTable<>(evalPool, numBits, maxChain);
	}
	
	public boolean hasPosition(long zKey) {
		numCalls++;
		cached = null;
		cached = ttable.get(zKey);
		cachedKey = zKey;
		boolean rv = (cached != null);
		if (rv) {
			numMatches++;
		}
		return rv;
	}
	public int getEvalForPV(long zKey) {
		if (cachedKey != zKey) {
			throw new RuntimeException("Bad key");
		}
		return cached.eval;
	}
	
	public int getEval(long zKey, int alpha, int beta, int depthLeft, boolean maximizingPlayer) {
		if (cachedKey != zKey) {
			throw new RuntimeException("Bad key");
		}
		if (depthLeft <= cached.depthLeft) {
			ttable.promote(zKey, cached);
			boolean flip = (maximizingPlayer != cached.maximizing);
			int seval = flip ? -cached.eval : cached.eval;
			byte flag = (byte) (flip ? -cached.flag : cached.flag);
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
					
		} else {
			depthRej++;
		}
		return NO_IEVAL;
	}

	public boolean storePosition(long zKey, int eval, int alpha, int beta, int depthLeft, int move, boolean maximizingPlayer) {
		if (overWriteByDepth) {
			Eval stored = ttable.get(zKey);
			if (stored != null && stored.depthLeft > depthLeft) {
				return false;
			}
		}
		Eval ev = evalPool.allocate();
		
		ev.depthLeft = (byte) depthLeft;
		ev.move = move;
		ev.maximizing = maximizingPlayer;
		byte flag = FLAG_EXACT;
		if (alpha > eval) {
			flag = FLAG_AT_MOST;
		}
		if (eval >= beta) {
			flag = FLAG_AT_LEAST;
		}
		ev.flag = flag;
		if (flag == FLAG_EXACT) {
			ev.eval = eval;
		}
		if (flag == FLAG_AT_MOST) {
			ev.eval = alpha;
		}
		if (flag == FLAG_AT_LEAST) {
			ev.eval = beta;
		}
		
		ttable.store(zKey, ev);
		
		return true;
	}

	public int getMove(long zKey) {
		if (cachedKey != zKey) {
			throw new RuntimeException("Bad key");
		}
		return cached.move;
	}
	public byte getFlag(long zKey, boolean maximizingPlayer) {
		if (cachedKey != zKey) {
			throw new RuntimeException("Bad key");
		}
		boolean flip = (maximizingPlayer != cached.maximizing);
		byte flag = (byte) (flip ? -cached.flag : cached.flag);
		return flag;
	}
	
	public void printStats() {
		System.out.println("EvalTable:   Calls:" + numCalls + 
				" Matches:" + (numMatches * 100)/numCalls +
				"%  Hits:" + (numHits * 100)/numCalls + 
				"%");
		ttable.printStats();
	}
}
