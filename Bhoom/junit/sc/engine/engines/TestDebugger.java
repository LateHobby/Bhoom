package sc.engine.engines;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import sc.bboard.EBitBoard;
import sc.engine.Evaluator;
import sc.engine.SearchEngine.Continuation;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.BoardUtils;
import sc.util.PrintUtils;
import sc.visualdebug.SearchTreeBuilder;
import sc.visualdebug.SearchTreePanel;

public class TestDebugger {

	private void nonVisualCheck(CTestEngine ce, int depth, String fen) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		Continuation c = ce.simpleAlphaBeta(bb, depth, 0);
		
		Continuation c2 = ce.aspWinSearch(bb, depth, Evaluator.MIN_EVAL);
		
		System.out.println(fen);
		System.out.printf("Found: %s[%d] Expected: %s[%d]\n", 
				PrintUtils.notation(c.line[0]),c.eval, PrintUtils.notation(c2.line[0]), c2.eval);
//		assertEquals(c2.eval, c.eval);
	}
	
	private void visualCheck(CTestEngine ce, int depth, String fen) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		ce.listen = true;
		SearchTreeBuilder stb = new SearchTreeBuilder();
		stb.fen = fen;
		ce.setListener(stb);
		Continuation reference = ce.simpleAlphaBeta(bb, depth, 0);
		display(stb, "SimpleAlphaBeta");
		
		SearchTreeBuilder stb2 = new SearchTreeBuilder();
		stb2.fen = fen;
		ce.setListener(stb2);
		Continuation c2 = ce.aspWinSearch(bb, depth, Evaluator.MIN_EVAL);
		display(stb2, "CEngine");
		
		System.out.println(fen);
		System.out.printf("Found: %s[%d] Expected: %s[%d]\n", 
				PrintUtils.notation(c2.line[0]), c2.eval,
				PrintUtils.notation(reference.line[0]),reference.eval);
//		assertEquals(c2.eval, c.eval);
	}

	void visualCheck(CTestEngine ce1, CTestEngine ce2, int depth, String fen) {
		EBitBoard bb = new EBitBoard();
		BoardUtils.initializeBoard(bb, fen);
		ce1.listen = true;
		SearchTreeBuilder stb = new SearchTreeBuilder();
		stb.fen = fen;
		ce1.setListener(stb);
		Continuation reference = ce1.searchByDepth(bb, depth);
		display(stb, ce1.name());
		
		SearchTreeBuilder stb2 = new SearchTreeBuilder();
		stb2.fen = fen;
		ce2.listen = true;
		ce2.setListener(stb2);
		Continuation c2 = ce2.searchByDepth(bb, depth);
		display(stb2, ce2.name());
		ce1.listen = false;
		ce2.listen = false;
		
		System.out.println(fen);
		System.out.printf("Found: %s[%d] Expected: %s[%d]\n", 
				PrintUtils.notation(c2.line[0]), c2.eval,
				PrintUtils.notation(reference.line[0]),reference.eval);
//		assertEquals(c2.eval, c.eval);
	}
	private void display(SearchTreeBuilder stb, String title) {
		SearchTreePanel stp = new SearchTreePanel();
		stp.setBuilder(stb);
		JFrame fr = new JFrame(title);
		fr.setLayout(new BorderLayout());
		fr.add(new JScrollPane(stp), BorderLayout.CENTER);
		fr.add(stp.boardEvalPanel, BorderLayout.EAST);
		stp.evalPanel.setEvaluator(new SideToMoveEvaluator());
		fr.setVisible(true);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestDebugger vdb = new TestDebugger();
		
		int depth = 4;
		CTestEngine ce = new CTestEngine("AspWin", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		boolean[] flags = new boolean[]{true, true, true, false, true, true, false};
		ce.setFlags(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
		String fen = "1rb2rk1/3nqppp/p1n1p3/1p1pP3/5P2/2NBQN2/PPP3PP/2KR3R w - - 0 0";
//		vdb.visualCheck(ce, 4, fen);
		vdb.nonVisualCheck(ce, 4, fen);


	}

}
