package finalcbr.topt.vector;

import finalcbr.topt.IVectorEvaluator;
import meka.core.Metrics;

public class AvgPrecisionVecEvaluator implements IVectorEvaluator {

	@Override
	public double eval(final int[] gt, final int[] pred) {
		return Metrics.P_AveragePrecision(gt, pred);
	}

	@Override
	public String getName() {
		return "AvgPrecision";
	}
}
