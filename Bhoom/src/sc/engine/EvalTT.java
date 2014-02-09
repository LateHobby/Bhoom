package sc.engine;

import sc.engine.SearchEngine.Continuation;
import sc.util.TTable.TTEntry;

public interface EvalTT {

	void storeToTT(EngineBoard board, TTEntry stored);

	boolean retrieveFromTT(EngineBoard board, TTEntry stored);

	Continuation getContinuation(EngineBoard board, int depth);

	void reset();
	
}
