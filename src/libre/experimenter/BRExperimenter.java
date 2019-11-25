package libre.experimenter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.IDatabaseConfig;
import ai.libs.jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import ai.libs.jaicore.basic.algorithm.exceptions.AlgorithmTimeoutedException;
import ai.libs.jaicore.experiments.ExperimentDBEntry;
import ai.libs.jaicore.experiments.ExperimentDatabasePreparer;
import ai.libs.jaicore.experiments.ExperimentRunner;
import ai.libs.jaicore.experiments.IExperimentDatabaseHandle;
import ai.libs.jaicore.experiments.IExperimentIntermediateResultProcessor;
import ai.libs.jaicore.experiments.IExperimentSetEvaluator;
import ai.libs.jaicore.experiments.databasehandle.ExperimenterMySQLHandle;
import ai.libs.jaicore.experiments.exceptions.ExperimentAlreadyExistsInDatabaseException;
import ai.libs.jaicore.experiments.exceptions.ExperimentDBInteractionFailedException;
import ai.libs.jaicore.experiments.exceptions.ExperimentEvaluationFailedException;
import ai.libs.jaicore.experiments.exceptions.IllegalExperimentSetupException;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import meka.core.Result;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class BRExperimenter {
	/**
	 * Variables for the experiment and database setup
	 */
	private static final File configFile = new File("brexp/setup.properties");
	private static final IBRExperimenterConfig m = (IBRExperimenterConfig) ConfigFactory.create(IBRExperimenterConfig.class).loadPropertiesFromFile(configFile);
	private static final IDatabaseConfig dbconfig = (IDatabaseConfig) ConfigFactory.create(IDatabaseConfig.class).loadPropertiesFromFile(configFile);
	private static final IExperimentDatabaseHandle dbHandle = new ExperimenterMySQLHandle(dbconfig);
	private static final Logger logger = LoggerFactory.getLogger(BRExperimenter.class);

	public static void main(final String[] args)
			throws ExperimentDBInteractionFailedException, AlgorithmTimeoutedException, IllegalExperimentSetupException, ExperimentAlreadyExistsInDatabaseException, InterruptedException, AlgorithmExecutionCanceledException {
		// createTableWithExperiments();
		if (args.length <= 0) {
			System.out.println("Executor needs to be executed with flag:");
			System.out.println("-t\tFor generating the experiment tables");
			System.out.println("-c\tFor deleting the entire table");
			System.out.println("-s\tFor a single run");
			System.out.println("-r\tFor infinite run");
			System.exit(0);
		}

		System.out.println("Passed argument: >" + args[0] + "<");
		switch (args[0]) {
		case "-t":
			System.out.println("Create table with experiments...");
			createTableWithExperiments();
			break;
		case "-c":
			System.out.println("Delete table...");
			deleteTable();
			break;
		case "-s":
			System.out.println("Conduct single experiment run...");
			runExperiments(1);
			break;
		case "-r":
			System.out.println("Conduct infinite experiment runs...");
			runExperiments(-1);
			break;
		}
	}

	public static void createTableWithExperiments()
			throws ExperimentDBInteractionFailedException, AlgorithmTimeoutedException, IllegalExperimentSetupException, ExperimentAlreadyExistsInDatabaseException, InterruptedException, AlgorithmExecutionCanceledException {
		ExperimentDatabasePreparer preparer = new ExperimentDatabasePreparer(m, dbHandle);
		preparer.synchronizeExperiments();
	}

	public static void deleteTable() throws ExperimentDBInteractionFailedException {
		dbHandle.deleteDatabase();
	}

	private static String getFileName(final String... parts) {
		return Arrays.asList(parts).stream().collect(Collectors.joining("_")) + ".arff";
	}

	public static void runExperiments(final int i) throws ExperimentDBInteractionFailedException, InterruptedException {
		ExperimentRunner runner = new ExperimentRunner(m, new IExperimentSetEvaluator() {
			@Override
			public void evaluate(final ExperimentDBEntry experimentEntry, final IExperimentIntermediateResultProcessor processor) throws InterruptedException, ExperimentEvaluationFailedException {
				/* get experiment setup */
				Map<String, String> description = experimentEntry.getExperiment().getValuesOfKeyFields();
				String dataset = description.get("dataset");
				String baselearner = description.get("baselearner");
				int testseed = Integer.parseInt(description.get("testseed"));
				int testfold = Integer.parseInt(description.get("testfold"));
				int valseed = Integer.parseInt(description.get("valseed"));
				int valfold = Integer.parseInt(description.get("valfold"));

				/* create objects for experiment */
				logger.info("Evaluate BR with baselearner {} for dataset {} and testseed {} testfold {} valseed {} and valfold {}", baselearner, dataset, testseed, testfold, valseed, valfold);

				/* run fictive experiment */
				long timeStartTraining = System.currentTimeMillis();

				String trainFile;
				String testFile;
				if (valfold == -1) {
					trainFile = getFileName("2cv", testseed + "", (1 - testfold) + "", -1 + "", valfold + "");
					testFile = getFileName("2cv", testseed + "", testfold + "", -1 + "", valfold + "");
				} else {
					trainFile = getFileName("2cv", testseed + "", (1 - testfold) + "", valseed + "", (1 - valfold) + "");
					testFile = getFileName("2cv", testseed + "", (1 - testfold) + "", valseed + "", valfold + "");
				}

				try {
					System.out.println(new File(m.getDatasetFolder(), dataset));
					File trainFileFile = new File(new File(m.getDatasetFolder(), dataset), trainFile);
					File testFileFile = new File(new File(m.getDatasetFolder(), dataset), testFile);

					Instances train = new Instances(new FileReader(trainFileFile));
					Instances test = new Instances(new FileReader(testFileFile));
					MLUtils.prepareData(train);
					MLUtils.prepareData(test);
					System.out.println("Loaded datasets, let's go!");

					BR br = new BR(experimentEntry.getExperiment().getNumCPUs());
					br.setOptions(new String[] { "-output-debug-info" });
					Classifier c = AbstractClassifier.forName(baselearner, new String[] {});
					br.setClassifier(c);
					Result res = Evaluation.evaluateModel(br, train, test);
					Instances resInstances = Result.getPredictionsAsInstances(res);
					resInstances.setRelationName(c.getClass().getName() + "_testSeed" + testseed + "_testFold" + testfold + "_valSeed" + valseed + "_valFold" + valfold);

					File outputFile = new File(new File(new File(m.getOutputFolder(), dataset), baselearner), getFileName("2cv", testseed + "", testfold + "", valseed + "", valfold + ""));
					outputFile.getParentFile().mkdirs();
					try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
						bw.write(resInstances.toString());
					}

					/* report results */
					Map<String, Object> results = new HashMap<>();
					results.put("evaltime", System.currentTimeMillis() - timeStartTraining);
					results.put("done", "true");
					processor.processResults(results);
				} catch (Exception e) {
					throw new ExperimentEvaluationFailedException(e);
				}
			}
		}, dbHandle);
		runner.randomlyConductExperiments(i);
	}

}
