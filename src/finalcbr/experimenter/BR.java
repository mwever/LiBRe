package finalcbr.experimenter;
/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import meka.classifiers.multilabel.ProblemTransformationMethod;
import meka.core.F;
import meka.core.MLUtils;
import meka.core.MultiLabelDrawable;
/**
 * BR.java - The Binary Relevance Method.
 * The standard baseline Binary Relevance method (BR) -- create a binary problems for each label and learn a model for them individually.
 * See also <i>BR</i> from the <a href=http://mulan.sourceforge.net>MULAN</a> framework
 * @author 	Jesse Read (jmr30@cs.waikato.ac.nz)
 */
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;

public class BR extends ProblemTransformationMethod implements MultiLabelDrawable {

	/** for serialization. */
	private static final long serialVersionUID = -5390512540469007904L;

	protected Classifier m_MultiClassifiers[] = null;
	protected Instances m_InstancesTemplates[] = null;
	protected NominalToBinary m_NominalToBinary[] = null;

	private final int numCPUs;

	public BR(final int numCPUs) {
		super();
		this.numCPUs = numCPUs;
	}

	/**
	 * Description to display in the GUI.
	 *
	 * @return the description
	 */
	@Override
	public String globalInfo() {
		return "The Binary Relevance Method.\n" + "See also MULAN framework:\n" + "http://mulan.sourceforge.net";
	}

	@Override
	public void buildClassifier(final Instances D) throws Exception {
		this.testCapabilities(D);
		int L = D.classIndex();

		if (this.getDebug()) {
			System.out.println(Thread.currentThread().getName() + ": Creating " + L + " models (" + this.m_Classifier.getClass().getName() + "): ");
		}
		this.m_MultiClassifiers = AbstractClassifier.makeCopies(this.m_Classifier, L);
		this.m_InstancesTemplates = new Instances[L];
		this.m_NominalToBinary = new NominalToBinary[L];
		IntStream.range(0, L).forEach(x -> this.m_NominalToBinary[x] = new NominalToBinary());
		final Lock lock = new ReentrantLock();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.numCPUs);
		Semaphore sem = new Semaphore(0);
		AtomicBoolean buildFailed = new AtomicBoolean(false);
		List<Throwable> exception = Collections.synchronizedList(new LinkedList<>());

		IntStream.range(0, L).forEach(j -> {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					// Select only class attribute 'j'
					try {
						Instances D_j = F.keepLabels(new Instances(D), L, new int[] { j });
						D_j.setClassIndex(0);

						BR.this.m_NominalToBinary[j].setInputFormat(D_j);
						D_j = Filter.useFilter(D_j, BR.this.m_NominalToBinary[j]);

						lock.lock();
						try {
							BR.this.m_InstancesTemplates[j] = new Instances(D_j, 0);
						} finally {
							lock.unlock();
						}
						// Build the classifier for that class
						BR.this.m_MultiClassifiers[j].buildClassifier(D_j);
						if (BR.this.getDebug()) {
							System.out.println(Thread.currentThread().getName() + ": " + (D_j.classAttribute().name()));
						}
						sem.release();
					} catch (Throwable e) {
						exception.add(e);
						buildFailed.set(true);
						sem.release(L);
					}
				}

			});
		});

		sem.acquire(L);
		if (buildFailed.get()) {
			executor.shutdownNow();
			throw new Exception(exception.get(0));
		} else {
			executor.shutdown();
			executor.awaitTermination(24, TimeUnit.HOURS);
		}

		// sanity check
		for (Instances temp : this.m_InstancesTemplates) {
			if (temp == null) {
				throw new Exception("Not all instances templates are filled.");
			}
		}
	}

	@Override
	public double[] distributionForInstance(final Instance x) throws Exception {
		int L = x.classIndex();
		double y[] = new double[L];

		for (int j = 0; j < L; j++) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Thread has been interrupted.");
			}
			Instance x_j = (Instance) x.copy();
			x_j.setDataset(null);
			x_j = MLUtils.keepAttributesAt(x_j, new int[] { j }, L);

			this.m_NominalToBinary[j].input(x_j);
			this.m_NominalToBinary[j].batchFinished();
			x_j = this.m_NominalToBinary[j].output();

			x_j.setDataset(this.m_InstancesTemplates[j]);

			// y[j] = m_MultiClassifiers[j].classifyInstance(x_j);
			y[j] = this.m_MultiClassifiers[j].distributionForInstance(x_j)[1];
		}

		return y;
	}

	/**
	 * Returns the type of graph representing the object.
	 *
	 * @return the type of graph representing the object (label index as key)
	 */
	@Override
	public Map<Integer, Integer> graphType() {
		Map<Integer, Integer> result;
		int i;

		result = new HashMap<>();

		if (this.m_MultiClassifiers != null) {
			for (i = 0; i < this.m_MultiClassifiers.length; i++) {
				if (this.m_MultiClassifiers[i] instanceof Drawable) {
					result.put(i, ((Drawable) this.m_MultiClassifiers[i]).graphType());
				}
			}
		}

		return result;
	}

	/**
	 * Returns a string that describes a graph representing the object. The string should be in XMLBIF
	 * ver. 0.3 format if the graph is a BayesNet, otherwise it should be in dotty format.
	 *
	 * @return the graph described by a string (label index as key)
	 * @throws Exception
	 *           if the graph can't be computed
	 */
	@Override
	public Map<Integer, String> graph() throws Exception {
		Map<Integer, String> result;
		int i;
		result = new HashMap<>();

		if (this.m_MultiClassifiers != null) {
			for (i = 0; i < this.m_MultiClassifiers.length; i++) {
				if (this.m_MultiClassifiers[i] instanceof Drawable) {
					result.put(i, ((Drawable) this.m_MultiClassifiers[i]).graph());
				}
			}
		}
		return result;
	}

	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 9117 $");
	}

	public static void main(final String args[]) {
		ProblemTransformationMethod.evaluation(new BR(4), args);
	}

}
