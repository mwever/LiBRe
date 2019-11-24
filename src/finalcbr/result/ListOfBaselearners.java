package finalcbr.result;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import finalcbr.GlobalConfig;

public class ListOfBaselearners {

	public static void main(final String[] args) {

		Set<String> baselearners = new HashSet<>();
		for (File dataset : GlobalConfig.RESULT_DIR.listFiles()) {
			for (File baselearner : dataset.listFiles()) {
				baselearners.add(baselearner.getName());
			}
		}

		List<String> blList = new LinkedList<>();
		baselearners.stream().map(x -> x.substring(x.lastIndexOf('.') + 1)).forEach(blList::add);
		Collections.sort(blList);
		System.out.println(blList.stream().collect(Collectors.joining(", ")));
	}

}
