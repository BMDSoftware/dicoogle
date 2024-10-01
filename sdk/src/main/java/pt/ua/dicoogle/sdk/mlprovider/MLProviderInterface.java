package pt.ua.dicoogle.sdk.mlprovider;

import pt.ua.dicoogle.sdk.DicooglePlugin;
import pt.ua.dicoogle.sdk.task.Task;

import java.util.List;
import java.util.Set;

/**
 * Interface to define Machine Learning providers.
 * A machine learning provider can be a remote service, hosted on the cloud, or a simple remote/local server
 * that has installed machine learning algorithms for problem solving.
 * Machine learning providers can work with either image or csv datasets.
 * The purpose of this interface is to provide a way to develop plugins to integrate with services such as Google's Vertex API or Amazon's SageMaker API.
 *
 * @author Rui Jesus
 */
public abstract class MLProviderInterface implements DicooglePlugin {

    protected Set<ML_DATA_TYPE> acceptedDataTypes;

    /**
     * This method uploads data to the provider.
     * The API assumes three kinds of data:
     *   - CSV files, identified by a URI
     *   - Image files, identified by a URI
     *   - DICOM files, identified by their respective UIDs.
     * This method can be used to upload labelled or un-labelled datasets to the provider.
     */
    public abstract void dataStore(MLDataset dataset);

    /**
     * This method creates a model using a specific dataset
     */
    public abstract MLModel createModel();

    /**
     * This method orders the training of a model.
     * @param modelID the unique model identifier within the provider.
     */
    public abstract boolean trainModel(String modelID);

    /**
     * This method creates a endpoint that exposes a service
     */
    public abstract void createEndpoint();

    /**
     * This method lists all available endpoints
     */
    public abstract List<MLEndpoint> listEndpoints();

    /**
     * This method deletes a endpoint
     */
    public abstract void deleteEndpoint();

    /**
     * This method deploys a model
     */
    public abstract void deployModel();

    /**
     * This method lists the models created on this provider.
     */
    public abstract List<MLModel> listModels();

    /**
     * This method deletes a model
     */
    public abstract void deleteModel();

    /**
     * Order a prediction over a single object.
     * The object can be a series instance, a sop instance or a 2D/3D ROI.
     *
     * @param predictionRequest object that defines this prediction request
     */
    public abstract Task<MLPrediction> makePrediction(MLPredictionRequest predictionRequest);

    /**
     * This method makes a bulk prediction using the selected model
     */
    public abstract void makeBulkPrediction();

    public Set<ML_DATA_TYPE> getAcceptedDataTypes() {
        return acceptedDataTypes;
    }

    public void setAcceptedDataTypes(Set<ML_DATA_TYPE> acceptedDataTypes) {
        this.acceptedDataTypes = acceptedDataTypes;
    }
}
