package libre.analysis.topt.vector;

import libre.analysis.topt.IVectorEvaluator;
import meka.core.Metrics;

public class AccuracyVecEvaluator implements IVectorEvaluator {

	@Override
	public double eval(final int[] gt, final int[] pred) {
		return (1 - Metrics.L_Hamming(gt, pred));
	}

	@Override
	public String getName() {
		return "Standard Accuracy";
	}

}
