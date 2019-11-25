package libre.result;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import ai.libs.jaicore.basic.ValueUtil;
import ai.libs.jaicore.basic.kvstore.IKVStore;
import ai.libs.jaicore.basic.kvstore.KVStore;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;
import ai.libs.jaicore.basic.kvstore.KVStoreUtil;

public class BaselearnerFrequencies {

	private static final String SUFFIX_1 = "Standard Accuracy.kvstore";// "-Accuracy.kvstore";
	private static final String SUFFIX_2 = "F1.kvstore";// "-Accuracy.kvstore";
	private static final File STORES_DIR = new File("brexp/kvstore");

	private static final String SINGLE_LABEL_THRESHOLD = "false";

	private static void countBaselearners(final String type, final KVStoreCollection col, final Map<String, Double> counterMapCBR, final Map<String, Double> counterMapSBS) {
		Map<String, String> selection = new HashMap<>();
		selection.put("algorithm", "cbr");
		selection.put("type", type);
		selection.put("singleLabelThreshold", SINGLE_LABEL_THRESHOLD);
		KVStoreCollection cbrs = col.select(selection);

		int l = cbrs.get(0).getAsStringList("baselearners").size();
		cbrs.get(0).getAsStringList("baselearners").stream().forEach(x -> counterMapCBR.put(x, counterMapCBR.computeIfAbsent(x, t -> 0.0) + 1.0));

		IKVStore bestBaseline = null;
		for (IKVStore baseline : col) {
			if (baseline.getAsString("type").equals(type) && !baseline.getAsString("algorithm").equals("cbr") && baseline.getAsString("singleLabelThreshold").equals(SINGLE_LABEL_THRESHOLD)) {
				if (bestBaseline == null || baseline.getAsDouble("avgOptEvalValue") > bestBaseline.getAsDouble("avgOptEvalValue")) {
					bestBaseline = baseline;
				}
			}
		}

		counterMapSBS.put(bestBaseline.getAsString("algorithm"), counterMapSBS.computeIfAbsent(bestBaseline.getAsString("algorithm"), t -> 0.0) + l);
	}

	private static final void addToFinalCol(final String name, final Map<String, Double> counterMap, final KVStoreCollection allCol) {
		double sumCBR = counterMap.values().stream().mapToDouble(x -> x).sum();
		for (Entry<String, Double> cbrEntry : counterMap.entrySet()) {
			KVStore store = new KVStore();
			store.put("approach", name);
			store.put("baselearner", cbrEntry.getKey());
			store.put("frequency", ValueUtil.round(cbrEntry.getValue() / sumCBR, 4));
			allCol.add(store);
		}
	}

	public static void main(final String[] args) throws IOException {
		KVStoreCollection allCol = new KVStoreCollection();

		for (String suf : Arrays.asList(SUFFIX_1, SUFFIX_2)) {
			Map<String, Double> counterMapCBR = new HashMap<>();
			Map<String, Double> counterMapSBS = new HashMap<>();
			Map<String, Double> counterMapCBROpt = new HashMap<>();
			Map<String, Double> counterMapSBSOpt = new HashMap<>();

			for (File resultFiles : STORES_DIR.listFiles()) {
				if (!resultFiles.getName().endsWith(suf)) {
					continue;
				}

				KVStoreCollection col = new KVStoreCollection("name=dummy\n" + FileUtils.readFileToString(resultFiles));

				countBaselearners("optimistic", col, counterMapCBR, counterMapSBS);
				countBaselearners("optimistic", col, counterMapCBROpt, counterMapSBSOpt);
			}

			addToFinalCol("cbrVal" + suf, counterMapCBR, allCol);
			addToFinalCol("sbsVal" + suf, counterMapSBS, allCol);
//			addToFinalCol("cbrOpt" + suf, counterMapCBROpt, allCol);
//			addToFinalCol("sbsOpt" + suf, counterMapSBSOpt, allCol);
		}

		allCol.stream().forEach(x -> x.put("baselearner", x.getAsString("baselearner").substring(x.getAsString("baselearner").lastIndexOf('.') + 1)));

		String latexTable = KVStoreUtil.kvStoreCollectionToLaTeXTable(allCol, "approach", "baselearner", "frequency", "0.0");
		String csvTable = KVStoreUtil.kvStoreCollectionToCSVTable(allCol, "approach", "baselearner", "frequency", "0.0");

		System.out.println(latexTable);

		System.out.println("CSV Table:");
		System.out.println("[[" + csvTable.substring(0, csvTable.length() - 1).replaceAll(";", ",").replaceAll("\n", "],\n[") + "]]");
	}

}
