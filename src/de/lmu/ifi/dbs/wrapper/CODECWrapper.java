package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.CoDeC;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Wrapper class for the CoDeC algorithm. Performs an attribute wise
 * normalization on the database objects, partitions the database according to
 * the correlation dimension of its objects, performs the algorithm DBSCAN over
 * the partitions and then determines the correlation dependencies in each
 * cluster of each partition.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CODECWrapper extends NormalizationWrapper {

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "the maximum radius of the neighborhood to"
                                         + "be considerd, must be suitable to "
                                         + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of "
                                   + "nearest neighbors considered in the PCA. "
                                   + "If this value is not defined, k ist set to minpts";

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private String minpts;

  /**
   * The value of the k parameter.
   */
  private String k;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    CODECWrapper wrapper = new CODECWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
    	wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
    	wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter epsilon, minpts and k in the parameter map
   * additionally to the parameters provided by super-classes.
   */
  public CODECWrapper() {
    super();
    optionHandler.put(DBSCAN.EPSILON_P, new Parameter(DBSCAN.EPSILON_P,EPSILON_D,Parameter.Types.DISTANCE_PATTERN));
    optionHandler.put(DBSCAN.MINPTS_P, new Parameter(DBSCAN.MINPTS_P,OPTICS.MINPTS_D,Parameter.Types.INT));
    optionHandler.put(KnnQueryBasedHiCOPreprocessor.K_P, new Parameter(KnnQueryBasedHiCOPreprocessor.K_P,K_D,Parameter.Types.INT));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm CoDeC
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(CoDeC.class.getName());

    // clustering algorithm COPAC
    parameters.add(OptionHandler.OPTION_PREFIX + CoDeC.CLUSTERING_ALGORITHM_P);
    parameters.add(COPAC.class.getName());

    // partition algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    parameters.add(DBSCAN.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(minpts);

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // omit preprocessing
    parameters.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F);

    // preprocessor for correlation dimension
    parameters.add(OptionHandler.OPTION_PREFIX + COPAA.PREPROCESSOR_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class .getName());

    // k
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
    parameters.add(k);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // epsilon, minpts
    epsilon = optionHandler.getOptionValue(OPTICS.EPSILON_P);
    minpts = optionHandler.getOptionValue(OPTICS.MINPTS_P);
    // k
    if (optionHandler.isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
      k = optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P);
    }
    else {
      k = minpts;
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(OPTICS.EPSILON_P, epsilon);
    mySettings.addSetting(OPTICS.MINPTS_P, minpts);
    mySettings.addSetting(KnnQueryBasedHiCOPreprocessor.K_P, k);
    return settings;
  }

}
