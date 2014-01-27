package sc.apps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
import sc.encodings.Encodings;
import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EngineStats;
import sc.engine.Evaluator;
import sc.engine.SearchEngine;
import sc.engine.ThinkingListener;
import sc.engine.engines.IDAspWinEngine;
import sc.engine.engines.MTDFEngine;
import sc.engine.movesorter.SeeHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.gui.ChessBoardPanel;
import sc.gui.ChessboardListener;
import sc.gui.ThinkingPanel;
import sc.testing.ExternalUCI;
import sc.testing.ExternalUCIEngine;
import sc.util.BoardUtils;
import sc.util.TextTransfer;
import sc.visualdebug.SearchTreeBuilder;
import sc.visualdebug.SearchTreePanel;

public class HumanVsEngine implements ChessboardListener, ThinkingListener {

	ExecutorService exec = Executors.newSingleThreadExecutor();

	private SearchEngine engine;
	// private SearchEngine black;
	private EngineBoard board;
	private EngineBoard guiboard;
	ChessBoardPanel cpan;
	ThinkingPanel tpan = new ThinkingPanel();
	SearchTreeBuilder stb = new SearchTreeBuilder();

	JButton fwd = new JButton(">");
	JButton back = new JButton("<");
	JButton search = new JButton("Search");
	JButton copyFen = new JButton("FEN->Clipboard");
	JTextField fenField = new JTextField(60);
	TextTransfer textTransfer = new TextTransfer();
	JFrame frame;

	private boolean engineIsWhite;

	public HumanVsEngine(SearchEngine engine, boolean white) {
		this.engine = engine;
		this.engineIsWhite = white;
		this.board = new EBitBoard();
		this.guiboard = new EBitBoard();
		board.initializeStandard();
		guiboard.initializeStandard();
		cpan = new ChessBoardPanel(guiboard);
		cpan.setPreferredSize(new Dimension(300, 300));
		cpan.setFlipped(engineIsWhite);
		cpan.addListener(this);
		engine.setThinkingListener(this);
		tpan.setPreferredSize(new Dimension(900, 300));
		frame = new JFrame();

		JFrame fr = frame;
		fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fr.setLayout(new BorderLayout());
		fr.add(fenComponent(), BorderLayout.NORTH);
		fr.add(cpan, BorderLayout.WEST);
		fr.add(tpan, BorderLayout.CENTER);
		fr.add(buttons(), BorderLayout.SOUTH);
		configureBoard();
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
					configureBoard();
				} else if ("search".equals(ac)) {
					// stb.fen = BoardUtils.getFen(board);
					showSearch();
				} else if ("fen".equals(ac)) {
					textTransfer.setClipboardContents(BoardUtils
							.getFen(guiboard));
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

	private int playMove() {
		boolean whiteToMove = board.getWhiteToMove();
		if (whiteToMove != engineIsWhite) {
			return 0;
		}
		tpan.clear();
		final int move = engine.searchByDepth(board, 7).line[0];
		// final int move = eng.searchByTime(board, 10000).line[0];
		if (move != 0) {
			board.makeMove(move, false);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showMoveOnBoard(move);
				}
			});
		} else {
//			throw new RuntimeException("Zero move!");
		}

		return move;
	}


	// Should be called from the event proc thread.
	private void showMoveOnBoard(final int move) {
		guiboard.makeMove(move, false);
		cpan.setLastMoveTo(Encodings.getToSquare(move));
		configureBoard();
		frame.repaint(0, 0, frame.getWidth(), frame.getHeight());
	}
	
	@Override
	public void userMadeMove(short fromSquare, short toSquare) {
		tpan.clear();
		int[] moveArr = new int[128];
		int numMoves = board.getMoveGenerator().fillLegalMoves(moveArr, 0);
		for (int i = 0; i < numMoves; i++) {
			int cm = moveArr[i];
			short from = Encodings.getFromSquare(cm);
			short to = Encodings.getToSquare(cm);
			if (fromSquare == from && toSquare == to) {
				board.makeMove(cm, false);
				showMoveOnBoard(cm);
				break;
			}
		}
	}

	@Override
	public void thinkingUpdate(String str) {
		tpan.thinkingUpdate(board, str);
	}
	
	private void showSearch() {
		boolean whiteToMove = board.getWhiteToMove();
		if (whiteToMove != engineIsWhite) {
			return;
		}
		stb.fen = BoardUtils.getFen(board);
		engine.setListener(stb);
		stb.startSearch();
		playMove();
		engine.setDefaultListener();

		SearchTreePanel span = new SearchTreePanel();
		span.setPreferredSize(new Dimension(1200, 500));
		span.evalPanel.setEvaluator(engine.getEvaluator());
		span.setBuilder(stb);
		JFrame fr = new JFrame("Search");
		fr.setLayout(new BorderLayout());
		JScrollPane scp = new JScrollPane(span);
		fr.add(scp, BorderLayout.CENTER);
		fr.add(span.boardEvalPanel, BorderLayout.EAST);
		fr.pack();
		fr.setVisible(true);
	}

	private void configureBoard() {
		boolean engineToMove = guiboard.getWhiteToMove() == engineIsWhite;

		fwd.setEnabled(engineToMove);
		search.setEnabled(engineToMove);
		cpan.setAcceptMouseMoves(!engineToMove);
	}

	public static void main(String[] args) {

		// SearchEngine white = new GEngine("white", new MaxPlayerEvaluator(),
		// 2);
		SideToMoveEvaluator sev9111 = new SideToMoveEvaluator();
//		sev9111.setWeights(1, 1, 1, 1);
		SideToMoveEvaluator sev0000 = new SideToMoveEvaluator();
		sev0000.setWeights(0, 1, 1, 0);
//		SearchEngine black = new MTDFEngine("mtdf", sev9111,
//				new AlwaysReplace(), new SeeHashSorter(), true, true, true, true, true);
//		SearchEngine black = new IDAspWinEngine("idAsp", sev9111, new
//				AlwaysReplace(), new SeeHashSorter(), 200, true, true, true, true, true);

		SearchEngine black = new UCIWrapper(new ExternalUCIEngine("java", "-Xmx1024M", "-jar", "C:\\Users\\Shiva\\chess-2013\\Flux-2.2.1.jar"));
		
		HumanVsEngine eve = new HumanVsEngine(black,false);
		// eve.playGame(board, white, black, pan, guiboard);

	}

	static class UCIWrapper implements SearchEngine {

		ExternalUCI uci;
		
		public UCIWrapper(ExternalUCIEngine externalUCIEngine) {
			uci = externalUCIEngine;
			try {
				uci.startup();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public String name() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setEvaluator(Evaluator eval) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setListener(EngineListener listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDefaultListener() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setThinkingListener(ThinkingListener listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Continuation searchByDepth(EngineBoard board, int searchDepth) {
			uci.getBestMove(board, searchDepth, 1000000);
			Continuation c = new Continuation();
			c.line[0] = uci.getMove();
			return c;
		}

		@Override
		public Continuation searchByTime(EngineBoard board, long msTime) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Continuation search(EngineBoard board, int depth,
				int engineTime, int engineInc, int movetime) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EngineStats getEngineStats() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Evaluator getEvaluator() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}