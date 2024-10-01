package pt.ua.dicoogle.server.web.servlets.mlprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.core.mlprovider.CreateDatasetRequest;
import pt.ua.dicoogle.plugins.PluginController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CreateDatasetServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateDatasetServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String dataString = IOUtils.toString(req.getReader());
        if (dataString == null) {
            resp.sendError(404, "Empty POST body");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        CreateDatasetRequest datasetRequest;
        try {
            datasetRequest = mapper.readValue(dataString, CreateDatasetRequest.class);
            /*
            if(PluginController.getInstance().getMachineLearningProviderByName(datasetRequest.getProviderName(), true) == null){
                resp.sendError(404, "The requested provider does not exist");
            }*/
            PluginController.getInstance().prepareMLDataset(datasetRequest);
        } catch (Exception e) {
            log.error("Error parsing json string", e);
            resp.sendError(404, "Malformed request");
        }
    }
}
