package sc.engine.movesorter;

import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EvalTT;
import sc.engine.MoveSorter;
import sc.util.TTable.TTEntry;

public class HashMoveSorter implements MoveSorter {

	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves,
			int numMoves) {
		swapWithStart(0, moves, hashMove, numMoves);

	}
	
	protected void swapWithStart(int startIndex, int[] moves, int move, int numMoves) {
		for (int i = startIndex + 1; i < numMoves; i++ ) {
			if (moves[i] == move) {
				int temp = moves[i];
				moves[i] = moves[startIndex];
				moves[startIndex] = temp;
				break;
			}
		}
	}

	@Override
	public void incrementHistoryHeuristicArray(int move, boolean increment) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addToKillerMoves(EngineBoard board, int distanceFromRoot,
			int move, int hashMove) {
		// TODO Auto-generated method stub
		
	}

}
