package sc.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.engine.ThinkingListener;
import sc.util.StreamGobbler.OnLineListener;

public class ExternalUCIEngine implements ExternalUCI, OnLineListener {

	boolean debug = true;

	private boolean processStarted;
	private String[] cmd;
	private StreamGobbler gobbler;

	ThinkingListener listener;
	
	int move;
	int eval;
	int nodes;
	private Process proc;
	private BufferedWriter bos;
	private Object synchronizer = new Object();
	private String lastString;
	// private String waitString;

	EBitBoard localBoard = new EBitBoard();

	public ExternalUCIEngine(String... cmd) {
		this.cmd = cmd;
	}

	@Override
	public void shutDown() {
		if (proc != null) {
			proc.destroy();
		}
	}

	@Override
	public void startup() throws IOException {
		if (proc != null) {
			throw new RuntimeException("Process already started!");
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		proc = pb.start();
		gobbler = new StreamGobbler("UCI", proc.getInputStream(), this);
		gobbler.start();
		bos = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
		processStarted = true;
		send("uci");
		waitfor("uciok");
		send("ucinewgame");
	}

	@Override
	public int getNodes() {
		return nodes;
	}
	
	@Override
	public int getEval() {
		return eval;
	}

	@Override
	public int getMove() {
		return move;
	}

	@Override
	public int evaluateMove(EngineBoard board, int move, int depth, int timeMs) {
		try {
			send("ucinewgame");
			send("isready");
			waitfor("readyok");
			String fen = BoardUtils.getFen(board);
			BoardUtils.initializeBoard(localBoard, fen);
			if (!localBoard.makeMove(move, true)) {
				throw new RuntimeException("Invalid move");
			}
			fen = BoardUtils.getFen(localBoard);
			send("position fen " + fen);
			send(buildGoString(depth - 1, timeMs));
			waitfor("bestmove");
			send("stop");
			return -eval;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	

	@Override
	public int evaluatePosition(EngineBoard board, int depth, int timeMs) {
		try {
			send("isready");
			waitfor("readyok");
			String fen = BoardUtils.getFen(board);
			BoardUtils.initializeBoard(localBoard, fen);
			send("position fen " + fen);
			send(buildGoString(depth, timeMs));
			waitfor("bestmove");
			return eval;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void getBestMove(EngineBoard board, int depth, int timeMs) {
		evaluatePosition(board, depth, timeMs);

	}

	private String buildGoString(int move, int depth, int timeMs) {
		return String.format("go searchmoves %s depth %d", PrintUtils.notation(move), depth, timeMs);
	}
	
	private String buildGoString(int depth, int timeMs) {
		return String.format("go depth %d", depth, timeMs);
	}

	private void waitfor(String string) {
		// waitString = string;
		while (true) {
			try {
				synchronized (synchronizer) {
					synchronizer.wait(1000);
					if (string != null && lastString.startsWith(string)) {
						break;
					} else if (debug) {
						System.out.println("Waiting for [" + string
								+ "]; currently see [" + lastString + "]");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void send(String str) throws IOException {
		bos.write(str);
		bos.newLine();
		bos.flush();
		if (debug) {
			System.out.println("Sending: " + str);
		}
	}

	@Override
	public void onLine(String line) {
		synchronized (synchronizer) {
			if (debug) {
				System.out.println("Received:" + line);
			}
			line = line.trim();
			boolean notify = true;
			lastString = line;
			if (line.startsWith("bestmove")) {
				String[] sa = line.split("\\s+");
				move = BoardUtils.encodedForm(localBoard, sa[1]);
				if (debug) {
					System.out.println("ExternalUCI: current fen "
							+ BoardUtils.getFen(localBoard));
					System.out.println("ExternalUCI: move [" + sa[1]
							+ "] encoded as:" + PrintUtils.notation(move));
				}
			}
			if (line.startsWith("info")) {
				String[] sa = line.split("\\s+");
				for (int i = 0; i < sa.length; i++) {
					if (sa[i].equals("cp")) {
						eval = Integer.parseInt(sa[i + 1]);
					}
					if (sa[i].equals("nodes")) {
						nodes = Integer.parseInt(sa[i + 1]);
					}
				}
				if (listener != null) {
					listener.thinkingUpdate(line);
				}
			}
			if (notify) {

				if (debug) {
					System.out.println("Notifying...");
				}
				synchronizer.notifyAll();
			}
		}

	}

	@Override
	protected void finalize() throws Throwable {
		if (proc != null) {
			proc.destroy();
		}
		super.finalize();
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		ExternalUCIEngine uci = new ExternalUCIEngine("BhoomMtdBin.bat");
		String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
		BoardUtils.initializeBoard(uci.localBoard, fen);
		uci.onLine("bestmove e7e5");
		fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
		BoardUtils.initializeBoard(uci.localBoard, fen);
		uci.onLine("bestmove d8f6");
	}

	@Override
	public void setThinkingListener(ThinkingListener listener) {
		this.listener = listener;
		
	}

	
}
