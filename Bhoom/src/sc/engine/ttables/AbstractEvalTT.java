package sc.engine.ttables;

import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.SearchEngine.Continuation;
import sc.util.BitManipulation;
import sc.util.PrintUtils;
import sc.util.TTable;
import sc.util.TTable.TTEntry;

abstract public class AbstractEvalTT implements EvalTT {

	abstract public void storeToTT(long key, TTEntry entry);
	
	abstract public boolean retrieveFromTT(long key, TTEntry returnValue);
	
	@Override
	public void storeToTT(EngineBoard board, TTEntry entry) {
		storeToTT(board.getZobristKey(), entry);
	}

	@Override
	public boolean retrieveFromTT(EngineBoard board, TTEntry returnValue) {
		return retrieveFromTT(board.getZobristKey(), returnValue);
	}
	
	public long encode(TTEntry entry) {
		long val = entry.type;
		val <<= 18;
		val |= (entry.move & BitManipulation.int_msb_masks[18]);
		val <<= 16;
		val |= (entry.depthLeft & BitManipulation.int_msb_masks[16]);
		val <<= 16;
		long r = entry.eval;
		r <<= 48;
		r >>>= 48;
		val |= r;
		return val;
	}
	
	public void decodeInto(long value, TTEntry entry) {
		entry.eval = (short) (value & BitManipulation.long_msb_masks[16]);
		value >>>= 16;
		entry.depthLeft = (int) (value & BitManipulation.long_msb_masks[16]);
		value >>>=16;
		entry.move = (int) (value & BitManipulation.long_msb_masks[18]);
		value >>>= 18;
		entry.type = (int) value;
	}
	
	public Continuation getContinuation(EngineBoard board, int depth) {
		TTEntry entry = new TTEntry();
		Continuation leval = null;
		int i = 0;
		do {
			entry.move = 0;
			if (!retrieveFromTT(board, entry)) {
//				if (i == 0) {
//					throw new RuntimeException("No PV found!");
//				}
				break;
			}
			if (entry.move == 0) {
				throw new RuntimeException("Zero move stored?");
			}
			if (leval == null) {
				leval = new Continuation();
			}
			if (i == 0) {
				leval.eval = entry.eval;
			}
			leval.line[i] = entry.move;
			if (!board.makeMove(leval.line[i], true)) {
				throw new RuntimeException("Invalid move:" + PrintUtils.notation(leval.line[i]));
			}
			i++;
			
		} while (entry.move != 0 && i <= depth);
		
		while (i > 0) {
			board.undoLastMove();
			i--;
		}
		return leval;
	}
	
//	public static void main(String[] args) {
//		AbstractEvalTT att = new AbstractEvalTT();
//		TTEntry entry = new TTEntry();
//		entry.depthLeft = 56;
//		entry.eval = -12345;
//		entry.move = 34567;
//		entry.type = TTable.LOWERBOUND;
//		long v = att.encode(entry);
//		TTEntry dec = new TTEntry();
//		att.decodeInto(v, dec);
//		
//		test("Eval", entry.eval, dec.eval);
//		test("DepthLeft", entry.depthLeft, dec.depthLeft);
//		test("Move", entry.move, dec.move);
//		test("Type", entry.type, dec.type);
//	}

	private static void test(String string, int v1, int v2) {
		if (v1 != v2) {
			System.out.println(string + "[" + v1 + " : " + v2 + "]");
		}
		
	}
}
