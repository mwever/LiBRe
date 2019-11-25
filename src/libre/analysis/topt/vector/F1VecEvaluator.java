package libre.analysis.topt.vector;

import libre.analysis.topt.IVectorEvaluator;
import meka.core.Metrics;

public class F1VecEvaluator implements IVectorEvaluator {

	@Override
	public double eval(final int[] gt, final int[] pred) {
		return Metrics.F1(gt, pred);
	}

	@Override
	public String getName() {
		return "F1";
	}
}
