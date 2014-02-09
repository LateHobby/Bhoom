package sc.engine.engines;

import java.util.HashMap;
import java.util.Map;

import sc.bboard.PositionInfo;
import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.util.DeepEquals;
import sc.util.ObjectCloner;
import sc.util.StringDump;

public class DebuggingCTestEngine extends CTestEngine {

	Map<Long, Integer> evalTestMap = new HashMap<Long, Integer>();
	Map<Long, PositionInfo> posInfoMap = new HashMap<Long, PositionInfo>();

	public DebuggingCTestEngine(String name, SearchMode mode, Evaluator eval,
			EvalTT ttable, MoveSorter sorter) {
		super(name, mode, eval, ttable, sorter);
		// TODO Auto-generated constructor stub
	}

	public DebuggingCTestEngine(String name, SearchMode mode, int aspWin,
			Evaluator eval, EvalTT ttable, MoveSorter sorter) {
		super(name, mode, aspWin, eval, ttable, sorter);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int getStaticEval(EngineBoard board) {
		int staticEval = super.getStaticEval(board);
//		checkStaticEval(board.getZobristKey(), staticEval,
//				board.getPositionInfo());
		return staticEval;
	}

	// Make sure that for equal zobrist keys the corresponding PositionInfo objects 
	// are equal.
	private void checkStaticEval(long key, int staticEval, PositionInfo posInfo) {

		if (evalTestMap.containsKey(key)) {
			if (staticEval != evalTestMap.get(key)) {
				PositionInfo oldPosInfo = posInfoMap.get(key);
				boolean posInfoEqual = DeepEquals.deepEquals(oldPosInfo,
						posInfo);
				System.out.println(StringDump.dump(oldPosInfo));
				System.out.println(StringDump.dump(posInfo));
				throw new RuntimeException(
						"Static eval mismatch: posInfoEqual=" + posInfoEqual);
			}
			
		}
		try {
			posInfoMap.put(key, (PositionInfo) ObjectCloner.deepCopy(posInfo));
			evalTestMap.put(key, staticEval);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
