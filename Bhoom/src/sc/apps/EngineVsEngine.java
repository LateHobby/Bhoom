package sc.apps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.SearchEngine;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.engines.CTestEngine;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.gui.ChessBoardPanel;
import sc.util.BoardUtils;
import sc.util.TextTransfer;
import sc.visualdebug.SearchTreeBuilder;
import sc.visualdebug.SearchTreePanel;

public class EngineVsEngine {

	ExecutorService exec = Executors.newSingleThreadExecutor();

	private SearchEngine white;
	private SearchEngine black;
	private EngineBoard board;
	private EngineBoard guiboard;
	ChessBoardPanel cpan;
	SearchTreeBuilder stb = new SearchTreeBuilder();
	
	JButton fwd = new JButton(">");
	JButton back = new JButton("<");
	JButton search = new JButton("Search");
	JButton copyFen = new JButton("FEN->Clipboard");
	JTextField fenField = new JTextField(60);
	TextTransfer textTransfer = new TextTransfer();
	JFrame frame;
	
	public EngineVsEngine(SearchEngine white, SearchEngine black) {
		this.white = white;
		this.black = black;
		this.board = new EBitBoard();
		this.guiboard = new EBitBoard();
		board.initializeStandard();
		guiboard.initializeStandard();
		cpan = new ChessBoardPanel(guiboard);
		cpan.setPreferredSize(new Dimension(300, 300));
		frame = new JFrame();
		
		JFrame fr = frame;
		fr.setLayout(new BorderLayout());
		fr.add(fenComponent(), BorderLayout.NORTH);
		fr.add(cpan, BorderLayout.CENTER);
		fr.add(buttons(), BorderLayout.SOUTH);
		fr.pack();
		fr.setVisible(true);
	}

	private Component fenComponent() {
		fenField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				BoardUtils.initializeBoard(board, fenField.getText());
				BoardUtils.initializeBoard(guiboard, fenField.getText());
				frame.repaint();
				
			}
			
		});
		JPanel pan = new JPanel();
		pan.add(new JLabel("FEN:"));
		pan.add(fenField);
		return pan;
	}

	private Component buttons() {
		JPanel pan = new JPanel();
		fwd.setActionCommand("fwd");
		back.setActionCommand("back");
		search.setActionCommand("search");
		copyFen.setActionCommand("fen");
		
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ac = arg0.getActionCommand();
				if ("fwd".equals(ac)) {
					fwd.setEnabled(false);
					exec.execute(new Runnable() {

						@Override
						public void run() {
							playMove();
							
						}
						
					});
				} else if ("back".equals(ac)) {
					board.undoLastMove();
					guiboard.undoLastMove();
				} else if ("search".equals(ac)) {
//					stb.fen = BoardUtils.getFen(board);
					showSearch();
				} else if ("fen".equals(ac)) {
					textTransfer.setClipboardContents(BoardUtils.getFen(guiboard));
				}
				frame.repaint();
				
			}

			
		};
		fwd.addActionListener(al);
		back.addActionListener(al);
		search.addActionListener(al);
		copyFen.addActionListener(al);
		
		pan.add(fwd);
		pan.add(back);
		pan.add(search);
		pan.add(copyFen);
		
		return pan;
		
	}

//	private static void playGame(DBitBoard board, FEngine white, FEngine black,
//			ChessBoardPanel pan, DBitBoard guiboard) {
//		board.initializeStandard();
//		guiboard.initializeStandard();
//
//		int moveNum = 1;
//		boolean whiteToMove = true;
//		while (true) {
//			int move = playMove();
//			if (move == 0) {
//				if (board.kingInCheck()) {
//					if (whiteToMove) {
//						System.out.println("White wins.");
//					} else {
//						System.out.println("Black wins.");
//					}
//				} else {
//					System.out.println("Draw by stalemate");
//				}
//				break;
//			}
//			if (whiteToMove) {
//				System.out.print(moveNum++ + ". ");
//			}
//			System.out.println(whiteToMove ? "white" : "black" + ":"
//					+ PrintUtils.notation(move));
//			whiteToMove = !whiteToMove;
//		}
//	}

	private int playMove() {
		Boolean whiteToMove = board.getWhiteToMove();
		SearchEngine eng = whiteToMove ? white : black;
		final int move = eng.searchByDepth(board, 7).line[0];
//		final int move = eng.searchByTime(board, 10000).line[0];
		if (move != 0) {
			board.makeMove(move, false);
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					guiboard.makeMove(move, false);
					cpan.repaint();
					fwd.setEnabled(true);
				}
				
			});
		} else {
			throw new RuntimeException("Zero move!");
		}
		
		
		return move;
	}

	private void showSearch() {
		Boolean whiteToMove = board.getWhiteToMove();
		SearchEngine eng = whiteToMove ? white : black;
		stb.fen = BoardUtils.getFen(board);
		eng.setListener(stb);
		stb.startSearch();
		playMove();
		eng.setDefaultListener();
		
		SearchTreePanel span = new SearchTreePanel();
		span.setPreferredSize(new Dimension(1200, 500));
		span.evalPanel.setEvaluator(eng.getEvaluator());
		span.setBuilder(stb);
		JFrame fr = new JFrame("Search");
		fr.setLayout(new BorderLayout());
		JScrollPane scp = new JScrollPane(span);
		fr.add(scp, BorderLayout.CENTER);
		fr.add(span.boardEvalPanel, BorderLayout.EAST);
		fr.pack();
		fr.setVisible(true);
	}

	public static void main(String[] args) {
		
		SideToMoveEvaluator sev9111 = new SideToMoveEvaluator();
		sev9111.setWeights(1,1,1,1);
		SearchEngine black = new CTestEngine("mtdf", SearchMode.MTDF, sev9111, new AlwaysReplace(), new SeeHashSorter());
		SearchEngine white = new CTestEngine("idAspSee", SearchMode.ASP_WIN, 200, sev9111, new AlwaysReplace(), new SeeHashSorter());
		

		EngineVsEngine eve = new EngineVsEngine(white, black);
//		eve.playGame(board, white, black, pan, guiboard);

	}

}