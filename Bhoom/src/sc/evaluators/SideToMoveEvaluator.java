package sc.evaluators;

import sc.engine.EngineBoard;

/**
 * Evaluates from the perspective of the side to move; i.e. better evaluations for
 * the side to move are higher.
 * @author Shiva
 *
 */
public class SideToMoveEvaluator extends WhiteEvaluator {

	@Override
	public int evaluate(EngineBoard board) {
		int eval = super.evaluate(board);
		boolean whiteToMove = board.getWhiteToMove();
		return whiteToMove ? eval : -eval;
	}

	@Override
	public int[] evalComponents(EngineBoard board) {
		boolean whiteToMove = board.getWhiteToMove();
		int[] comps = super.evalComponents(board);
		for(int i = 0; i < comps.length; i++) {
			comps[i] = whiteToMove ? comps[i] : -comps[i];
		}
		return comps;
	}

	
}
