package libre.analysis.br;

public interface IBRModel {

	public int[][] getGroundTruth();

	public int[][] getThresholdedPredictions();

	public double[][] getPredictions();

	public double[] getScores();

	public double[] getThresholds();

	public int getNumLabels();

}
