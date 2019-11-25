package libre.analysis.topt.vector;

import libre.analysis.topt.IVectorEvaluator;
import meka.core.Metrics;

public class HarmonicAccuracyVecEvaluator implements IVectorEvaluator {

	@Override
	public double eval(final int[] gt, final int[] pred) {
		return Metrics.P_Harmonic(gt, pred);
	}

	@Override
	public String getName() {
		return "HarmonicAccuracy";
	}

}
