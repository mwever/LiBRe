package finalcbr.topt;

public interface IMatrixEvaluator<P> {

	public double eval(int[][] gt, P pred);

	public String getName();

}
