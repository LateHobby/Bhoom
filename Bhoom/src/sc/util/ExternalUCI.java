package sc.util;

import java.io.IOException;

import sc.engine.EngineBoard;
import sc.engine.ThinkingListener;

public interface ExternalUCI {

		int getEval();

		void startup() throws IOException;

		void shutDown();
		
		int getMove();

		void evaluatePosition(EngineBoard board, int depth, int timeMs);

		void getBestMove(EngineBoard board, int depth, int timeMs);
		
		void setThinkingListener(ThinkingListener listener);

		void evaluateMove(EngineBoard board, int move, int depth, int timeMs);
		
		void send(String command) throws IOException;
	}