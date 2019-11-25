package libre.analysis.topt.matrix;

import libre.analysis.topt.IIntMatrixEvaluator;
import meka.core.Metrics;

public class HammingMatrixEvaluator implements IIntMatrixEvaluator {

	@Override
	public double eval(final int[][] gt, final int[][] pred) {
		return Metrics.L_Hamming(gt, pred);
	}

	@Override
	public String getName() {
		return "L_Hamming";
	}

}
