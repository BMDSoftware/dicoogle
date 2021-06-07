/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle.
 *
 * Dicoogle/dicoogle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.server;

import pt.ua.dicoogle.core.ServerSettings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;

///import org.dcm4che2.net.Executor;
/** dcm4che doesn't support Executor anymore, so now import from java.util */ 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.StorageService;
import org.dcm4che2.net.service.VerificationService;


import pt.ua.dicoogle.plugins.PluginController;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.datastructs.Report;


/**
 * DICOM Storage Service is provided by this class
 * @author Marco Pereira
 */

public class RSIStorage extends StorageService
{
    
    private SOPList list;
    private ServerSettings settings;
        
    private Executor executor = new NewThreadExecutor("RSIStorage");
    private Device device = new Device("RSIStorage");
    private NetworkApplicationEntity nae = new NetworkApplicationEntity();
    private NetworkConnection nc = new NetworkConnection();
    
    private String path;
    private DicomDirCreator dirc;
    
    private int fileBufferSize = 256;
    private int threadPoolSize = 10;
    
    private ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
    
    private boolean gzip = ServerSettings.getInstance().isGzipStorage();;

    private Set<String> alternativeAETs = new HashSet<>();
    private Set<String> priorityAETs = new HashSet<>();

    // Changed to support priority queue.
    private BlockingQueue<ImageElement> queue = new PriorityBlockingQueue<>();
    private NetworkApplicationEntity[] naeArr = null;
    private AtomicLong seqNum = new AtomicLong(0L);

    
    /**
     * 
     * @param Services List of supported SOP Classes
     * @param l list of Supported SOPClasses with supported Transfer Syntax
     * @param s Server Settings for this execution of the storage service
     */
    
    public RSIStorage(String [] Services, SOPList l)
    {
        //just because the call to super must be the first instruction
        super(Services); 
        
            //our configuration format
            list = l;
            settings = ServerSettings.getInstance();

            // Added default alternative AETitle.
            alternativeAETs.add(ServerSettings.getInstance().getNodeName());

            path = settings.getPath();
            if (path == null) {
                path = "/dev/null";
            }

            this.priorityAETs = settings.getPriorityAETitles();
            LoggerFactory.getLogger(RSIStorage.class).debug("Priority C-STORE: " + this.priorityAETs);

            device.setNetworkApplicationEntity(nae);

            device.setNetworkConnection(nc);
            nae.setNetworkConnection(nc);

            //we accept assoociations, this is a server
            nae.setAssociationAcceptor(true);
            //we support the VerificationServiceSOP
            nae.register(new VerificationService());
            //and the StorageServiceSOP
            nae.register(this);

            nae.setAETitle(settings.getAE());


            nc.setPort(settings.getStoragePort());
            this.nae.setInstalled(true);
            this.nc.setMaxScpAssociations(Integer.parseInt(System.getProperty("dicoogle.cstore.maxScpAssociations", "50")));
            this.nc.setAcceptTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.acceptTimeout", "5000")));
            this.nc.setConnectTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.connectTimeout", "5000")));


            this.nae.setAssociationAcceptor(true);
            this.nae.setAssociationInitiator(false);

            ServerSettings s  = ServerSettings.getInstance();
            this.nae.setDimseRspTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.dimseRspTimeout", "18000000")));
            //
            this.nae.setIdleTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.idleTimeout", "18000000")));
            this.nae.setMaxPDULengthReceive(s.getMaxPDULengthReceive()+Integer.parseInt(System.getProperty("dicoogle.cstore.appendMaxPDU", "1000")));
            this.nae.setMaxPDULengthSend(s.getMaxPDULenghtSend()+Integer.parseInt(System.getProperty("dicoogle.cstore.appendMaxPDU", "1000")));
            this.nae.setRetrieveRspTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.retrieveRspTimeout", "18000000")));


            // Added alternative AETitles.

            naeArr = new NetworkApplicationEntity[alternativeAETs.size()+1];
            // Just adding the first AETitle
            naeArr[0] = nae;
            
            int k = 1 ; 
            
            for (String alternativeAET: alternativeAETs)
            {
                NetworkApplicationEntity nae2 = new NetworkApplicationEntity();
                nae2.setNetworkConnection(nc);
                nae2.setDimseRspTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.dimseRspTimeout", "18000000")));
                nae2.setIdleTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.idleTimeout", "18000000")));
                nae2.setMaxPDULengthReceive(s.getMaxPDULengthReceive()+Integer.parseInt(System.getProperty("dicoogle.cstore.appendMaxPDU", "1000")));
                nae2.setRetrieveRspTimeout(Integer.parseInt(System.getProperty("dicoogle.cstore.retrieveRspTimeout", "18000000")));
                //we accept assoociations, this is a server
                nae2.setAssociationAcceptor(true);
                //we support the VerificationServiceSOP
                nae2.register(new VerificationService());
                //and the StorageServiceSOP
                nae2.register(this);
                nae2.setAETitle(alternativeAET);
                ServerSettings settings = ServerSettings.getInstance();
                String[] array = settings.getCAET();

                if (array != null)
                {
                    nae2.setPreferredCallingAETitle(settings.getCAET());
                }
                naeArr[k] = nae2;
                k++;
                
            }

            // Just set the Network Application Entity array - which accepts a set of AEs.
            device.setNetworkApplicationEntity(naeArr);

            

            initTS(Services);
    }
    /**
     *  Sets the tranfer capability for this execution of the storage service
     *  @param Services Services to be supported
     */
    private void initTS(String [] Services)
    {
        int count = list.getAccepted();
        //System.out.println(count);
        TransferCapability[] tc = new TransferCapability[count + 1];
        String [] Verification = {UID.ImplicitVRLittleEndian, UID.ExplicitVRLittleEndian, UID.ExplicitVRBigEndian};
        String [] TS;
        TransfersStorage local;        

        tc[0] = new TransferCapability(UID.VerificationSOPClass, Verification, TransferCapability.SCP);
        int j = 0;
        for (int i = 0; i < Services.length; i++)
        {
            count = 0;
            local = list.getTS(Services[i]);  
            if (local.getAccepted())
            {
                TS = local.getVerboseTS();
                if(TS != null)
                {                

                    tc[j+1] = new TransferCapability(Services[i], TS, TransferCapability.SCP);
                    j++;
                }                        
            }
        }
        
        // Setting the TS in all NetworkApplicationEntitys 
        for (int i = 0 ; i<naeArr.length;i++)
        {

            naeArr[i].setTransferCapability(tc);
        }
        nae.setTransferCapability(tc);
    }
      
    @Override
    /**
     * Called when a C-Store Request has been accepted
     * Parameters defined by dcm4che2
     */
    public void cstore(final Association as, final int pcid, DicomObject rq, PDVInputStream dataStream, String tsuid) throws DicomServiceException, IOException
    {
        //DebugManager.getInstance().debug(":: Verify Permited AETs @??C-Store Request ");

        boolean permited = false;

        if(ServerSettings.getInstance().getPermitAllAETitles()){
            permited = true;
        }
        else {
            String permitedAETs[] = ServerSettings.getInstance().getCAET();

            for (int i = 0; i < permitedAETs.length; i++) {
                if (permitedAETs[i].equals(as.getCallingAET())) {
                    permited = true;
                    break;
                }
            }
        }

        if (!permited) {
            //DebugManager.getInstance().debug("Client association NOT permited: " + as.getCallingAET() + "!");
            System.err.println("Client association NOT permited: " + as.getCallingAET() + "!");
            as.abort();
            
            return;
        }

        final DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
        onCStoreRQ(as, pcid, rq, dataStream, tsuid, rsp);
        as.writeDimseRSP(pcid, rsp);       
        //onCStoreRSP(as, pcid, rq, dataStream, tsuid, rsp);
    }
    
    @Override
    /**
     * Actually do the job of saving received file on disk
     * on this server with extras such as Lucene indexing
     * and DICOMDIR update
     */
    protected void onCStoreRQ(Association as, int pcid, DicomObject rq, PDVInputStream dataStream, String tsuid, DicomObject rsp) throws IOException, DicomServiceException 
    {  
        try
        {

            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            String iuid = rq.getString(Tag.AffectedSOPInstanceUID);

            DicomObject d = dataStream.readDataset();
            
            d.initFileMetaInformation(cuid, iuid, tsuid);
            
            Iterable <StorageInterface> plugins = PluginController.getInstance().getStoragePlugins(true);

            for (StorageInterface storage : plugins) {
                URI uri = storage.store(d);
                if (uri != null) {
                    // enqueue to index
                    ImageElement element = new ImageElement(uri, as.getCallingAET(), seqNum.getAndIncrement());
                    queue.add(element);
                }
            }

        } catch (IOException e) {
           throw new DicomServiceException(rq, Status.ProcessingFailure, e.getMessage());          
         }
    }

    /**
     * A C-STORE entry.
     * For Each C-STORE RQ, an ImageElement is created
     * and put in the storage service's priority queue for indexing.
     * 
     * The priority criteria are, in descending order of importance:
     * 1. whether the calling AE title is in the list of priority AEs
     * 2. earliest sequence number
     *
     * This only happens after the store in Storage Plugins.
     */
    final class ImageElement implements Comparable<ImageElement> {
        private final URI uri;
        private final String callingAET;
        private final long seqNumber;

        ImageElement(URI uri, String callingAET, long seqNumber) {
            Objects.requireNonNull(uri);
            Objects.requireNonNull(callingAET);
            this.uri = uri;
            this.callingAET = callingAET;
            this.seqNumber = seqNumber;
        }

        public URI getUri() {
            return uri;
        }

        public String getCallingAET() {
            return callingAET;
        }

        public long getSequenceNumber() {
            return seqNumber;
        }

        @Override
        public int compareTo(ImageElement other) {
            boolean thisPriority = priorityAETs.contains(this.getCallingAET());
            boolean thatPriority = priorityAETs.contains(other.getCallingAET());

            int priorityOrder = Boolean.compare(thisPriority, thatPriority);

            if (priorityOrder != 0) {
                return priorityOrder;
            }

            return Long.compare(this.seqNumber, other.seqNumber);
        }
    }

    
    class Indexer extends Thread
    {
        public Collection<IndexerInterface> plugins;
        
        public void run()
        {
            while (true)
            {
                try 
                {
                    // Fetch an element by the queue taking into account the priorities.
                    ImageElement element = queue.take();
                    URI exam = element.getUri();
                    List <Report> reports = PluginController.getInstance().indexBlocking(exam);
                } catch (InterruptedException ex) {
                    LoggerFactory.getLogger(DicomStorage.class).error("Could not take instance to index", ex);
                }
                 
            }
            
        }
    }
    
    
    private String getFullPath(DicomObject d)
    {
    
        return getDirectory(d) + File.separator + getBaseName(d);
    
    }
    
    
    private String getFullPathCache(String dir, DicomObject d)
    {    
        return dir + File.separator + getBaseName(d);
 
    }
    
    
    
    private String getBaseName(DicomObject d)
    {
        String result = "UNKNOWN.dcm";
        String sopInstanceUID = d.getString(Tag.SOPInstanceUID);
        return sopInstanceUID+".dcm";
    }
    
    
    private String getDirectory(DicomObject d)
    {
    
        String result = "UN";
        
        String institutionName = d.getString(Tag.InstitutionName);
        String modality = d.getString(Tag.Modality);
        String studyDate = d.getString(Tag.StudyDate);
        String accessionNumber = d.getString(Tag.AccessionNumber);
        String studyInstanceUID = d.getString(Tag.StudyInstanceUID);
        String patientName = d.getString(Tag.PatientName);
        
        if (institutionName==null || institutionName.equals(""))
        {
            institutionName = "UN_IN";
        }
        institutionName = institutionName.trim();
        institutionName = institutionName.replace(" ", "");
        institutionName = institutionName.replace(".", "");
        institutionName = institutionName.replace("&", "");

        
        if (modality == null || modality.equals(""))
        {
            modality = "UN_MODALITY";
        }
        
        if (studyDate == null || studyDate.equals(""))
        {
            studyDate = "UN_DATE";
        }
        else
        {
            try
            {
                String year = studyDate.substring(0, 4);
                String month =  studyDate.substring(4, 6);
                String day =  studyDate.substring(6, 8);
                
                studyDate = year + File.separator + month + File.separator + day;
                
            }
            catch(Exception e)
            {
                e.printStackTrace();
                studyDate = "UN_DATE";
            }
        }
        
        if (accessionNumber == null || accessionNumber.equals(""))
        {
            patientName = patientName.trim();
            patientName = patientName.replace(" ", "");
            patientName = patientName.replace(".", "");
            patientName = patientName.replace("&", "");
            
            if (patientName == null || patientName.equals(""))
            {
                if (studyInstanceUID == null || studyInstanceUID.equals(""))
                {
                    accessionNumber = "UN_ACC";
                }
                else
                {
                    accessionNumber = studyInstanceUID;
                }
            }
            else
            {
                accessionNumber = patientName;
                
            }
            
        }
        
        result = path+File.separator+institutionName+File.separator+modality+File.separator+studyDate+File.separator+accessionNumber;
        
        return result;
        
    }
    private Indexer indexer = new Indexer();
    /*
     * Start the Storage Service 
     * @throws java.io.IOException
     */
    public void start() throws IOException
    {       
        //dirc = new DicomDirCreator(path, "Dicoogle");
        pool = Executors.newFixedThreadPool(threadPoolSize);
        device.startListening(executor); 
        indexer.start();
        

    } 
    
    /**
     * Stop the storage service 
     */
    public void stop()
    {
        this.pool.shutdown();
        try {
            pool.awaitTermination(6, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            LoggerFactory.getLogger(RSIStorage.class).error(ex.getMessage(), ex);
        }
        device.stopListening();
        
        //dirc.dicomdir_close();
    }   
}
