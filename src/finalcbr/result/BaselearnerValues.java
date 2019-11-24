package finalcbr.result;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ai.libs.jaicore.basic.FileUtil;
import ai.libs.jaicore.basic.kvstore.KVStoreCollection;

public class BaselearnerValues {

	public static void main(final String[] args) throws IOException {
		KVStoreCollection col = new KVStoreCollection(FileUtil.readFileAsString(new File("brexp/kvstore/medical-F1.kvstore")));

		Map<String, String> selection = new HashMap<>();
		selection.put("type", "val");
		selection.put("singleLabelThreshold", "false");
		col.select(selection).stream().map(x -> x.get("algorithm") + "\n" + x.getAsString("baselearnerValues")).forEach(System.out::println);
	}

}
