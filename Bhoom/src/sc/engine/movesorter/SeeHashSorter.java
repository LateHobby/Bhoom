package sc.engine.movesorter;

import sc.engine.EngineBoard;
import sc.engine.MoveSorter;

public class SeeHashSorter extends SeeMoveSorter implements MoveSorter {
	
	
	
	public SeeHashSorter() {
		
	}
	
	@Override
	public void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves,
			int numMoves) {
		super.sortMoves(board, ply, hashMove, moves, numMoves);
		
		// move hash move to front
		
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
	

}
