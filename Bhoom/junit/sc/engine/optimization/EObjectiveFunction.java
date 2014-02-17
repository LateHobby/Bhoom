package sc.engine.optimization;

import sc.engine.engines.EngineParameters;


public interface EObjectiveFunction {

	public EngineParameters getEngineParameters();
	public double evaluateObjectiveFunction(double[] x);
	
}
