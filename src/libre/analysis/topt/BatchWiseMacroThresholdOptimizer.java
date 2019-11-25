package libre.analysis.topt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import libre.util.ArrayUtil;

public class BatchWiseMacroThresholdOptimizer implements IThresholdOptimizer {

	private IVectorEvaluator eval;

	private double[] thresholds;
	private double[] scores;
	private boolean singleLabel;

	public BatchWiseMacroThresholdOptimizer(final IVectorEvaluator eval) {
		this.eval = eval;
	}

	@Override
	public double[] getThresholds(final List<double[][]> expected, final List<double[][]> actual) {
		Set<Double> possibleTs = new HashSet<>();
		IntStream.range(0, (int) ((1E2) + 1)).mapToDouble(x -> x * 1E-2).forEach(possibleTs::add);

		int L = expected.get(0)[0].length;
		this.thresholds = new double[L];
		this.scores = new double[L];

		if (this.singleLabel) {
			List<int[][]> expecteds = expected.stream().map(x -> ArrayUtil.thresholdDoubleToBinaryMatrix(x, 0.5)).collect(Collectors.toList());
			double[] bestThresholds = null;
			double[] scores = null;
			for (double t : possibleTs) {
				List<int[][]> preds = actual.stream().map(x -> ArrayUtil.thresholdDoubleToBinaryMatrix(x, t)).collect(Collectors.toList());
				double[] curScores = IntStream.range(0, L)
						.mapToDouble(l -> IntStream.range(0, preds.size()).mapToDouble(x -> this.eval.eval(ArrayUtil.extractColumn(expecteds.get(x), l), ArrayUtil.extractColumn(preds.get(x), l))).average().getAsDouble()).toArray();
				if (bestThresholds == null || Arrays.stream(curScores).average().getAsDouble() > Arrays.stream(scores).average().getAsDouble()) {
					bestThresholds = IntStream.range(0, L).mapToDouble(x -> t).toArray();
					scores = curScores;
				}
			}

			this.thresholds = bestThresholds;
			this.scores = scores;
		} else {
			for (int l = 0; l < L; l++) {
				int currentLabel = l;

				List<int[]> expectedLabel = expected.stream().map(x -> ArrayUtil.thresholdDoubleToBinaryArray(ArrayUtil.extractColumn(x, currentLabel), 0.5)).collect(Collectors.toList());
				List<double[]> actualLabel = actual.stream().map(x -> ArrayUtil.extractColumn(x, currentLabel)).collect(Collectors.toList());

				Double bestEval = null;
				Double bestT = null;
				for (double t : possibleTs) {
					double curEval = IntStream.range(0, expectedLabel.size()).mapToDouble(x -> this.eval.eval(expectedLabel.get(x), ArrayUtil.thresholdDoubleToBinaryArray(actualLabel.get(x), t))).average().getAsDouble();
					if (bestEval == null || curEval > bestEval) {
						bestEval = curEval;
						bestT = t;
					}
				}
				this.thresholds[l] = bestT;
				this.scores[l] = bestEval;
			}
		}

		return this.thresholds;
	}

	public double[] getThresholds() {
		return this.thresholds;
	}

	@Override
	public double[] getScores() {
		return this.scores;
	}

	@Override
	public void setSingleLabel(final boolean singleLabel) {
		this.singleLabel = singleLabel;
	}

	public IVectorEvaluator getEval() {
		return this.eval;
	}

}
