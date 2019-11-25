# LiBRe
Implementation and data for LiBRe

# Implementation Details
The implementation of LiBRe is available in Java (see ```src directory```). To run the experiments and the analyses it relies on the library AILibs release ```0.1.6``` (```ai.libs:mlplan:0.1.6```). The implementation contains everything from A-Z:

* A parallelized implementation of Binary Relevance learning (package ```libre.experimenter```)
* The experimenter to evaluate all the dataset splits and combinations of Binary Relevance learning and the base learners (package ```libre.experimenter```)
* The analysis including threshold optimization (package ```libre.analysis```)
* The generator for the tables of the paper. (package ```ai.libs.jaicore.basic.kvstore```)

# Available Data
As for this evaluation a vast amount of data has been needed and thus generated, this data is also provided within this repository. First of all we provide the dataset splits used for the nested 2-fold cross validation in order to ensure reproducibility of the evaluation of Binary Relevance learnings. Furthermore, we provide the prediction matrices to this datasets splits for the various base learners. The prediction matrices are available in ARFF format as output by MEKA's default serialization for evaluations. Finally, we also provide the output of the single best base learner selection as well as the LiBRe label-wise selection of base learners in Binary Relevance:

* The dataset train / val / test splits used for this evaluation (directory ```brexp/datasets/```)
* Prediction matrices (directory ```brexp/output/```)
* The output data of the analysis (directory ```brexp/kvstore/```)
