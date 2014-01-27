package sc.evaluators;

import sc.engine.Evaluator;

public abstract class AbstractEvaluator implements Evaluator {

	protected int weight = 1;
	
	
	

	@Override
	public void setWeight(int weight) {
		this.weight = weight;
		
	}

	@Override
	public int getWeight() {
		return weight;
	}
}
