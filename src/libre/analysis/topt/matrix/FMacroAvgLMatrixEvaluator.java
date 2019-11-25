package libre.analysis.topt.matrix;

import libre.analysis.topt.IIntMatrixEvaluator;
import meka.core.Metrics;

public class FMacroAvgLMatrixEvaluator implements IIntMatrixEvaluator {

	@Override
	public double eval(final int[][] gt, final int[][] pred) {
		return Metrics.P_FmacroAvgL(gt, pred);
	}

	@Override
	public String getName() {
		return "P_FMacroL";
	}
}
