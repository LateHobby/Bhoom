package sc.engine.ttables;

import sc.util.LPTTable;
import sc.util.TTable.ProbeResult;
import sc.util.TTable.TTEntry;

public class ReplaceIfDeeperAlternate extends AbstractEvalTT {

	LPTTable alwaysReplace;
	LPTTable replaceIfDeeper;
	
	ProbeResult pr = new ProbeResult();
	
	TTEntry localEntry = new TTEntry();
	boolean storeNow = true;
	
	public ReplaceIfDeeperAlternate() {
		alwaysReplace = new LPTTable(20, 1);
		replaceIfDeeper = new LPTTable(20, 1);
	}
	
	public ReplaceIfDeeperAlternate(int numBits) {
		alwaysReplace = new LPTTable(numBits, 1);
		replaceIfDeeper = new LPTTable(numBits, 1);
	}
	
	@Override
	public void storeToTT(long key, TTEntry entry) {
		storeNow = !storeNow;
		boolean replaceIfDeeperHasKey = replaceIfDeeper.contains(key, pr);
		boolean stored = false;
		if (replaceIfDeeperHasKey && pr.existingKey != 0) {
			if (storeNow) {
				replaceIfDeeper.store(key, encode(entry));
			} else {
				decodeInto(pr.existingValue, localEntry);
				if (localEntry.depthLeft <= entry.depthLeft) {
					stored = true;
					replaceIfDeeper.store(key, encode(entry));
				}
			}
		} else {
			stored = true;
			replaceIfDeeper.store(key, encode(entry));
		}
		if (!stored) {
			alwaysReplace.store(key, encode(entry));
		}

	}

	@Override
	public boolean retrieveFromTT(long key, TTEntry returnValue) {
		if (replaceIfDeeper.contains(key, pr)) {
			long value = replaceIfDeeper.get(key);
			decodeInto(value, returnValue);
			return true;
		} else if (alwaysReplace.contains(key, pr)){
			long value = alwaysReplace.get(key);
			decodeInto(value, returnValue);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void reset() {
		replaceIfDeeper.reset();
		alwaysReplace.reset();
		
	}
}
