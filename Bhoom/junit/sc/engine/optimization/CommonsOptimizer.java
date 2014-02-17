package sc.engine.optimization;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.InitialGuess;

public class CommonsOptimizer implements Optimizer {

	
	@Override
	public double[] optimize(final EObjectiveFunction problem) {
		int n = problem.getEngineParameters().getNumParameters();
		BOBYQAOptimizer bopt = new BOBYQAOptimizer(n+2);
		MultivariateFunction mf = new MultivariateFunction() {
			
			
			@Override
			public double value(double[] point) {
				return problem.evaluateObjectiveFunction(point);
			}
		};
		double[] startPoint = new double[n];
		double[] lB  = new double[n];
		double[] uB  = new double[n];
		double[][] bounds = problem.getEngineParameters().getParameterBounds();
		for (int i = 0; i < n; i ++) {
			lB[i] = bounds[i][0];
			uB[i] = bounds[i][1];
			startPoint[i] = lB[i]; // (lB[i] + uB[i])/2;
		}
		ObjectiveFunction of = new ObjectiveFunction(mf);
		GoalType gtype = GoalType.MAXIMIZE;
		InitialGuess ig = new InitialGuess(startPoint);
		SimpleBounds sb = new SimpleBounds(lB, uB);
		MaxEval maxEval = new MaxEval(2000);
		MaxIter maxIter = new MaxIter(2000);
		
		PointValuePair pvp = bopt.optimize((OptimizationData) of, (OptimizationData)gtype, 
				(OptimizationData)ig, (OptimizationData)sb, maxEval, maxIter);
		 
		 return pvp.getPoint();
		
		
	}

}
