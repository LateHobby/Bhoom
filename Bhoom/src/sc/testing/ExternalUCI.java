package sc.testing;

import java.io.IOException;

import sc.engine.EngineBoard;

public interface ExternalUCI {

		int getEval();

		void startup() throws IOException;

		int getMove();

		void evaluatePosition(EngineBoard board, int depth, int timeMs);

		void getBestMove(EngineBoard board, int depth, int timeMs);
		
	}