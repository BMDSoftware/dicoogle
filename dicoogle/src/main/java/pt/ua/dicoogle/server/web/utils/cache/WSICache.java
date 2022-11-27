package pt.ua.dicoogle.server.web.utils.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData;
import org.dcm4che3.io.BulkDataDescriptor;
import org.dcm4che3.io.DicomInputStream;
import pt.ua.dicoogle.core.settings.ServerSettingsManager;
import pt.ua.dicoogle.plugins.PluginController;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.utils.QueryException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Cache used to store DicomMetadata objects temporarily, as they are quite heavy to build on-demand
 * @author Rui Jesus <r.jesus@ua.pt>
 */
public class WSICache extends MemoryCache<DicomMetaData>{

    private static WSICache instance = null;
    private static final String EXTENSION_GZIP = ".gz";
    private static final int BUFFER_SIZE = 8192;

    private QueryInterface queryInterface;
    private final String queryProvider;

    private WSICache(){
        super();
        memoryCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(hoursToKeep, TimeUnit.HOURS)
                .build(new WsiDcmLoader());

        List<String> dicomProviders = ServerSettingsManager.getSettings().getArchiveSettings().getDIMProviders();
        queryProvider = dicomProviders.iterator().next();
    }

    public static synchronized WSICache getInstance(){
        if (instance==null){
            instance = new WSICache();
        }
        return instance;
    }

    private class WsiDcmLoader extends CacheLoader<String, DicomMetaData> {

        @Override
        public DicomMetaData load(String sopInstanceUID) throws Exception {

            queryInterface = PluginController.getInstance().getQueryProviderByName(queryProvider, false);

            URI uri = retrieveURI(sopInstanceUID);
            if(uri == null){
                logger.info("URI == null");
                throw new InvalidParameterException("Could not find the desired URI");
            }

            Attributes fmi;
            Attributes dataset;
            DicomInputStream dis;
            StorageInputStream sis = retrieveInputStream(uri);

            if(sis == null){
                throw new InvalidParameterException("Could not find the desired URI");
            }

            String filePath = sis.getURI().getPath();
            if (filePath.endsWith(EXTENSION_GZIP)){
                InputStream inStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE));
                dis = new DicomInputStream(inStream);
            }
            else {
                dis = new DicomInputStream(new File(filePath));
            }

            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.URI);
            dis.setBulkDataDescriptor(BulkDataDescriptor.PIXELDATA);
            fmi = dis.readFileMetaInformation();
            dataset = dis.readDataset(-1, -1);
            return new DicomMetaData(fmi, dataset);
        }

    }

    /**
     * Helper method to retrieve the URI from SOPInstanceUID in lucene.
     *
     * @param sop SopInstanceUID
     * @return uri of the SopInstance
     */
    private URI retrieveURI(String sop){
        String query = "SOPInstanceUID:" + sop;

        Iterable<SearchResult> results;
        try {
            results = queryInterface.query(query);
        } catch (QueryException e) {
            logger.error("Could not complete query:", e);
            return null;
        }

        for (SearchResult first : results) {
            if (first != null) {
                return first.getURI();
            }
        }
        return null;
    }

    private StorageInputStream retrieveInputStream(URI uri){
        return PluginController.getInstance().resolveURI(uri).iterator().next();
    }

}
