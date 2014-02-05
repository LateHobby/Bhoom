package sc.visualdebug;

import sc.engine.EngineListener;
import sc.engine.SearchEngine.Continuation;
import sc.util.TTable.TTEntry;


public class SearchTreeBuilder implements EngineListener {

	public Node root;
	public Node currentNode;
	public String fen;
	
	@Override
	public void startSearch() {
		root = null;
		currentNode = null;
	}
	
	@Override
	public void enteredNode(int alpha, int beta, int depthLeft, int ply, int lastMove, int flags) {
		Node n = new Node(depthLeft <= 0, alpha, beta, lastMove);
		if (root == null) {
			root = n;
		} else {
			currentNode.add(n);
		}
		currentNode = n;
		currentNode.flags = flags;
	}

	@Override
	public void exitNode(int score) {
		
		currentNode.eval = score;
		if (-score >= -currentNode.alpha) {
			currentNode.betaCutoff = true;
		}
		currentNode = currentNode.parent;

	}
	
	@Override
	public void searchResult(Continuation pv) {
		markResultNodes(root, pv, 0);
		countAndMarkMaximizing(root, true);
		
	}
	
	@Override
	public void ttableHit(int type) {
		currentNode.flags |= type;
		
	}
	
	@Override
	public void futilityPrune(int terminalEval, int mvp) {
		currentNode.futilityPrune = true;
		currentNode.mvp = mvp;
		currentNode.teval = terminalEval;
		
	}
	
	private int countAndMarkMaximizing(Node node, boolean b) {
		node.maximizing = b;
		int sum = 1;
		for (int i = 0; i < node.numChildren; i++) {
			Node child = node.children[i];
			sum += countAndMarkMaximizing(child, !b);
		}
		node.nodeCount = sum;
		return sum;
	}

	private void markResultNodes(Node node, Continuation pv, int index) {
		if (pv.line[index] == 0) {
			return;
		}
		node.onResultPath = true;
		for (int i = 0; i < node.numChildren; i++) {
			Node child = node.children[i];
			if (child.lastMove == pv.line[index]) {
				markResultNodes(child, pv, index+1);
				break;
			}
		}
		
	}

	static public class Node {
		
		public int flags;
		public int teval;
		public int mvp;
		public boolean futilityPrune;
		public Node parent = null;
		public Node[] children = new Node[10];
		public int numChildren = 0;
		public boolean maximizing = true;
		public int alpha;
		public int beta;
		public int eval;
		public int lastMove;
		public boolean quiescent;
		public boolean onResultPath;
		public boolean betaCutoff;
		public int nodeCount;
		public int staticEval;
		
		Node(boolean quiescent, int alpha, int beta, int lastMove) {
			this.quiescent = quiescent;
			this.alpha = alpha;
			this.beta = beta;
			this.lastMove = lastMove;
		}
		
		
		public void add(Node child) {
			if (numChildren == children.length) {
				doubleChildren();
			}
			children[numChildren++] = child;
			child.parent = this;
		}
		
		private void doubleChildren() {
			Node[] doubled = new Node[numChildren*2];
			System.arraycopy(children, 0, doubled, 0, numChildren);
			children = doubled;
		}

		public boolean isLeaf() {
			return numChildren == 0;
		}
	}

	@Override
	public void abandonSearch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void store(TTEntry stored) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void retrieve(TTEntry stored) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void staticEval(int staticEval) {
		currentNode.staticEval = staticEval;
		
	}

	

	

}
