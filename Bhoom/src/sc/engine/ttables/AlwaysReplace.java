package sc.engine.ttables;

import sc.engine.EvalTT;
import sc.util.LPTTable;
import sc.util.TTable.ProbeResult;
import sc.util.TTable.TTEntry;

public class AlwaysReplace extends AbstractEvalTT implements EvalTT {

	LPTTable ttable;
	
	ProbeResult pr = new ProbeResult();
	
	public AlwaysReplace() {
		ttable = new LPTTable(21, 1);
	}
	
	public AlwaysReplace(int numBits) {
		ttable = new LPTTable(numBits, 1);
	}
	
	@Override
	public void storeToTT(long key, TTEntry stored) {
		long value = encode(stored);
		ttable.store(key, value);
	}

	@Override
	public boolean retrieveFromTT(long key, TTEntry stored) {
		if (ttable.contains(key, pr)) {
			long value = ttable.get(key);
			decodeInto(value, stored);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void reset() {
		ttable.reset();
		
	}
	
	
}
