package sc.testing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import sc.bboard.EBitBoard;
import sc.engine.EngineBoard;
import sc.testing.StreamGobbler.OnLineListener;
import sc.util.BoardUtils;

public class ExternalUCIEngine implements ExternalUCI, OnLineListener {

	boolean debug = true;
	
	private boolean processStarted;
	private String[] cmd;
	private StreamGobbler gobbler;
	
	int move;
	int eval;
	private Process proc;
	private BufferedWriter bos;
	private Object synchronizer = new Object();
	private String lastString;
	private String waitString;
	
	
	EBitBoard localBoard = new EBitBoard();
	
	public ExternalUCIEngine(String... cmd) {
		this.cmd = cmd;
	}


	@Override
	public void startup() throws IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream();
		proc = pb.start();
		gobbler = new StreamGobbler("UCI", proc.getInputStream(), this);
		gobbler.start();
		bos = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
		processStarted = true;
		send("uci");
		waitfor("uciok");
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
	public void evaluatePosition(EngineBoard board,  int depth, int timeMs) {
		try {
			send("ucinewgame");
			send("isready");
			waitfor("readyok");
			String fen = BoardUtils.getFen(board);
			BoardUtils.initializeBoard(localBoard, fen);
			send("position " + fen + " moves");
			send(buildGoString(depth, timeMs));
			waitfor("bestmove");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	


	@Override
	public void getBestMove(EngineBoard board,  int depth, int timeMs) {
		evaluatePosition(board, depth, timeMs);
		
	}
	
	private String buildGoString(int depth, int timeMs) {
		return String.format("go depth %d movetime %d", depth, timeMs);
	}
	
	private void waitfor(String string) {
		waitString = string;
		try {
			synchronized (synchronizer) {
				synchronizer.wait(2000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (string != null && !string.equals(lastString)) {
			throw new RuntimeException("Expected readyok but got: " + lastString);
		}
	}


	private void send(String str) throws IOException {
		bos.write(str);
		bos.newLine();
		bos.flush();
		if (debug) {
			System.out.println("Sending: " + str);
		}
	}


	@Override
	public void onLine(String line) {
		if (debug) {
			System.out.println("Received:" + line);
		}
		boolean notify = false;
		if (waitString != null && line.startsWith(waitString) ) {
			notify = true;
		}
		lastString = line;
		if (line.startsWith("bestMove")) {
			String[] sa = line.split("\\s+");
			move = BoardUtils.encodedForm(localBoard, sa[1]);
		}
		if (line.startsWith("info")) {
			String[] sa = line.split("\\s+");
			for (int i = 0; i < sa.length; i++) {
				if (sa[i].equals("cp")) {
					eval = Integer.parseInt(sa[i+1]);
				}
			}
		}
		if (notify) {
			waitString = null;
			synchronizer.notifyAll();
		}
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		ExternalUCIEngine uci = new ExternalUCIEngine("lib\\Flux-2.2.1\\Flux.bat");
		uci.startup();
		uci.send("uci");
		Thread.sleep(5000);
	}
}
