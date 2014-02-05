package sc.engine;

import sc.SPCBoard;
import sc.bboard.PositionInfo;
/**
 * EngineBoards exposes more of the internals of the bitboard to help in evaluation.
 * Evaluators must not change any internal bboard variables.
 * 
 * @author Shiva
 *
 */
public interface EngineBoard extends SPCBoard {

	long getZobristKey();
	
	/**
	 * Returns an array of the internal bitboards of the board, indexed by the piece 
	 * definitions in Encoding.
	 * @return
	 */
	PositionInfo getPositionInfo();

	boolean drawByRepetition();

	void makeNullMove();

	void undoNullMove();

	long getPawnZobristKey(boolean white);

	boolean drawByInsufficientMaterial();
	
	
}
