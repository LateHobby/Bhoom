package sc.visualdebug;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.Evaluator;
import sc.engine.See;
import sc.gui.ChessBoardPanel;
import sc.util.BoardUtils;
import sc.util.PrintUtils;
import sc.visualdebug.SearchTreeBuilder.Node;

/**
 * Shows a search tree for debugging
 * 
 * @author Shiva
 * 
 */
public class SearchTreePanel extends JPanel implements MouseListener {

	static final int NODE_SIZE = 100;
	private static final int HSEP = NODE_SIZE;
	private static final int VSEP = 2 * NODE_SIZE;

	private static final Color QUIESCENT_INNER_BORDER = new Color(0x0099ff);
	private static final Color LEAF_OUTER_BORDER = new Color(0xff00ff);
	private static final Color NULLMOVE_ZW_BACKGROUND = new Color(0xff9966);
	private static final Color NULLMOVE_QUIESCENT_BACKGROUND = new Color(0xff66ff);
	private static final Color LMR_ZW_BACKGROUND = new Color(0x00ccff);
	private static final Color LMR_FULL_BACKGROUND = new Color(0x00cc99);
	private static final Color DEFAULT_NODE_BACKGROUND = new Color(0xffffff);
	private static final Color SELECTED_BACKGROUND = new Color(0xffff99);
	private static final Color RESULT_COLOR = new Color(0x33CC33);
	private static final Color PANEL_BACKGROUND = new Color(0xeeeeee);
	private static final Color NODE_LABEL_BACKGROUND = new Color(0xeeeeee);
	private static final Color NODE_LABEL_FOREGROUND = new Color(0x555555);
	
	Map<Node, NodeComp> visibleNodes = new HashMap<Node, NodeComp>();
	Map<Node, NodeComp> expandedNodes = new HashMap<Node, NodeComp>();

	See see = new See();
	
	private SearchTreeBuilder builder;
	private JLabel nodeLabel = new JLabel();
	private NodeComp mouseOverComp;
	private NodeComp boardComp;
	
	private EBitBoard board = new EBitBoard();
	public ChessBoardPanel cpanel = new ChessBoardPanel(board);
	public EvalPanel evalPanel = new EvalPanel();
	public JPanel boardEvalPanel = new JPanel();
	
	public SearchTreePanel() {
		super();
		setLayout(null);
		setBackground(PANEL_BACKGROUND);
		
		nodeLabel.setOpaque(true);
		nodeLabel.setBackground(NODE_LABEL_BACKGROUND);
		nodeLabel.setForeground(NODE_LABEL_FOREGROUND);
		cpanel.setPreferredSize(new Dimension(300, 300));
		cpanel.setSize(new Dimension(300, 300));
		boardEvalPanel.setLayout(new BorderLayout());
		boardEvalPanel.add(cpanel, BorderLayout.NORTH);
		boardEvalPanel.add(evalPanel, BorderLayout.SOUTH);
	}

	public void setBuilder(SearchTreeBuilder builder) {
		this.builder = builder;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,    
				RenderingHints.VALUE_ANTIALIAS_ON);
		if (builder != null && expandedNodes.size() == 0 && builder.root != null) {
			toggleExpanded(builder.root);
		}
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		paintChildren(g);
		g.setColor(Color.black);
		for (NodeComp comp : visibleNodes.values()) {
			Node n = comp.node;
			for (Node child : n.children) {
				NodeComp ccomp = visibleNodes.get(child);
				if (ccomp != null) {
					Point from = SwingUtilities.convertPoint(comp, comp.lower,
							this);
					Point to = SwingUtilities.convertPoint(ccomp, ccomp.upper,
							this);
					g.setColor(Color.black);
					g.drawLine(from.x, from.y, to.x, to.y);
				}
			}
		}
		if (mouseOverComp != null) {
			nodeLabel.setText(getNodeDescription(mouseOverComp.node));
			nodeLabel.setSize(nodeLabel.getPreferredSize());
			Graphics2D g2d = (Graphics2D) g.create();
			int dx = mouseOverComp.getX();
			int dy = Math.max(0,mouseOverComp.getY() - nodeLabel.getHeight());
			g2d.translate(dx, dy);
			g2d.setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.5f));
			nodeLabel.paint(g2d);
			g2d.translate(-dx, -dy);
			
		}
	}

	private void calcLayout() {
		Map<Integer, Integer> widthByDepth = new HashMap<Integer, Integer>();
		calcLayout(builder.root, 0, widthByDepth);
		int width = 0;
		for (int w : widthByDepth.values()) {
			if (w > width) {
				width = w;
			}
		}
		Dimension newSize = new Dimension(width, (widthByDepth.size() + 1) * VSEP);
		setPreferredSize(newSize);
		setSize(newSize);
		removeAll();
		
		for (NodeComp comp : visibleNodes.values()) {
			add(comp);
		}
		repaint();
	}

	private void calcLayout(Node node, int depth, Map<Integer, Integer> xByDepth) {
		Integer ix = xByDepth.get(depth);
		int x = ix == null ? 0 : ix.intValue();
		NodeComp comp = visibleNodes.get(node);
		comp.setLocation(x + HSEP, (depth + 1) * VSEP);
		xByDepth.put(depth, x + NODE_SIZE + HSEP);
		if (expandedNodes.containsKey(node)) {
			for (int i = 0; i < node.numChildren; i++) {
				Node child = node.children[i];
				calcLayout(child, depth + 1, xByDepth);
			}
		}
	}

	private void toggleExpanded(Node node) {
		if (!expandedNodes.containsKey(node)) {
			NodeComp comp = visibleNodes.get(node);
			if (comp == null) {
				comp = node.maximizing ? new MaxNode(node) : new MinNode(node);
				comp.addMouseListener(this);
				visibleNodes.put(node, comp);

			}
			expandedNodes.put(node, comp);
			for (int i = 0; i < node.numChildren; i++) {
				Node child = node.children[i];
				if (!visibleNodes.containsKey(child)) {
					NodeComp ccomp = child.maximizing ? new MaxNode(child)
							: new MinNode(child);
					ccomp.addMouseListener(this);
					visibleNodes.put(child, ccomp);
				}
			}
			
		} else {
			NodeComp comp = visibleNodes.get(node);
			expandedNodes.remove(node);
			for (int i = 0; i < node.numChildren; i++) {
				removeFromVisible(node.children[i]);
			}
		}
		calcLayout();
	}

	private void removeFromVisible(Node node) {
		visibleNodes.remove(node);
		for (int i = 0; i < node.numChildren; i++) {
			removeFromVisible(node.children[i]);
		}
		
	}

	private void configureBoard(Node node) {
		
		if (builder.fen != null) {
			BoardUtils.initializeBoard(board, builder.fen);
		} else {
			board.initializeStandard();
		}
		if (node.parent == null) {
			return;
		}
		int[] moves = new int[50];
		int index = 0;
		moves[index++] = node.lastMove;
		while (node.parent != null) {
			node = node.parent;
			moves[index++] = node.lastMove;
		}
		int seeEval = 0;
		for (int i = index-1; i >= 0; i--) {
			if (i == 0) {
				try {
					if (moves[i] != 0) {
						seeEval = see.evaluateMove(board, moves[i], evalPanel.eval);
					}
				} catch (Throwable t) {
					System.out.printf("Exception at move %d", moves[i]);
				}
			}
			if (moves[i] == 0) {
				board.makeNullMove();
			} else {
				board.makeMove(moves[i], false);
			}
		}
		evalPanel.evaluate(board, seeEval);
	}
	
	private String getNodeDescription(Node node) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><h2>");
		sb.append("&alpha;=" + node.alpha);
		sb.append("<br/>");
		sb.append("Eval=" + node.eval);
		sb.append("<br/>");
		sb.append("&beta;=" + node.beta);
		sb.append("<br/>");
		sb.append("Move=" + PrintUtils.notation(node.lastMove));
		sb.append("<br/>");
		sb.append("beta-cutoff=" + node.betaCutoff);
		sb.append("<br/>");
		sb.append("ttHit=" + getTTHitString(node.flags));
		sb.append("<br/>");
		sb.append("futilityPruned=" + node.futilityPrune);
		sb.append("<br/>");
		sb.append("teval=" + node.teval);
		sb.append("<br/>");
		sb.append("mvp=" + node.mvp);
		sb.append("<br/>");
		sb.append("</h2></body></html>");
		return sb.toString();
	}

	private String getTTHitString(int flags) {
		if ((flags & EngineListener.TT_EXACT) != 0) {
			return "EXACT";
		} else if ((flags & EngineListener.TT_LOWERBOUND) != 0) {
			return "LOWERBOUND";
		} else if ((flags & EngineListener.TT_UPPERBOUND) != 0) {
			return "UPPERBOUND";
		}
		return "";
	}

	private class NodeComp extends Component {
		
		Node node;
		Dimension childBox;
		Shape shape;
		Shape innerShape;
		Shape outerShape;
		
		Point upper = new Point(NODE_SIZE / 2, 0);
		Point lower = new Point(NODE_SIZE / 2, NODE_SIZE);

		NodeComp(Node node) {
			this.node = node;
			childBox = new Dimension(node.children.length * (NODE_SIZE + HSEP)
					+ HSEP, NODE_SIZE);
			setSize(new Dimension(NODE_SIZE, NODE_SIZE));
		}

		@Override
		public void paint(Graphics g0) {
			Graphics2D g = (Graphics2D) g0;
			boolean isNullMoveZw = (node.flags & EngineListener.NULLMOVE_ZW) != 0;
			boolean isNullMoveQ = (node.flags & EngineListener.NULLMOVE_QUIESCENT) != 0;
			boolean isLmrZw = (node.flags & EngineListener.LMR_ZW) != 0;
			boolean isLmrFull = (node.flags & EngineListener.LMR_FULL) != 0;
			g.setColor(SearchTreePanel.this.boardComp == this ? SELECTED_BACKGROUND : 
				isNullMoveZw ? NULLMOVE_ZW_BACKGROUND : 
					isNullMoveQ ? NULLMOVE_QUIESCENT_BACKGROUND:
						isLmrZw ? LMR_ZW_BACKGROUND :
							isLmrFull ? LMR_FULL_BACKGROUND :
								node.quiescent ? QUIESCENT_INNER_BORDER : 
									DEFAULT_NODE_BACKGROUND);
			String descString = isNullMoveZw ? "NULL_ZW" : 
				isNullMoveQ ? "NULL_Q" :
					isLmrZw ? "LMR_ZW" :
						isLmrFull ? "LMR_FULL" :
							"";
			g.fill(shape);
			if (node.isLeaf()) {
				g.setColor(LEAF_OUTER_BORDER);
				g.draw(outerShape);
			} else {
				g.setColor(Color.black);
				g.draw(shape);
			}
			if (node.onResultPath) {
				g.setColor(RESULT_COLOR);
				g.drawOval(0, 0, NODE_SIZE, NODE_SIZE);
			}
			g.setColor(Color.black);
			int y = NODE_SIZE-60;
			int x = NODE_SIZE/8;
			g.drawString("" + node.eval, NODE_SIZE/3, y);
			g.drawString("[" + node.alpha + "," + node.beta + "]", x, y+12);
			g.drawString(PrintUtils.notation(node.lastMove), x, y+24);
			g.drawString("" + node.nodeCount, x, y+36);
			g.drawString(descString, x, y+48);
			
			boolean white = ((node.flags & EngineListener.WHITE_TO_MOVE) != 0);
			
			if (white) {
				g.setColor(Color.white);
				g.fillRect(0, 0, 10,10);
				g.setColor(Color.black);
				g.drawRect(0, 0, 10,10);
			} else {
				g.setColor(Color.black);
				g.fillRect(0, 0, 10,10);
				g.setColor(Color.white);
				g.drawRect(0, 0, 10,10);
			}
		}
	}

	private class MaxNode extends NodeComp {
		public MaxNode(Node node) {
			super(node);
			outerShape = new Rectangle(0, 0, NODE_SIZE - 1, NODE_SIZE - 1);
			shape = new Rectangle(1, 1, NODE_SIZE - 2, NODE_SIZE - 2);
			innerShape = new Rectangle(2, 2, NODE_SIZE - 3, NODE_SIZE - 3);
		}

		
	}

	private class MinNode extends NodeComp {

		

		public MinNode(Node node) {
			super(node);
			Polygon pOuter = new Polygon(new int[] { NODE_SIZE / 2, NODE_SIZE - 1, 0 },
					new int[] { 0, NODE_SIZE - 1, NODE_SIZE - 1 }, 3);
			Polygon p = new Polygon(new int[] { NODE_SIZE / 2, NODE_SIZE - 2, 1 },
					new int[] { 1, NODE_SIZE - 2, NODE_SIZE - 2 }, 3);
			Polygon pInner = new Polygon(new int[] { NODE_SIZE / 2, NODE_SIZE - 3, 2 },
					new int[] { 2, NODE_SIZE - 3, NODE_SIZE - 3 }, 3);
			shape = p;
			outerShape = pOuter;
			innerShape = pInner;
		}

		
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getClickCount() == 2) {
			NodeComp n = (NodeComp) arg0.getSource();
			toggleExpanded(n.node);
			repaint();
		} 
		if (SwingUtilities.isRightMouseButton(arg0)) {
			NodeComp n = (NodeComp) arg0.getSource();
			boardComp = n;
			configureBoard(n.node);
			cpanel.repaint();
			
		} 
	}

	

	@Override
	public void mouseEntered(MouseEvent arg0) {
		NodeComp n = (NodeComp) arg0.getSource();
		mouseOverComp = n;
		repaint();

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		mouseOverComp = null;
		repaint();

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
	
	public static class EvalPanel extends JPanel {
		
		private Evaluator eval;
		private JLabel[] evalComponents;
		private String[] evalNames;
		private JLabel seeVal = new JLabel();
		EvalPanel() {
			
		}
		
		public void evaluate(EngineBoard board, int seeEval) {
			int[] comps = eval.evalComponents(board);
			for (int i = 0; i < comps.length; i++) {
				evalComponents[i].setText(evalNames[i] + ": " + comps[i]);
			}
			seeVal.setText("SeeEval: " + seeEval);
		}
		public void setEvaluator(Evaluator eval) {
			this.eval = eval;
			computeLayout();
		}

		private void computeLayout() {
			removeAll();
			evalNames = eval.evalNames();
			evalComponents = new JLabel[evalNames.length];
			setLayout(new GridLayout(evalComponents.length + 1, 1));
			for (int i = 0; i < evalComponents.length; i++) {
				evalComponents[i] = new JLabel(evalNames[i]);
				add(evalComponents[i]);
			}
			add(seeVal);
			setSize(getPreferredSize());
			getParent().doLayout();
		}
	}
}
