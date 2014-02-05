package sc.engine;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import sc.engine.SearchEngine.Continuation;
import sc.util.TTable.TTEntry;

/**
 * Provides a debug facility. The engine makes calls to this class if the trace variable is turned on.
 * @author Shiva
 *
 */
public class Tracer implements EngineListener {

	private DataOutputStream dos;
	
	public Tracer(File file) {
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void enteredNode(int alpha, int beta, int depthLeft, int ply,
			int move, int flags) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exitNode(int eval) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void searchResult(Continuation cont) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startSearch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ttableHit(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void futilityPrune(int terminalEval, int mvp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abandonSearch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void store(TTEntry stored) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void retrieve(TTEntry stored) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void staticEval(int staticEval) {
		// TODO Auto-generated method stub
		
	}

	
}
