package libre.analysis.topt;

import java.util.LinkedList;
import java.util.List;

public class CompleteBatchMacroThresholdOptimizer implements IThresholdOptimizer {

	private IVectorEvaluator eval;
	private IThresholdOptimizer reducedOpt;
	private boolean singleLabel = false;

	public CompleteBatchMacroThresholdOptimizer(final IVectorEvaluator eval) {
		this.eval = eval;
		this.reducedOpt = new BatchWiseMacroThresholdOptimizer(this.eval);
	}

	@Override
	public double[] getThresholds(final List<double[][]> expected, final List<double[][]> actual) {
		double[][] expectedM = expected.stream().reduce(CompleteBatchMacroThresholdOptimizer::concatMatrices).get();
		double[][] actualM = actual.stream().reduce(CompleteBatchMacroThresholdOptimizer::concatMatrices).get();
		List<double[][]> expectedML = new LinkedList<>();
		List<double[][]> actualML = new LinkedList<>();
		expectedML.add(expectedM);
		actualML.add(actualM);
		this.reducedOpt.setSingleLabel(this.singleLabel);
		return this.reducedOpt.getThresholds(expectedML, actualML);
	}

	@Override
	public double[] getScores() {
		return this.reducedOpt.getScores();
	}

	public static double[][] concatMatrices(final double[][] arg0, final double[][] arg1) {
		double[][] merged = new double[arg0.length + arg1.length][];
		System.arraycopy(arg0, 0, merged, 0, arg0.length);
		System.arraycopy(arg1, 0, merged, arg0.length, arg1.length);
		return merged;
	}

	@Override
	public void setSingleLabel(final boolean singleLabel) {
		this.singleLabel = singleLabel;
	}
}
