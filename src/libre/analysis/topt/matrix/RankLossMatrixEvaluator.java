package libre.analysis.topt.matrix;

import libre.analysis.topt.IDoubleMatrixEvaluator;
import meka.core.Metrics;

public class RankLossMatrixEvaluator implements IDoubleMatrixEvaluator {

	@Override
	public double eval(final int[][] gt, final double[][] pred) {
		return Metrics.L_RankLoss(gt, pred);
	}

	@Override
	public String getName() {
		return "L_RankLoss";
	}

}