package sc.engine;

public interface EngineStats {

	int getNodes(boolean quiescent);
	
	double getAvgBetaCutoffIndex(boolean quiescent);
	
	double getTTHitPercentage(boolean quiescent);
	
	int getDepth();
	
}
