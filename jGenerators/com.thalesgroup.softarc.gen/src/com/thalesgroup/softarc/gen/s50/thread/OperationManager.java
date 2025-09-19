/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread;

import java.util.LinkedHashMap;
import java.util.Map;

import java.io.IOException;
import com.thalesgroup.softarc.gen.s50.thread.sizing.OperationSizer;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Platform;
import com.thalesgroup.softarc.sf.PortData;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.tools.InconsistentModelError;

// Délégation du calcul des dimensionnements de chaque opération déclarée dans
// un fichier d'assemblage

public class OperationManager {

    private static final long OPERATION_HEADER_SIZE = 16;

    private final boolean simulation;
    private final boolean generateObservation;

    public OperationManager(boolean isSimulation, boolean generateObservation) {
        this.simulation = isSimulation;
        this.generateObservation = generateObservation;
    }

    // Analyse le fichier d'assemblage pour en extraire le contexte des
    // opérations, organisées par identifiant numérique (sous forme de chaîne)

    public Map<Long, OperationContext> generateOperationContext(Assembly assembly) throws IOException {
        Map<Long, OperationContext> result = new LinkedHashMap<Long, OperationContext>();

        OperationSizer operationSizer = new OperationSizer(this.simulation);
        long operId;

        // RequestResponses

        for (RequestResponseLink serviceLink : assembly.getRequestResponseLinks()) {

            operId = serviceLink.getId();

            // On prend le point de vue du client, qui a l'avantage d'être
            // à la fois obligatoire et unique dans le langage ASSEMBLY
            RequestResponseLinkReceiver link = serviceLink.getClient();

            OperationRequestResponse operationDefinition = link.getPort().getRequestResponse();
            OperationContext ctxt = new OperationContext(operationDefinition.getIsAsynchronous(), link.getPort().getInstance(),
                    serviceLink);
            operationSizer.computeRequestResponseSize(operationDefinition, ctxt);
            result.put(operId, ctxt);
        }

        // Événements

        for (EventLink eventLink : assembly.getEventLinks()) {
            operId = eventLink.getId();

            // Un événement a au moins un récepteur (attention pour les virtual eventlinks il n'y a pas d'émetteur)
            EventLinkReceiver link = eventLink.getReceivers().iterator().next();

            OperationEvent operationDefinition = link.getPort().getEvent();

            OperationContext ctxt = new OperationContext(true, link.getPort().getInstance(), eventLink);
            operationSizer.computeEventSize(operationDefinition, ctxt);
            result.put(operId, ctxt);
        }

        // Données

        for (DataLink dataLink : assembly.getDataLinks()) {
            operId = dataLink.getId();

            // Une donnée a au moins un écrivain (mais pas forcément de
            // lecteur) ... ou pas mais c'est le plus probable
            PortData link = null;
            OperationData operationDefinition = null;

            if (dataLink.getWriters().size() > 0) {
                link = dataLink.getWriters().iterator().next().getPort();
                operationDefinition = link.getData();
            }

            if (operationDefinition == null) {
                // Pas d'écrivain on prend un reader
                link = dataLink.getReaders().iterator().next().getPort();
                operationDefinition = link.getData();
            }

            if (operationDefinition == null) {
                throw new InconsistentModelError("Datalink id = " + operId + " shall have at least one reader or writer");
            } else {
                OperationContext ctxt = new OperationContext(true, link.getInstance(), dataLink);
                operationSizer.computeDataSize(operationDefinition, ctxt);
                result.put(operId, ctxt);
            }
        }

        return result;
    }

    private long bufferSizeRequestResponse(OperationContext operationCtxt) {
        return OPERATION_HEADER_SIZE + Math.max(operationCtxt.in.raw_size, operationCtxt.out.raw_size);
    }

    private long bufferSizeEvent(OperationContext operationCtxt) {
        return OPERATION_HEADER_SIZE + operationCtxt.in.raw_size;
    }

    private long bufferSizeData(OperationContext operationCtxt) {
        return OPERATION_HEADER_SIZE + operationCtxt.in.raw_size;
    }

    public void computeInstancesBufferSizes(Assembly assemblyModel) throws IOException {

        OperationSizer operationSizer = new OperationSizer(this.simulation);

        // RequestResponses
        for (RequestResponseLink link : assemblyModel.getRequestResponseLinks()) {

            // Required
            {
                RequestResponseLinkReceiver linkc = link.getClient();
                OperationRequestResponse op = linkc.getPort().getRequestResponse();
                OperationContext ctxt = new OperationContext(op.getIsAsynchronous(), linkc.getPort().getInstance(), link);
                operationSizer.computeRequestResponseSize(op, ctxt);
                ctxt.updateBufferSize(bufferSizeRequestResponse(ctxt));
            }

            // Provided
            for (RequestResponseLinkReceiver linksvr : link.getServers()) {
                OperationRequestResponse op = linksvr.getPort().getRequestResponse();
                OperationContext ctxt = new OperationContext(false, linksvr.getPort().getInstance(), link);
                operationSizer.computeRequestResponseSize(op, ctxt);
                ctxt.updateBufferSize(bufferSizeRequestResponse(ctxt));
            }
        }

        // Events
        for (EventLink link : assemblyModel.getEventLinks()) {
            // Received

            // Received events need to be sized for GenLib purpose, but do not
            // have any impact on SOFTARC buffers sizing.
            for (EventLinkReceiver r : link.getReceivers()) {
                OperationEvent op = r.getPort().getEvent();
                OperationContext ctxt =
                        new OperationContext(false, r.getPort().getInstance(), link);
                operationSizer.computeEventSize(op, ctxt);
                // ctxt.updateBufferSize(bufferSizeEvent(ctxt));
            }

            // Sent
            for (EventLinkSender s : link.getSenders()) {
                OperationEvent op = s.getPort().getEvent();
                OperationContext ctxt =
                        new OperationContext(false, s.getPort().getInstance(), link);
                operationSizer.computeEventSize(op, ctxt);
                ctxt.updateBufferSize(bufferSizeEvent(ctxt));
            }
        }

        // Data
        for (DataLink link : assemblyModel.getDataLinks()) {
            // Read (for observation, or else for JAVA and PYTHON only)
            for (DataLinkElement r : link.getReaders()) {
                OperationData op = r.getPort().getData();
                OperationContext ctxt = new OperationContext(false, r.getPort().getInstance(), link);
                operationSizer.computeDataSize(op, ctxt);

                if (generateObservation || r.getPort().getInstance().getType().getIsJavaComponent()
                        || r.getPort().getInstance().getType().getIsPythonComponent()) {
                    ctxt.updateBufferSize(bufferSizeData(ctxt));
                }
            }

            // Written (for observation, or if the data is sent outside the current Site)
            for (DataLinkElement w : link.getWriters()) {
                OperationData op = w.getPort().getData();
                OperationContext ctxt = new OperationContext(false, w.getPort().getInstance(), link);
                operationSizer.computeDataSize(op, ctxt); // must be done for all operations (needed by GenLib)

                if (generateObservation || !isLocal(link)) {
                    ctxt.updateBufferSize(bufferSizeData(ctxt));
                }
            }
        }
    }

    /**
     * Find if the data is local or not
     */
    private boolean isLocal(DataLink link) {
        for (DataLinkElement r : link.getReaders()) {
            if (r.getPort().getInstance().getIsExtern()) {
                return false;
            }
        }
        return true;
    }
}
