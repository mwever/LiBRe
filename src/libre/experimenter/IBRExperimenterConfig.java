package libre.experimenter;

import java.io.File;
import java.util.List;

import ai.libs.jaicore.experiments.IExperimentSetConfig;

public interface IBRExperimenterConfig extends IExperimentSetConfig {

	// static properties
	public static final String DATASET_FOLDER = "datasetFolder";
	public static final String OUTPUT_FOLDER = "outputFolder";

	// cross product properties
	public static final String DATASETS = "dataset";
	public static final String BASE_LEARNERS = "baselearner";
	public static final String TEST_SEED = "testseed";
	public static final String TEST_FOLD = "testfold";
	public static final String VAL_SEED = "valseed";
	public static final String VAL_FOLD = "valfold";

	@Key(DATASETS)
	public List<String> getDatasets();

	@Key(BASE_LEARNERS)
	public List<String> getBaselearners();

	@Key(TEST_SEED)
	public List<Integer> getTestSeeds();

	@Key(VAL_SEED)
	public List<Integer> getValSeeds();

	@Key(TEST_FOLD)
	public List<Integer> getTestFolds();

	@Key(VAL_FOLD)
	public List<Integer> getValFolds();

	@Key(DATASET_FOLDER)
	public File getDatasetFolder();

	@Key(OUTPUT_FOLDER)
	public File getOutputFolder();

}
