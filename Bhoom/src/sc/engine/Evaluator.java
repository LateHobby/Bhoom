package sc.engine;

/**
 * @author Shiva
 *
 */
public interface Evaluator {

	static public short MIN_EVAL = Short.MIN_VALUE + 2;
	static public short MAX_EVAL = -MIN_EVAL;
	static public short MATE_BOUND = MAX_EVAL - 1;
	
	static public int[] STATIC_PIECE_WEIGHTS = new int[] {
		0, // EMPTY = 0;
		1000, // WKING = 1;
		800, // WQUEEN = 2;
		500, // WROOK = 3;
		300, // WBISHOP = 4;
		300, // WKNIGHT = 5;
		100, // WPAWN = 6;
		1000, // BKING = 7;
		800, // BQUEEN = 8;
		500, // BROOK = 9;
		300, // BBISHOP = 10;
		300, // BKNIGHT = 11;
		100 // BPAWN = 12;
	};
	
	public int evaluate(EngineBoard board);
	
	public void setWeight(int weight);
	
	public int getWeight();
	
	public String[] evalNames();
	
	public int[] evalComponents(EngineBoard board);

	public int pieceWeight(byte piece);

	public void reset();
	
}
