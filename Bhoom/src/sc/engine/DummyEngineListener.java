package sc.engine;

import sc.engine.SearchEngine.Continuation;


public class DummyEngineListener implements EngineListener {

	@Override
	public void enteredNode(int alpha, int beta, 
			 boolean quiescent, int lastMove, int flags) {

	}

	@Override
	public void exitNode(int score) {

	}

	@Override
	public void searchResult(Continuation cont) {

	}

	@Override
	public void startSearch() {

	}


	@Override
	public void ttableHit(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void futilityPrune(int terminalEval, int mvp) {
		// TODO Auto-generated method stub
		
	}

}
