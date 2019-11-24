package finalcbr.topt.matrix;

import finalcbr.topt.IIntMatrixEvaluator;
import meka.core.Metrics;

public class JaccardMatrixEvaluator implements IIntMatrixEvaluator {

	@Override
	public double eval(final int[][] gt, final int[][] pred) {
		return Metrics.P_JaccardIndex(gt, pred);
	}

	@Override
	public String getName() {
		return "P_JaccardIndex";
	}

}
