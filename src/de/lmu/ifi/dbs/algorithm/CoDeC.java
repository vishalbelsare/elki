package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.algorithm.classifier.CorrelationBasedClassifier;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TODO comment
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CoDeC extends AbstractAlgorithm<RealVector> {

	public static final String EVALUATE_AS_CLASSIFIER_F = "classify";

	public static final String EVALUATE_AS_CLASSIFIER_D = "demand evaluation of the cluster-models as classifier";

	public static final String CLASS_LABEL_P = "classlabel";

	public static final String DEFAULT_CLASS_LABEL_CLASS = HierarchicalClassLabel.class.toString();

	public static final String CLASS_LABEL_D = "use the designated classLabel class "
			+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ClassLabel.class) + ". Default: " + DEFAULT_CLASS_LABEL_CLASS;

	public static final String CLUSTERING_ALGORITHM_P = "clusteringAlgorithm";

	public static final String CLUSTERING_ALGORITHM_D = "the clustering algorithm to use to derive cluster "
			+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) + ". Default: " + COPAC.class.getName();

	private boolean evaluateAsClassifier = false;

	@SuppressWarnings("unchecked")
	private ClassLabel classLabel;

	@SuppressWarnings("unchecked")
	private Result<RealVector> result;

	@SuppressWarnings("unchecked")
	private Clustering<RealVector> clusteringAlgorithm;

	/**
	 * The Dependency Derivator algorithm.
	 */
	@SuppressWarnings("unchecked")
	private DependencyDerivator dependencyDerivator = new DependencyDerivator();

	@SuppressWarnings("unchecked")
	private Classifier<RealVector> classifier = new CorrelationBasedClassifier();

	public CoDeC() {
		super();
		optionHandler.put(EVALUATE_AS_CLASSIFIER_F, new Flag(EVALUATE_AS_CLASSIFIER_F, EVALUATE_AS_CLASSIFIER_D));

		// parameter class label
		ClassParameter classLabel = new ClassParameter(CLASS_LABEL_P, CLASS_LABEL_D, ClassLabel.class);
		classLabel.setDefaultValue(HierarchicalClassLabel.class.toString());
		optionHandler.put(CLASS_LABEL_P, classLabel);

		// parameter clustering algorithm
		ClassParameter clAlg = new ClassParameter(CLUSTERING_ALGORITHM_P, CLUSTERING_ALGORITHM_D, Clustering.class);
		clAlg.setDefaultValue(COPAC.class.getName());
		optionHandler.put(CLUSTERING_ALGORITHM_P, clAlg);
	}

	/**
	 * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
	 */
	@SuppressWarnings("unchecked")
	protected void runInTime(Database<RealVector> database) throws IllegalStateException {
		// run clustering algorithm
		if (isVerbose()) {
			verbose("\napply clustering algorithm: " + clusteringAlgorithm.getClass().getName());
		}
		clusteringAlgorithm.run(database);
		// if evaluate as classifier:
		// demand database with class labels, evaluate
		// CorrelationBasedClassifier
		if (evaluateAsClassifier) {
			Database<RealVector> annotatedClasses = clusteringAlgorithm.getResult().associate((Class<ClassLabel>) classLabel.getClass());
			classifier.run(annotatedClasses);
			result = classifier.getResult();
		}
		// if not evaluate as classifier:
		// derive dependency model for every cluster
		else {
			ClusteringResult<RealVector> clusterResult = clusteringAlgorithm.getResult();
			Map<ClassLabel, Database<RealVector>> cluster = clusterResult.clustering((Class<ClassLabel>) classLabel.getClass());
			List<ClassLabel> keys = new ArrayList<ClassLabel>(cluster.keySet());
			Collections.sort(keys);
			for (ClassLabel label : keys) {
				if (isVerbose()) {
					verbose("Deriving dependencies for cluster " + label.toString());
				}
				dependencyDerivator.run(cluster.get(label));
				clusterResult.appendModel(label, dependencyDerivator.getResult());
			}
			result = clusterResult;
		}

	}

	/**
	 * @see Algorithm#getResult()
	 */
	public Result<RealVector> getResult() {
		return result;
	}

	/**
	 * @see Algorithm#getDescription()
	 */
	public Description getDescription() {
		return new Description("CoDeC", "Correlation Derivator and Classifier", "...", "unpublished");
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	@Override
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super.getAttributeSettings();

		attributeSettings.addAll(clusteringAlgorithm.getAttributeSettings());
		if (evaluateAsClassifier) {
			attributeSettings.addAll(classifier.getAttributeSettings());
		} else {
			attributeSettings.addAll(dependencyDerivator.getAttributeSettings());
		}
		return attributeSettings;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// evaluateAsClassifier
		evaluateAsClassifier = optionHandler.isSet(EVALUATE_AS_CLASSIFIER_F);

		// classlabel
		String classLabelString = (String) optionHandler.getOptionValue(CLASS_LABEL_P);

		try {
			classLabel = Util.instantiate(ClassLabel.class, classLabelString);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException(CLASS_LABEL_P, classLabelString, CLASS_LABEL_D, e);
		}

		// clustering algorithm
		String clusteringAlgorithmString = (String) optionHandler.getOptionValue(CLUSTERING_ALGORITHM_P);
		try {
			// noinspection unchecked
			clusteringAlgorithm = Util.instantiate(Clustering.class, clusteringAlgorithmString);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException(CLUSTERING_ALGORITHM_P, clusteringAlgorithmString, CLUSTERING_ALGORITHM_D, e);
		}

		clusteringAlgorithm.setTime(isTime());
		clusteringAlgorithm.setVerbose(isVerbose());
		remainingParameters = clusteringAlgorithm.setParameters(remainingParameters);

		if (evaluateAsClassifier) {
			classifier.setTime(isTime());
			classifier.setVerbose(isVerbose());
			remainingParameters = classifier.setParameters(remainingParameters);
		} else {
			dependencyDerivator.setTime(isTime());
			dependencyDerivator.setVerbose(isVerbose());
			remainingParameters = dependencyDerivator.setParameters(remainingParameters);
		}
		setParameters(args, remainingParameters);
		return remainingParameters;
	}

}
