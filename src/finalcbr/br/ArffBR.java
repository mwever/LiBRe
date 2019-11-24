package finalcbr.br;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import ai.libs.jaicore.basic.sets.SetUtil;
import finalcbr.GlobalConfig;
import finalcbr.util.ArrayUtil;
import weka.core.Instances;

public class ArffBR {

	private final int numLabels;
	private final double[][] groundTruth;
	private final double[][] predictions;

	public ArffBR(final String dataset, final String baselearner, final String evalMode, final int testSeed, final int testFold, final int valSeed, final int valFold) throws FileNotFoundException, IOException {
		File resultsFile = new File(new File(new File(GlobalConfig.RESULT_DIR, dataset), baselearner), SetUtil.implode(Arrays.asList(evalMode, testSeed + "", testFold + "", valSeed + "", valFold + ""), "_") + ".arff");
		Instances predData = new Instances(new FileReader(resultsFile));

		this.numLabels = (int) (predData.numAttributes() / 2.0);
		this.groundTruth = new double[predData.size()][this.numLabels];
		this.predictions = new double[predData.size()][this.numLabels];

		for (int i = 0; i < predData.size(); i++) {
			for (int j = 0; j < predData.numAttributes(); j++) {
				double value = predData.get(i).value(j);
				int arrayIndex = j % this.numLabels;
				if (j < this.numLabels) {
					this.groundTruth[i][arrayIndex] = value;
				} else {
					this.predictions[i][arrayIndex] = value;
				}
			}
		}
	}

	public ArffBR(final int numLabels, final double[][] groundTruth, final double[][] predictions) {
		this.numLabels = numLabels;
		this.groundTruth = groundTruth;
		this.predictions = predictions;
	}

	public double[][] getGroundTruth() {
		return this.groundTruth;
	}

	public double[][] getPredictions() {
		return this.predictions;
	}

	public double[] getLabelPredictions(final int label) {
		return ArrayUtil.extractColumn(this.getPredictions(), label);
	}

	public double[] getLabelGroundTruth(final int label) {
		return ArrayUtil.extractColumn(this.getGroundTruth(), label);
	}

	public int getNumLabels() {
		return this.numLabels;
	}

}
