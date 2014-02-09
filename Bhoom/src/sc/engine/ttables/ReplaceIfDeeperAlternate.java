package sc.engine.ttables;

import sc.util.LPTTable;
import sc.util.TTable.TTEntry;

public class ReplaceIfDeeperAlternate extends AbstractEvalTT {

	LPTTable ttable;
	TTEntry localEntry = new TTEntry();
	boolean storeNow = true;
	
	public ReplaceIfDeeperAlternate() {
		ttable = new LPTTable(20, 2);
	}
	
	public ReplaceIfDeeperAlternate(int numBits) {
		ttable = new LPTTable(numBits, 2);
	}
	
	@Override
	public void storeToTT(long key, TTEntry entry) {
		storeNow = !storeNow;
		if (ttable.contains(key)) {
			if (storeNow) {
				ttable.store(key, encode(entry));
			} else {
				long value = ttable.get(key);
				decodeInto(value, localEntry);
				if (localEntry.depthLeft <= entry.depthLeft) {
					ttable.store(key, encode(entry));
				}
			}
		} else {
			ttable.store(key, encode(entry));
		}

	}

	@Override
	public boolean retrieveFromTT(long key, TTEntry returnValue) {
		if (ttable.contains(key)) {
			long value = ttable.get(key);
			decodeInto(value, returnValue);
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
