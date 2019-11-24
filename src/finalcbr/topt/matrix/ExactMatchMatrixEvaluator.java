package finalcbr.topt.matrix;

import finalcbr.topt.IIntMatrixEvaluator;
import meka.core.Metrics;

public class ExactMatchMatrixEvaluator implements IIntMatrixEvaluator {

	@Override
	public double eval(final int[][] gt, final int[][] pred) {
		return Metrics.P_ExactMatch(gt, pred);
	}

	@Override
	public String getName() {
		return "P_ExactMatch";
	}
}
