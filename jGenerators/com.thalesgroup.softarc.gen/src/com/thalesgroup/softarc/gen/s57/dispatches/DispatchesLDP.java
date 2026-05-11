package com.thalesgroup.softarc.gen.s57.dispatches;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.IdAllocator;
import com.thalesgroup.softarc.gen.s50.thread.Constants;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.Dispatch;
import com.thalesgroup.softarc.sf.DispatchOperationsMapEntry;
import com.thalesgroup.softarc.sf.DispatchedOperation;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Executable;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.Link;
import com.thalesgroup.softarc.sf.OperationLink;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.impl.QDispatch;
import com.thalesgroup.softarc.sf.impl.QDispatchOperationsMapEntry;
import com.thalesgroup.softarc.sf.impl.QDispatchedOperation;
import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.deployment.IOInterfaceIn;
import technology.ecoa.model.deployment.IOOperation;
import technology.ecoa.model.deployment.IOSection;

import java.io.IOException;
import java.util.*;

public class DispatchesLDP extends AbstractPass {
    private final Map<String, Dispatch> dispatchThreads = new LinkedHashMap<>();

    private IdAllocator threadNumber;

    @Override
    public void execute() throws Exception {
        long initialValue = context.system.getMapping().getMaxThreadId() + 1;
        threadNumber  = new IdAllocator("Thread", initialValue);
        generateThreadDispatch();
    }

    private void generateThreadDispatch() throws IOException {
        if(context.DEFILE.getExternalIo() != null) {
            createDispatchThreads();
            resolveDispatchableOperations(context.system.getMapping().getGlobalExecutable(), context.DEFILE);
            linkNotificationEvents(context.system.getMapping().getGlobalExecutable());
        }
    }


    private void resolveDispatchableOperations(Executable exec, DEApplication deployment) throws IOException {
        IOSection externalIo = deployment.getExternalIo();
        List<IOInterfaceIn> externalInChannels = externalIo.getInPort();

        for(IOInterfaceIn inChannel : externalInChannels ) {
            Dispatch dispatchThread = getDispatchThread(inChannel.getName());
            Map<Long, List<DispatchedOperation>> operationsMapByExternalId = new TreeMap<>();
            for(IOOperation inOperation : inChannel.getOperation()) {
                // ICI : Couple IOInterfance / IOOperation -> ExternalOperationInfo

                //On récupère tous les liens en lien avec le nom de l'opération déclarée.
                List<Link> allLinksFromInOperation = getLinkForInOperation(inOperation);
                long externalId = Long.parseLong(inOperation.getId());

                Map<Long, List<DispatchedOperation>> dispatchedOperationsAlreadyCreated = new TreeMap<>();
                for(Link link : allLinksFromInOperation){
                    for (Instance instance : exec.getInstances()) {
                        for (OperationLink operationLink : instance.getAllLinks()) {
                            boolean operationMatched = false;
                            long operationReqId = operationLink.getId();
                            if(operationLink.getId() == link.getId()
                                && (operationLink.getIsReceivedEvent()
                                    || (operationLink.getIsProvidedRequestResponse() && !operationLink.getIsDirect())
                                    || operationLink.getData() != null) ){
                                operationMatched = true;
                            }

                            //Cas de la callback
                            if (link.getId() == operationLink.getId()
                                    && operationLink.getIsRequiredRequestResponse()) {
                                operationReqId = operationLink.getId() + 1;
                                operationMatched = true;
                            }

                            if(operationMatched) {
                                if(dispatchedOperationsAlreadyCreated.containsKey(operationReqId)){
                                    //On ne veut pas ajouter une DispatchedOperation s'il y en a déjà une avec le même reqId et dont
                                    //le thread de destination est le même que que celui du lien parcouru. (Sinon message envoyé en double)
                                    for(DispatchedOperation dop: dispatchedOperationsAlreadyCreated.get(operationReqId)){
                                        if(!dop.getLink().getDestinationThreads().contains(instance.getThread())){
                                            operationLink.getDestinationThreads().add(instance.getThread()); //Toujours un seul thread car vu de l'instance
                                            DispatchedOperation newDop = createDispatchedOperation(operationLink,
                                                    operationReqId, inOperation.getName(), dispatchThread);
                                            dispatchedOperationsAlreadyCreated.get(operationReqId).add(newDop);
                                            operationsMapByExternalId.get(externalId).add(newDop);
                                        }
                                    }
                                } else {
                                    operationLink.getDestinationThreads().add(instance.getThread()); //Toujours un seul thread car vu de l'instance
                                    DispatchedOperation dop = createDispatchedOperation(operationLink,
                                            operationReqId, inOperation.getName(), dispatchThread);
                                    if (!operationsMapByExternalId.containsKey(externalId)) {
                                        // On ajoute les instances destinatrices
                                        operationsMapByExternalId.put(externalId, new ArrayList<>());
                                    }
                                    dispatchedOperationsAlreadyCreated.put(operationReqId, List.of(dop));
                                    operationsMapByExternalId.get(externalId).add(dop);
                                }
                            }

                            //Maybe something to do with data here for serialization if needed
                        }
                    }
                }

            }
            addAllOperationsToDispatch(operationsMapByExternalId, dispatchThread);
        }
    }

    private DispatchedOperation createDispatchedOperation(OperationLink operationLink, long operationReqId, String operationName, Dispatch dispatch ) {
        DispatchedOperation dop = new QDispatchedOperation();
        dop.setXmlID(
                "dispatchedoperation:"
                        + dispatch.getName()
                        + '/'
                        + dispatch.getId()
                        + '/'
                        + operationReqId);
        dop.setReqId(operationReqId);
        dop.setLink(operationLink);
        dop.setOperationName(operationName);
        dop.setIsReceivedEvent(operationLink.getIsReceivedEvent());
        dop.setIsProvidedRequestResponse(operationLink.getIsProvidedRequestResponse());
        dop.setIsRequiredRequestResponse(operationLink.getIsRequiredRequestResponse());
        dop.setIsReadData(operationLink.getData() != null);
        assert dop.getIsReceivedEvent()
                || dop.getIsProvidedRequestResponse()
                || dop.getIsRequiredRequestResponse()
                || dop.getIsReadData();
        {
            long size_max = getSizeMax(operationLink, dop);
            dop.setSizeMax(size_max);
        }

        return dop;
    }

    private List<Link> getLinkForInOperation(IOOperation operation) {
        ArrayList<Link> links = new ArrayList<>();
        Assembly assembly = context.system.getAssembly();
        boolean isExtern;
        for (EventLink link : assembly.getEventLinks()) {
            isExtern = false;
            if (link.getNotifiedData() == null) {
                for(EventLinkSender eventSender: link.getSenders()){
                    if(eventSender.getPort().getInstance().getIsExtern()) {
                        isExtern = true;
                        break;
                    }
                }
                if(isExtern) {
                    for (EventLinkReceiver eventReceiver : link.getReceivers()) {
                        if (eventReceiver.getPort().getOperation().getName().equals(operation.getName())) {
                            links.add(link);
                        }
                    }
                }
            }
        }

        for (DataLink link : assembly.getDataLinks()) {
            isExtern = false;
            for(DataLinkElement le : link.getWriters()) {
                if(le.getPort().getInstance().getIsExtern()) {
                    isExtern = true;
                }
            }
            if(isExtern) {
                for (DataLinkElement le : link.getReaders()) {
                    if (le.getPort().getOperation().getName().equals(operation.getName())) {
                        links.add(link);
                    }
                }
            }
        }

        for (RequestResponseLink link : assembly.getRequestResponseLinks()) {
            isExtern = false;
            for (RequestResponseLinkReceiver le : link.getServers()) {
                if (le.getPort().getOperation().getName().equals(operation.getName())) {
                    if(le.getPort().getInstance().getIsExtern()) {
                        isExtern = true;
                    }
                    links.add(link);
                }
            }
            if (link.getClient().getPort().getOperation().getName().equals(operation.getName()) && isExtern) {
                links.add(link);
            }
        }
        return links;
    }

    private void createDispatchThreads() throws IOException {
        // Identification des opérations à aiguiller
        Executable globalExec = context.system.getMapping().getGlobalExecutable();

        IOSection externalIo = context.DEFILE.getExternalIo();
        List<IOInterfaceIn> externalInChannels = externalIo.getInPort();
        externalInChannels.addAll(externalIo.getInOutPort());

        if(!externalInChannels.isEmpty()) {
            globalExec.setIsDispatcher(true);
        }

        //Pour chaque port In présent dans le déploiement.
        //On récupère les opérations présentes, puis les liens associés à ces opérations.
        // On cherche derrière à créer des DispatchedsLinks
        for(IOInterfaceIn inChannel : externalInChannels ) {
            getDispatchThread(inChannel.getName());
        }

        // Rattachement des threads d'aiguillage à l'aiguilleur
        for (Dispatch dispatchThread : this.dispatchThreads.values()) {
            dispatchThread.setIdNo(this.threadNumber.allocate());

            dispatchThread.setName(Constants.THREAD_PREFIX_DISPATCH + dispatchThread.getChannelName());
            dispatchThread.setXmlID("thread:" + globalExec.getName() + '/' + dispatchThread.getName());
            dispatchThread.setId(dispatchThread.getName().toUpperCase());
            dispatchThread.setIsPrompt(true);
            dispatchThread.setParent(globalExec);
            globalExec.getDispatches().add(dispatchThread);
            context.system.getMapping().setMaxThreadId(context.system.getMapping().getMaxThreadId() + 1);
        }


    }

    private Dispatch getDispatchThread(String pProducerName){
        Dispatch result = this.dispatchThreads.get(pProducerName);

        if (result == null) {
            result = new QDispatch();
            result.setStack(Constants.SARC_DEFAULT_THREAD_STACK_SIZE);
            result.setChannelName(pProducerName);
            this.dispatchThreads.put(pProducerName, result);
        }

        return result;
    }


    private void linkNotificationEvents(Executable exec) {

        // Link notification events reqId to data dispatched by current
        // executable
        for (Dispatch dispatchThread : exec.getDispatches()) {
            for (DispatchOperationsMapEntry e : dispatchThread.getOperationsMapByExternalId()) {
                for (DispatchedOperation operation : e.getOperations()) {
                    if (operation.getIsReadData()) {
                        // Search for notification events of this data update
                        // among all events dispatched by current executable
                        for (long eventId : notificationLinks(operation.getReqId())) {
                            operation.getNotificationReqIds().add(eventId);
                        }
                    }
                }
            }
        }
    }

    private Collection<Long> notificationLinks(long dataLinkId) {
        ArrayList<Long> result = new ArrayList<>();
        for (EventLink link : context.system.getAssembly().getEventLinks()) {
            if (link.getNotifiedData() != null && link.getNotifiedData().getId() == dataLinkId) {
                result.add(link.getId());
            }
        }
        return result;
    }

    private static void addAllOperationsToDispatch(Map<Long, List<DispatchedOperation>> operationsMapByExternalId, Dispatch dispatchThread) {
        for (Map.Entry<Long, List<DispatchedOperation>> entry : operationsMapByExternalId.entrySet()) {
            DispatchOperationsMapEntry e = new QDispatchOperationsMapEntry();
            e.setExternalId(entry.getKey());
            boolean isFirst = true;
            for (DispatchedOperation dop : entry.getValue()) {
                if (!isFirst) {
                    assert e.getIsReceivedEvent() == dop.getIsReceivedEvent();
                    assert e.getIsProvidedRequestResponse() == dop.getIsProvidedRequestResponse();
                    assert e.getIsRequiredRequestResponse() == dop.getIsRequiredRequestResponse();
                    assert e.getIsReadData() == dop.getIsReadData();
                    assert e.getNbProducers() == dop.getNbProducers();
                    assert e.getSizeMax() == dop.getSizeMax();
                }

                isFirst = false;
                e.setIsReceivedEvent(dop.getIsReceivedEvent());
                e.setIsProvidedRequestResponse(dop.getIsProvidedRequestResponse());
                e.setIsRequiredRequestResponse(dop.getIsRequiredRequestResponse());
                e.setIsReadData(dop.getIsReadData());
                e.setNbProducers(dop.getNbProducers());
                e.setSizeMax(dop.getSizeMax());
            }
            e.getOperations().addAll(entry.getValue());
            dispatchThread.getOperationsMapByExternalId().add(e);
        }
    }

    private static long getSizeMax(OperationLink operationLink, DispatchedOperation dop) {
        long size_max = 0;

        if (dop.getIsReadData()) {
            size_max = operationLink.getData().getSize();
        } else if (dop.getIsReceivedEvent()) {
            size_max = operationLink.getEvent().getSize();
        } else if (dop.getIsProvidedRequestResponse()) {
            size_max = operationLink.getRequestResponse().getSize();
        } else if (dop.getIsRequiredRequestResponse()) {
            size_max = operationLink.getRequestResponse().getSizeOut();
        }
        return size_max;
    }

}
