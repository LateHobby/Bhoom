package sc.engine;



public interface SearchEngine {
	
	public static class Continuation {
		public int[] line = new int[100];
		public int eval;
	}
	
	public String name();
	
	public void setEvaluator(Evaluator eval);
	
	public void setListener(EngineListener listener);

	public void setDefaultListener();

	public void setThinkingListener(ThinkingListener listener);

	Continuation searchByDepth(EngineBoard board, int searchDepth);

	Continuation searchByTime(EngineBoard board, long msTime);

	public Continuation search(EngineBoard board, int depth, int engineTime,
			int engineInc, int movetime, int oppTime, int oppInc);

	public EngineStats getEngineStats();

	public Evaluator getEvaluator();
}
