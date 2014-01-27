package sc.engine;

public interface MoveSorter {

	void sortMoves(EngineBoard board, int ply, int hashMove, int[] moves, int numMoves);
	
	void incrementHistoryHeuristicArray(int move, boolean increment);
	
	void addToKillerMoves(EngineBoard board, int distanceFromRoot, int move, int hashMove);
	
}
