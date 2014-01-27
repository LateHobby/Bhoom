package sc.engine;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import sc.engine.BaseNegamaxEngine.Moves;
import sc.engine.NFullQuiescenceEngine;
import sc.evaluators.SideToMoveEvaluator;

public class TestQuiescence {

	@Test
	public void testMoveSorting() {
		NFullQuiescenceEngine nfq = new NFullQuiescenceEngine("Test", new SideToMoveEvaluator(), 5);
		Moves m = nfq.new Moves();
		Moves ranks = nfq.new Moves();
		
		Random r = new Random();
		int numMoves = 2;
		for (int i = 0; i < numMoves; i++) {
			int ri = r.nextInt(numMoves);
			ranks.moves[i] = ri;
			m.moves[i] = ri;
		}
		nfq.isortDescending(m, numMoves, ranks);
		for (int i = 0; i < numMoves - 1; i++) {
			assertTrue(ranks.moves[i] >= ranks.moves[i+1]);
		}
	}

}
