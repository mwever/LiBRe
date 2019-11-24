package finalcbr.br;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import finalcbr.topt.IThresholdOptimizer;
import finalcbr.util.ArrayUtil;

public class TestArffBR implements IBRModel {

	private List<ArffBR> validations;
	private ArffBR test;

	private String baselearner;
	public double[] thresholds;
	public double[] evalValues;

	public TestArffBR(final String dataset, final String baselearner, final String evalMode, final int testseed, final int testfold, final int... valseeds) throws FileNotFoundException, IOException {
		this.baselearner = baselearner;
		this.test = new ArffBR(dataset, baselearner, evalMode, testseed, testfold, 0, -1);
		this.validations = new LinkedList<>();
		for (int valseed : valseeds) {
			for (int valfold : new int[] { 0, 1 }) {
				this.validations.add(new ArffBR(dataset, baselearner, evalMode, testseed, testfold, valseed, valfold));
			}
		}
		this.thresholds = new double[this.test.getNumLabels()];
	}

	public String getLearnerName() {
		return this.baselearner;
	}

	@Override
	public int getNumLabels() {
		return this.test.getNumLabels();
	}

	public List<ArffBR> getValidations() {
		return this.validations;
	}

	public ArffBR getTest() {
		return this.test;
	}

	@Override
	public double[] getScores() {
		return this.evalValues;
	}

	@Override
	public double[] getThresholds() {
		return this.thresholds;
	}

	@Override
	public int[][] getThresholdedPredictions() {
		int[][] thresholdedPredictions = new int[this.test.getNumLabels()][];
		for (int l = 0; l < this.test.getNumLabels(); l++) {
			thresholdedPredictions[l] = ArrayUtil.thresholdDoubleToBinaryArray(ArrayUtil.extractColumn(this.test.getPredictions(), l), this.thresholds[l]);
		}
		return ArrayUtil.transposeMatrix(thresholdedPredictions);
	}

	@Override
	public double[][] getPredictions() {
		return this.test.getPredictions();
	}

	@Override
	public int[][] getGroundTruth() {
		return ArrayUtil.thresholdDoubleToBinaryMatrix(this.test.getGroundTruth(), 0.5);
	}

	public void optimizeValThresholds(final IThresholdOptimizer opt) {
		this.thresholds = opt.getThresholds(this.validations.stream().map(x -> x.getGroundTruth()).collect(Collectors.toList()), this.validations.stream().map(x -> x.getPredictions()).collect(Collectors.toList()));
		this.evalValues = opt.getScores();
	}

	public void optimizeTestThresholds(final IThresholdOptimizer opt) {
		List<double[][]> expected = new LinkedList<>();
		List<double[][]> actual = new LinkedList<>();
		expected.add(this.test.getGroundTruth());
		actual.add(this.test.getPredictions());
		this.thresholds = opt.getThresholds(expected, actual);
		this.evalValues = opt.getScores();
	}

	public void setThreshold(final double t) {
		IntStream.range(0, this.thresholds.length).forEach(x -> this.thresholds[x] = t);
	}

	public void setThreshold(final double[] t) {
		this.thresholds = t;
	}

	public void setThreshold(final int index, final double t) {
		this.thresholds[index] = t;
	}

}
