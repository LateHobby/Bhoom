package sc.engine;

import sc.engine.SearchEngine.Continuation;


public interface EngineListener {
	
	static public final int NORMAL = 0;
	static public final int QUIESCENT = 1;
	static public final int LMR_ZW = 2;
	static public final int LMR_FULL = 4;
	static public final int NULLMOVE_ZW = 8;
	static public final int NULLMOVE_QUIESCENT = 16;
	static public final int TT_EXACT = 32;
	static public final int TT_LOWERBOUND = 64;
	static public final int TT_UPPERBOUND = 128;
	static public final int WHITE_TO_MOVE = 256;
	
	public void enteredNode(int alpha, int beta, boolean quiescent, int move, int flags);
	
	public void exitNode(int eval);

	public void searchResult(Continuation cont);
	
	public void startSearch();

	public void ttableHit(int type);

	public void futilityPrune(int terminalEval, int mvp);
		
	
}
