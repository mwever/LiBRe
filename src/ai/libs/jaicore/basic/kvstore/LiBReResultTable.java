package ai.libs.jaicore.basic.kvstore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection.EGroupMethod;
import ai.libs.jaicore.basic.sets.SetUtil;

/**
 * This class is used to generate the results table of the paper, identifying best performing
 * approaches, and conducting significance tests with the Wilcoxon signed rank test.
 *
 * @author mwever
 *
 */

public class LiBReResultTable {

	private static final int MAX_SEED = 5;

	private static final String SUFFIX = "Standard Accuracy.kvstore";
	private static final String TM = "L_Hamming";
	private static final boolean INVERT_FOR_TM = false;

//	private static final String SUFFIX = "F1.kvstore";
//	private static final String TM = "P_FMacroL";
//	private static final boolean INVERT_FOR_TM = true;

	public static void main(final String[] args) throws IOException {
		KVStoreCollection merged = new KVStoreCollection();
		for (File resultFile : new File("brexp/kvstore/").listFiles()) {
			if (!resultFile.getName().endsWith(SUFFIX)) {
				continue;
			}
			String[] filenameSplit = resultFile.getName().split("-");
			int index = 2;
			if (!filenameSplit[index - 1].equals("2cv")) {
				index++;
			}
			int testSeed = Integer.parseInt(filenameSplit[index++]);
			int testFold = Integer.parseInt(filenameSplit[index]);

			if (testSeed >= MAX_SEED) {
				System.err.println("MAX_SEED not sufficiently large to ensure unique IDs");
				System.exit(0);
			}
			int id = testSeed + (testFold) * MAX_SEED;

			KVStoreCollection col = new KVStoreCollection("name=dummy\n" + FileUtil.readFileAsString(resultFile));
			KVStoreCollection bls = new KVStoreCollection(col.toString());
			bls.removeAny("algorithm=cbr");
			KVStoreCollection sbs = getSBS(bls);

			Map<String, String> selection = new HashMap<>();
			selection.put("algorithm", "cbr");
			KVStoreCollection cbr = col.select(selection);

			sbs.stream().forEach(x -> x.put("evalid", id));
			cbr.stream().forEach(x -> x.put("evalid", id));

			merged.addAll(sbs);
			merged.addAll(cbr);
		}

		merged.projectRemove("P_FMicro", "P_FMacroD", "values", "P_JaccardIndex", "L_RankLoss", "baselearners", "P_RankLoss", "thresholds");

		merged.serializeTo(new File("brexp/mergedResults.kvstore"));

		Map<String, EGroupMethod> grouping = new HashMap<>();
		grouping.put("avgOptEvalValue", EGroupMethod.AVG);
		grouping.put("P_FMacroL", EGroupMethod.AVG);
		grouping.put("L_Hamming", EGroupMethod.AVG);
		grouping.put("baselearnerValues", EGroupMethod.AVG);

		merged.stream().forEach(x -> x.put("baselearnerValues", x.getAsDoubleList("baselearnerValues").stream().mapToDouble(y -> y).average().getAsDouble()));

		merged = merged.group(new String[] { "type", "algorithm", "optEval", "dataset", "singleLabelThreshold" }, grouping);
		merged.sort(new libre.result.KVStoreSequentialComparator("type", "singleLabelThreshold", "algorithm", "dataset"));

		if (INVERT_FOR_TM) {
			merged.stream().forEach(x -> x.put(TM, (-1) * x.getAsDouble(TM)));
		}

		Map<String, String> selection = new HashMap<>();
		selection.put("type", "optimistic");
		selection.put("singleLabelThreshold", "true");
		KVStoreCollection optTrue = merged.select(selection);
		selection.put("singleLabelThreshold", "false");
		KVStoreCollection optFalse = merged.select(selection);

		selection.put("type", "val");
		KVStoreCollection valFalse = merged.select(selection);
		selection.put("singleLabelThreshold", "true");
		KVStoreCollection valTrue = merged.select(selection);

		merged.stream().forEach(x -> x.put("approach", SetUtil.implode(Arrays.asList(x.getAsString("algorithm"), x.getAsString("type"), x.getAsString("singleLabelThreshold")), "-")));

		List<KVStoreCollection> colList = Arrays.asList(optTrue, valTrue, optFalse, valFalse);

		colList.stream().forEach(x -> KVStoreStatisticsUtil.best(x, "dataset", "approach", TM, "best"));

		KVStoreStatisticsUtil.wilcoxonSignedRankTest(valTrue, "dataset", "approach", "evalid", TM + "_list", "cbr-val-true", "wilcoxon");
		KVStoreStatisticsUtil.wilcoxonSignedRankTest(valFalse, "dataset", "approach", "evalid", TM + "_list", "cbr-val-false", "wilcoxon");

		Map<String, String> datasetSel = new HashMap<>();
		datasetSel.put("dataset", "genbase");

		if (INVERT_FOR_TM) {
			merged.stream().forEach(x -> x.put(TM, (-1) * x.getAsDouble(TM)));
		}

		merged.stream().forEach(x -> x.put("entry", x.getAsBoolean("best") ? "\\textbf{" + ValueUtil.valueToString(x.getAsDouble(TM), 4) + "}" : ValueUtil.valueToString(x.getAsDouble(TM), 4)));

		IntStream.range(0, colList.size()).forEach(x -> colList.get(x).stream().forEach(y -> y.put("approach", x + "-" + y.getAsString("approach"))));

		merged.stream().forEach(x -> {
			x.put("entry", x.getAsString("baselearnerValues") + "/" + x.getAsString("entry"));

			if (!x.getAsString("approach").contains("cbr")) {

				if (x.containsKey("wilcoxon")) {
					if (INVERT_FOR_TM) {
						switch (x.getAsString("wilcoxon")) {
						case "SUPERIOR":
							x.put("entry", x.getAsString("entry") + " $\\circ$");
							break;
						case "INFERIOR":
							x.put("entry", x.getAsString("entry") + " $\\bullet$");
							break;
						default:
							x.put("entry", x.getAsString("entry") + " $\\phantom{\\circ}$");
							break;
						}
					} else {
						switch (x.getAsString("wilcoxon")) {
						case "INFERIOR":
							x.put("entry", x.getAsString("entry") + " $\\circ$");
							break;
						case "SUPERIOR":
							x.put("entry", x.getAsString("entry") + " $\\bullet$");
							break;
						default:
							x.put("entry", x.getAsString("entry") + " $\\phantom{\\circ}$");
							break;
						}
					}
				}
			}
		});

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(merged, "dataset", "approach", "entry");

		System.out.println("\\resizebox{\\textwidth}{!}{\n" + latexTable + "}");
	}

	private static KVStoreCollection getSBS(final KVStoreCollection col) {
		TwoLayerKVStoreCollectionPartition twoLayerPartition = new TwoLayerKVStoreCollectionPartition("type", "singleLabelThreshold", col);
		KVStoreCollection sbsCol = new KVStoreCollection();

		for (Entry<String, Map<String, KVStoreCollection>> entryType : twoLayerPartition) {
			for (Entry<String, KVStoreCollection> thresholdType : entryType.getValue().entrySet()) {
				IKVStore sbs = null;
				String optValueKey = "avgOptEvalValue";
				for (IKVStore store : thresholdType.getValue()) {
					if (sbs == null || store.getAsDouble(optValueKey) > sbs.getAsDouble(optValueKey)) {
						sbs = store;
					}
				}
				sbsCol.add(sbs);
			}
		}

		sbsCol.stream().forEach(x -> {
			x.put("algorithm", "sbs");
		});

		return sbsCol;
	}

}
