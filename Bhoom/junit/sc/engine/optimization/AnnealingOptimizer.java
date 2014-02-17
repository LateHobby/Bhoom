package sc.engine.optimization;

import java.util.Arrays;
import java.util.Random;

import sc.engine.engines.EngineParameters;

public class AnnealingOptimizer implements Optimizer {

	private double INITIAL_TEMPERATURE = 100;
	private double FINAL_TEMPERATURE = 1;
	private double COOLING_RATE = 0.05;
	private double[] STEP_SIZE_FACTORS = new double[]{10, 20, 40, 80, 160};
	
	Random r = new Random(0);
	
	@Override
	public double[] optimize(EObjectiveFunction problem) {
		double[] startPoint = chooseStartPoint(problem.getEngineParameters());
		for (double stepSizeFactor : STEP_SIZE_FACTORS) {
			startPoint = optimize(problem, startPoint, stepSizeFactor);
		}
		return startPoint;
	}
	
	public double[] optimize(EObjectiveFunction problem, double[] start, double stepSizeFactor) {
		double bestEval;
		double[] bestPoint;
		double[][] bounds = problem.getEngineParameters().getParameterBounds();
		
		 
		double[] currentPoint = start;
		bestPoint = start;
		double currentEval = problem.evaluateObjectiveFunction(start);
		bestEval = currentEval;
		double temperature = INITIAL_TEMPERATURE;
		do {
			double[] nextPoint = getNextPoint(currentPoint, bounds, stepSizeFactor);
			
			System.out.println("---------------------------------------------------");
			System.out.println("Trying: " + Arrays.toString(nextPoint));
			double nextEval = problem.evaluateObjectiveFunction(nextPoint);
			System.out.println("Eval: " + nextEval);
			if (nextEval > bestEval) {
				bestEval = nextEval;
				bestPoint = nextPoint;
			}
			if (nextEval > currentEval || acceptLowerEval(nextEval, currentEval, temperature)) {
				currentPoint = nextPoint;
				currentEval = nextEval;
			}
			temperature = reduceTemperature(temperature);
		}
		while (temperature > FINAL_TEMPERATURE);
		return bestPoint;
			
	}

	private double[] getNextPoint(double[] currentPoint, double[][] bounds,
			double stepSizeFactor) {
		double[] point = new double[currentPoint.length];
		for (int i = 0; i < point.length; i++) {
			double stepSize = (bounds[i][1] - bounds[i][0])/stepSizeFactor;
			double step = Math.random() > 1/2 ? stepSize : - stepSize;
			double newVal = currentPoint[i] + step;
			if (newVal > bounds[i][1]) {
				newVal = bounds[i][1];
			}
			if (newVal < bounds[i][0]) {
				newVal = bounds[i][0];
			}
			point[i] = newVal;
		}
		return point;
	}

//	private double[] getNextPoint(double[] currentPoint, double[][] bounds,
//			double temperature) {
//		double[] point = new double[currentPoint.length];
//		for (int i = 0; i < point.length; i++) {
//			int range = (int) (bounds[i][1] - bounds[i][0]);
//			int maxChange = (int) (range/2.0 * (temperature/100) );
//			if (maxChange < 1) {
//				maxChange = 1;
//			}
//			int upper = (int) Math.min(bounds[i][1], currentPoint[i] + maxChange);
//			int lower = (int) Math.ceil(Math.max(bounds[i][0], currentPoint[i] - maxChange));
//			int rval = r.nextInt(upper - lower);
//			point[i] = lower + rval;
//		}
//		return point;
//	}

	private double[] getRanges(double[][] parameterBounds) {
		double[] ranges = new double[parameterBounds.length];
		for (int i = 0; i < ranges.length; i++) {
			ranges[i] = parameterBounds[i][1] - parameterBounds[i][0];
		}
		return ranges;
	}

	private double[] chooseStartPoint(EngineParameters engineParameters) {
		int n = engineParameters.getNumParameters();
		double[] point = new double[n];
		double[][] bounds = engineParameters.getParameterBounds();
		for (int i = 0; i < n; i++) {
			point[i] = bounds[i][0];
		}
		return point;
	}

	private boolean acceptLowerEval(double nextEval, double currentEval,
			double temperature) {
		double acceptanceProbability = Math.exp((nextEval - currentEval)/temperature);
		return acceptanceProbability > Math.random();
		
	}

	private double reduceTemperature(double temperature) {
		return temperature * (1 - COOLING_RATE);
	}

}
