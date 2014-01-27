package sc.engine;


/**
 * This implementation does a full quiescent search over all captures once the
 * search depth has been reached.
 * @author Shiva
 *
 */
public class NLimitedDepthQuiescenceEngine extends NFullQuiescenceEngine {

	private int quiescenceDepth;
	
	public NLimitedDepthQuiescenceEngine(String name, Evaluator eval, int searchDepth) {
		super(name, eval, searchDepth);
		quiescenceDepth = searchDepth;
	}

	
	@Override
	protected int staticEval(int entryMove, EngineBoard board,
			boolean maximizingPlayer, int alpha, int beta) {
		boolean inCheck = board.kingInCheck(board.getWhiteToMove());
		return quiescence(entryMove, board, quiescenceDepth, maximizingPlayer, alpha, beta, inCheck);
	}



	
}
