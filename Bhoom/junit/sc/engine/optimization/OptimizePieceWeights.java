package sc.engine.optimization;

import java.io.IOException;

import sc.engine.EvalTT;
import sc.engine.Evaluator;
import sc.engine.MoveSorter;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.engines.CTestEngine;
import sc.engine.engines.EngineParameters;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.util.ExternalUCIEngine;

public class OptimizePieceWeights  {

	
	
	static class PWOpt extends CTestEngine {

		public PWOpt(String name, SearchMode mode, Evaluator eval,
				EvalTT ttable, MoveSorter sorter) {
			super(name, mode, eval, ttable, sorter);
		}

		@Override
		public EngineParameters getEngineParameters() {
			EngineParameters ep = new EngineParameters();
			ep.addParameter("King", 900, 1000);
			ep.addParameter("Queen", 0, 900);
			ep.addParameter("Rook", 0, 900);
			ep.addParameter("Bishop", 0, 900);
			ep.addParameter("Knight", 0, 900);
			ep.addParameter("Pawn", 0, 900);
			return ep;
		}

		@Override
		public void setParameters(double[] values) {
			for (int i = 1; i < Evaluator.STATIC_PIECE_WEIGHTS.length; i++) {
				int valuesIndex = (i-1 < values.length) ? i-1 : i-1 - values.length;
				Evaluator.STATIC_PIECE_WEIGHTS[i] = (int) values[valuesIndex];
				
			}
		}
		
		
	}
	
	
	public static void main(String[] args) throws IOException {
		PWOpt engine = new PWOpt("Opt", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
		ExternalUCIEngine uci = new ExternalUCIEngine("C:\\Program Files (x86)\\BabasChess\\Engines\\toga\\togaII.exe");
		uci.startup();
		uci.send("setoption name MultiPV value 1");
		uci.send("setoption name Ponder value false");
		int depth = 5;
		String suiteFilename = "junit/sc/engine/engines/resources/Test100.epd";
		SuiteObjectiveFunction sof = new SuiteObjectiveFunction(engine, uci, depth, suiteFilename);
//		CommonsOptimizer opt = new CommonsOptimizer();
		AnnealingOptimizer opt = new AnnealingOptimizer();
		double[] result = opt.optimize(sof);
		String[] names = engine.getEngineParameters().getParameterNames();
		for (int i = 0; i < result.length; i++) {
			System.out.printf("%s  = %d\n", names[i], (int) result[i]);
		}
		System.exit(0);
	}

}
