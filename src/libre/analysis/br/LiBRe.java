package libre.analysis.br;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import libre.GlobalConfig;
import libre.analysis.topt.IMatrixEvaluator;
import libre.analysis.topt.IThresholdOptimizer;
import libre.util.ArrayUtil;

public class LiBRe implements IBRModel {

	private List<TestArffBR> availableModels;

	private TestArffBR[] labelWiseBaselearners;

	private double[] thresholds;
	private double[] scores;

	public LiBRe(final String dataset, final String evalMode, final int testseed, final int testfold, final int... valseeds) throws FileNotFoundException, IOException {
		this.availableModels = new LinkedList<>();
		for (File baselearner : new File(GlobalConfig.RESULT_DIR, dataset).listFiles()) {
			System.out.println("Load baselearner data for " + baselearner.getName());
			try {
				this.availableModels.add(new TestArffBR(dataset, baselearner.getName(), evalMode, testseed, testfold, valseeds));
			} catch (Exception e) {
				System.err.println("Could not load " + baselearner.getName());
			}
		}
	}

	public List<TestArffBR> getAvailableModels() {
		return this.availableModels;
	}

	public void customizeForTest(final IThresholdOptimizer opt) {
		this.availableModels.stream().forEach(x -> x.optimizeTestThresholds(opt));
		this.pickBLModels();
	}

	public void customizeForVal(final IThresholdOptimizer opt) {
		this.availableModels.stream().forEach(x -> x.optimizeValThresholds(opt));
		this.pickBLModels();
	}

	public void customizeForTest(final IMatrixEvaluator matVal) {
		final Set<Double> possibleTs = new HashSet<>();
		IntStream.range(0, (int) ((1E2) + 1)).mapToDouble(x -> x * 1E-2).forEach(possibleTs::add);
		this.labelWiseBaselearners = new TestArffBR[this.getNumLabels()];
		IntStream.range(0, this.labelWiseBaselearners.length).forEach(x -> this.labelWiseBaselearners[x] = this.availableModels.get(0));
		double[][] currentMatrix = this.availableModels.get(0).getPredictions();
		Double bestScore = null;

		ArrayUtil.copyArrayRetaining(currentMatrix, IntStream.range(0, currentMatrix.length).mapToObj(x -> Integer.valueOf(x)).collect(Collectors.toList()));

		for (int l = 0; l < this.getNumLabels(); l++) {
			for (TestArffBR test : this.availableModels) {
				for (Double t : possibleTs) {

				}
			}
		}
	}

	private void pickBLModels() {
		this.labelWiseBaselearners = new TestArffBR[this.getNumLabels()];
		for (int l = 0; l < this.labelWiseBaselearners.length; l++) {
			for (TestArffBR test : this.availableModels) {
				if (this.labelWiseBaselearners[l] == null || this.labelWiseBaselearners[l].getScores()[l] < test.getScores()[l]) {
					this.labelWiseBaselearners[l] = test;
				}
			}
		}
		this.scores = IntStream.range(0, this.labelWiseBaselearners.length).mapToDouble(x -> this.labelWiseBaselearners[x].getScores()[x]).toArray();
		this.thresholds = IntStream.range(0, this.labelWiseBaselearners.length).mapToDouble(x -> this.labelWiseBaselearners[x].getThresholds()[x]).toArray();
	}

	@Override
	public int getNumLabels() {
		return this.availableModels.get(0).getNumLabels();
	}

	@Override
	public double[][] getPredictions() {
		double[][] predictions = new double[this.labelWiseBaselearners.length][];
		for (int l = 0; l < this.labelWiseBaselearners.length; l++) {
			predictions[l] = ArrayUtil.extractColumn(this.labelWiseBaselearners[l].getPredictions(), l);
		}
		return ArrayUtil.transposeMatrix(predictions);
	}

	@Override
	public int[][] getThresholdedPredictions() {
		int[][] predictions = new int[this.labelWiseBaselearners.length][];
		for (int l = 0; l < this.labelWiseBaselearners.length; l++) {
			predictions[l] = ArrayUtil.thresholdDoubleToBinaryArray(ArrayUtil.extractColumn(this.labelWiseBaselearners[l].getPredictions(), l), this.thresholds[l]);
		}
		return ArrayUtil.transposeMatrix(predictions);
	}

	@Override
	public int[][] getGroundTruth() {
		int[][] gt = new int[this.labelWiseBaselearners.length][];
		for (int l = 0; l < this.labelWiseBaselearners.length; l++) {
			gt[l] = ArrayUtil.extractColumn(this.labelWiseBaselearners[l].getGroundTruth(), l);
		}
		return ArrayUtil.transposeMatrix(gt);
	}

	@Override
	public double[] getScores() {
		return this.scores;
	}

	@Override
	public double[] getThresholds() {
		return this.thresholds;
	}

	public String[] getBaselearners() {
		return Arrays.stream(this.labelWiseBaselearners).map(x -> x.getLearnerName()).collect(Collectors.toList()).toArray(new String[] {});
	}

}
