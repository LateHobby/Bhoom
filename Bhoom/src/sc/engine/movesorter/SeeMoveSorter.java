package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.See;
import sc.engine.engines.AbstractEngine;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.IntArray;
import sc.util.PrintUtils;
import sc.util.TTable.TTEntry;

public class SeeMoveSorter extends AbstractMoveSorter {

	/*
	 * Monsoon/Typhoon move order
	 * 
Hash move
Winning captures (judged by the SEE)
Even captures (judged by SEE)
Killer moves
Other non-captures (ranked by history value)
Losing captures (judged by SEE)
	 */
	protected int[] seeRanks = new int[128];

	IntArray winningAndEvenCaptures = new IntArray(128);
	IntArray losingCaptures = new IntArray(128);
	IntArray nonCaptures = new IntArray(128);
	IntArray killerMovesArr = new IntArray(8);
	
	See see = new See();
	Evaluator eval = new SideToMoveEvaluator();
	
	public SeeMoveSorter() {
		super();
	}
	
	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves,
			int numMoves) {
		for (int i = 0; i < numMoves; i++) {
			try {
				
				seeRanks[i] = see.evaluateMove(board, moves[i], eval);
				
				hhranks[i] = getHistoryRank(moves[i]);
			} catch (Throwable t) {
				System.out.println(BoardUtils.getFen(board));
				System.out.println(PrintUtils.notation(moves[i]));
				throw t;
			}
		}
		isortDescending(moves, numMoves, seeRanks, hhranks);
		orderMovesFinally(board, ply, moves, numMoves);
		
	}


	protected void orderMovesFinally(EngineBoard board, int ply, int[] moves,
			int numMoves) {
		winningAndEvenCaptures.clear();
		losingCaptures.clear();
		nonCaptures.clear();
		killerMovesArr.clear();
		for (int i = 0; i < numMoves; i++) {
			if (isCapture(board, moves[i])) {
				if (seeRanks[i] >= 0) {
					winningAndEvenCaptures.push(moves[i]);
				} else {
					losingCaptures.push(moves[i]);
				}
			} else if (isKillerMove(ply, moves[i])){
				killerMovesArr.push(moves[i]);
			} else {
				nonCaptures.push(moves[i]);
			}
		}
		int index = 0;
		index = append(moves, winningAndEvenCaptures, index);
		index = append(moves, killerMovesArr, index);
		index = append(moves, nonCaptures, index);
		index = append(moves, losingCaptures, index);
		if (index != numMoves) {
			throw new RuntimeException("Error in move sorting");
		}
	}

	
	

	private int append(int[] moves, IntArray arr, int startIndex) {
		for (int i = 0; i < arr.size(); i++) {
			moves[startIndex + i] = arr.get(i);
		}
		return startIndex + arr.size();
	}



}
