package libre.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import libre.GlobalConfig;
import libre.analysis.br.LiBRe;
import libre.analysis.br.TestArffBR;
import libre.analysis.topt.CompleteBatchMacroThresholdOptimizer;
import libre.analysis.topt.IDoubleMatrixEvaluator;
import libre.analysis.topt.IIntMatrixEvaluator;
import libre.analysis.topt.IMatrixEvaluator;
import libre.analysis.topt.IThresholdOptimizer;
import libre.analysis.topt.IVectorEvaluator;
import libre.analysis.topt.matrix.FMacroAvgDMatrixEvaluator;
import libre.analysis.topt.matrix.FMacroAvgLMatrixEvaluator;
import libre.analysis.topt.matrix.FMicroMatrixEvaluator;
import libre.analysis.topt.matrix.HammingMatrixEvaluator;
import libre.analysis.topt.matrix.JaccardMatrixEvaluator;
import libre.analysis.topt.matrix.RankLossMatrixEvaluator;
import libre.analysis.topt.vector.AccuracyVecEvaluator;
import libre.analysis.topt.vector.AvgPrecisionVecEvaluator;
import libre.analysis.topt.vector.F1VecEvaluator;
import libre.analysis.topt.vector.HarmonicAccuracyVecEvaluator;
import meka.core.Metrics;

/**
 * Controller to conduct the whole optimization task including selection of base learners as well as optimizing the thresholds.
 *
 * @author mwever
 */
public class LiBReAnalyzer {
	private static final File OUTPUT_DIR = new File("brexp/kvstore/");
	private static final String EVAL_MODE = "2cv";

	static IVectorEvaluator evalOptF1 = new F1VecEvaluator();
	static IVectorEvaluator evalOptAcc = new AccuracyVecEvaluator();
	static IVectorEvaluator evalOptAvgPrecision = new AvgPrecisionVecEvaluator();
	static IVectorEvaluator evalOptHarmonicAcc = new HarmonicAccuracyVecEvaluator();

	static IIntMatrixEvaluator finalFMacroL = new FMacroAvgLMatrixEvaluator();
	static IIntMatrixEvaluator finalHamming = new HammingMatrixEvaluator();
	static IIntMatrixEvaluator finalJaccard = new JaccardMatrixEvaluator();
	static IIntMatrixEvaluator finalFMacroD = new FMacroAvgDMatrixEvaluator();
	static IIntMatrixEvaluator finalFMicro = new FMicroMatrixEvaluator();
	static IDoubleMatrixEvaluator finalRank = new RankLossMatrixEvaluator();

	static IVectorEvaluator[] evals = new IVectorEvaluator[] { evalOptF1, evalOptAcc };
	static IMatrixEvaluator[] matrixEvals = new IMatrixEvaluator[] { finalJaccard, finalFMacroD, finalFMicro, finalRank };
	@SuppressWarnings("rawtypes")
	static IMatrixEvaluator[] finalEval = new IMatrixEvaluator[] { finalFMacroL, finalHamming, finalJaccard, finalFMacroD, finalFMicro, finalRank };
	static int evalIx = 1;

	static int[] testSeeds = { 0, 1, 2, 3, 4 };
	static int[] testFolds = { 0, 1 };
	static int valSeed = 0;

	public static void main(final String[] args) throws IOException {
		String evalMode = EVAL_MODE;

		for (int testSeed : testSeeds) {
			for (int testFold : testFolds) {
				for (File datasetFile : GlobalConfig.RESULT_DIR.listFiles()) {
					if (!datasetFile.isDirectory()) {
						continue;
					}
					System.out.println("Consider dataset " + datasetFile.getName());
					String datasetName = datasetFile.getName();

					if (!datasetName.contains("tmc") && !datasetName.contains("bookmarks")) {
						System.out.println("Skip");
						continue;
					}

					LiBRe libre = null;

					for (IVectorEvaluator valOpt : evals) {
						File outputFile = new File(OUTPUT_DIR, Arrays.stream(new String[] { datasetFile.getName(), evalMode, testSeed + "", testFold + "", valSeed + "", valOpt.getName() }).collect(Collectors.joining("-")) + ".kvstore");
						if (outputFile.exists()) {
							continue;
						}

						if (libre == null) {
							libre = new LiBRe(datasetName, "2cv", testSeed, testFold, valSeed);
						}

						KVStoreCollection col = new KVStoreCollection();
						for (int i = 0; i < evals.length; i++) {
							if (valOpt == evals[i]) {
								evalIx = i;
							}
						}
						System.out.println("Optimize " + valOpt.getName());
						IThresholdOptimizer globalOpt = new CompleteBatchMacroThresholdOptimizer(valOpt);

						System.out.println("Validation based label-wise threshold");
						libre.customizeForVal(globalOpt);
						addToCol(libre, col, valOpt, "val", datasetName, false);

						System.out.println("Optimistic label-wise threshold");
						libre.customizeForTest(globalOpt);
						addToCol(libre, col, valOpt, "optimistic", datasetName, false);

						globalOpt.setSingleLabel(true);
						System.out.println("Validation based single label threshold");
						libre.customizeForVal(globalOpt);
						addToCol(libre, col, valOpt, "val", datasetName, true);

						System.out.println("Optimistic single label threshold");
						libre.customizeForTest(globalOpt);
						addToCol(libre, col, valOpt, "optimistic", datasetName, true);
						outputFile.getParentFile().mkdirs();
						col.serializeTo(outputFile);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void addToCol(final LiBRe cbr, final KVStoreCollection col, final IVectorEvaluator valOpt, final String type, final String dataset, final boolean singleLabelThreshold) {
		KVStore sd = new KVStore();
		// setting description
		sd.put("dataset", dataset);
		sd.put("type", type);
		sd.put("singleLabelThreshold", singleLabelThreshold);
		sd.put("optEval", valOpt.getName());
		sd.put("values", Arrays.stream(finalEval).map(x -> x.getName()).collect(Collectors.joining(",")) + ",P_RankLoss");

		KVStore cbrStore = new KVStore(sd.toString());
		cbrStore.put("algorithm", "cbr");
		// optimization result
		cbrStore.put("avgOptEvalValue", Arrays.stream(cbr.getScores()).average().getAsDouble());
		cbrStore.put("thresholds", Arrays.stream(cbr.getThresholds()).mapToObj(x -> x + "").collect(Collectors.joining(",")));
		cbrStore.put("baselearners", Arrays.stream(cbr.getBaselearners()).collect(Collectors.joining(",")));
		cbrStore.put("baselearnerValues", Arrays.stream(cbr.getScores()).mapToObj(x -> x + "").collect(Collectors.joining(",")));
		// measured test results
		Arrays.stream(finalEval).forEach(x -> cbrStore.put(x.getName(), (x instanceof IIntMatrixEvaluator) ? x.eval(cbr.getGroundTruth(), cbr.getThresholdedPredictions()) : x.eval(cbr.getGroundTruth(), cbr.getPredictions())));
		cbrStore.put("P_RankLoss", (1 - Metrics.L_RankLoss(cbr.getGroundTruth(), cbr.getPredictions())));
		col.add(cbrStore);

		for (TestArffBR single : cbr.getAvailableModels()) {
			KVStore singleStore = new KVStore(sd.toString());
			singleStore.put("algorithm", single.getLearnerName());
			// optimization result
			singleStore.put("avgOptEvalValue", Arrays.stream(single.getScores()).average().getAsDouble());
			singleStore.put("thresholds", Arrays.stream(single.getThresholds()).mapToObj(x -> x + "").collect(Collectors.joining(",")));
			singleStore.put("baselearnerValues", Arrays.stream(single.getScores()).mapToObj(x -> x + "").collect(Collectors.joining(",")));
			// measured test results
			Arrays.stream(finalEval)
					.forEach(x -> singleStore.put(x.getName(), (x instanceof IIntMatrixEvaluator) ? x.eval(single.getGroundTruth(), single.getThresholdedPredictions()) : x.eval(single.getGroundTruth(), single.getPredictions())));
			col.add(singleStore);
		}
	}

}
