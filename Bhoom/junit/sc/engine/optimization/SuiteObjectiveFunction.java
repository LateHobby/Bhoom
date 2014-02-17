package sc.engine.optimization;

import java.io.IOException;
import java.util.List;

import sc.engine.EngineBoard;
import sc.engine.EngineListener;
import sc.engine.EngineStats;
import sc.engine.Evaluator;
import sc.engine.ThinkingListener;
import sc.engine.engines.CTestEngine;
import sc.engine.engines.ComparisonTestBase;
import sc.engine.engines.ComparisonTestBase.MultiStats;
import sc.engine.engines.EngineParameters;
import sc.util.EPDTestingUtils;
import sc.util.EPDTestingUtils.EPDTest;
import sc.util.ExternalUCIEngine;

public class SuiteObjectiveFunction  implements EObjectiveFunction {

	CTestEngine engine;
	ExternalUCIEngine uci;
	String suiteFilename;
	List<EPDTest> tests;
	ComparisonTestBase testBase = new ComparisonTestBase();
	private int depth;
	
	public SuiteObjectiveFunction(CTestEngine engine, ExternalUCIEngine uci, int depth, String suiteFilename) {
		this.engine = engine;
		this.uci = uci;
		this.depth = depth;
		this.suiteFilename = suiteFilename;
		try {
			tests = EPDTestingUtils.getTestsFromFile(suiteFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	
	
	@Override
	public double evaluateObjectiveFunction(double[] x) {
		engine.newGame();
		engine.setParameters(x);
		try {
			MultiStats stats = testBase.getResultStats("ObjectiveFunc", engine, uci, depth, suiteFilename);
			return stats.stats("externalEval").mean();
		} catch (IllegalAccessException | IOException e) {
			throw new RuntimeException(e);
		}
	}



	@Override
	public EngineParameters getEngineParameters() {
		return engine.getEngineParameters();
	}



	
}
