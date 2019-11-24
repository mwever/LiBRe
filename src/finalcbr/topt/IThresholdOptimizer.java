package finalcbr.topt;

import java.util.List;

public interface IThresholdOptimizer {

	public double[] getThresholds(List<double[][]> expected, List<double[][]> actual);

	public double[] getScores();

	public void setSingleLabel(boolean singleLabel);

}
