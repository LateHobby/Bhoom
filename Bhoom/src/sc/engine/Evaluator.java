package sc.engine;

/**
 * @author Shiva
 *
 */
public interface Evaluator {

	static public short MIN_EVAL = Short.MIN_VALUE + 2;
	static public short MAX_EVAL = -MIN_EVAL;
	static public short MATE_BOUND = MAX_EVAL - 1;
	
	public int evaluate(EngineBoard board);
	
	public void setWeight(int weight);
	
	public int getWeight();
	
	public String[] evalNames();
	
	public int[] evalComponents(EngineBoard board);

	public int pieceWeight(byte piece);
	
}
