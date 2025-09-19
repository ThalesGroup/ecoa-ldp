/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s55.containers;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.Container;
import com.thalesgroup.softarc.sf.DataConnection;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.DataVersion;
import com.thalesgroup.softarc.sf.Dispatch;
import com.thalesgroup.softarc.sf.DispatchedLink;
import com.thalesgroup.softarc.sf.EntryPoint;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Executable;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.LinkedInstance;
import com.thalesgroup.softarc.sf.LinkedInstanceData;
import com.thalesgroup.softarc.sf.Mapping;
import com.thalesgroup.softarc.sf.Operation;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationGroup;
import com.thalesgroup.softarc.sf.OperationLink;
import com.thalesgroup.softarc.sf.OperationsMap;
import com.thalesgroup.softarc.sf.OperationsMapEntry;
import com.thalesgroup.softarc.sf.Platform;
import com.thalesgroup.softarc.sf.PortData;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.Thread;
import com.thalesgroup.softarc.sf.ThreadBase;
import com.thalesgroup.softarc.sf.impl.QNotificationLink;
import com.thalesgroup.softarc.sf.impl.QContainer;
import com.thalesgroup.softarc.sf.impl.QDataConnection;
import com.thalesgroup.softarc.sf.impl.QLinkedInstance;
import com.thalesgroup.softarc.sf.impl.QLinkedInstanceData;
import com.thalesgroup.softarc.sf.impl.QOperationGroup;
import com.thalesgroup.softarc.sf.impl.QOperationsMap;
import com.thalesgroup.softarc.sf.impl.QOperationsMapEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Création des objets de type Container (intersection d'un exécutable et d'une implémentation de
 * composant). Un même conteneur peut servir à plusieurs instances.
 */
public class ContainersCore extends AbstractPass {

    @Override
    public void execute() throws IOException {

        computeIds();

        for (Platform platform : context.system.getMapping().getPlatforms()) {

            for (Executable exec : platform.getExecutables()) {

                createSortedOperationsForInstances(exec.getInstances());
                createSortedOperationsForThreads(exec);

                listComponentTypes(exec);

                createContainers(exec);

                computeExecLanguages(exec);
                
                computeCommand(exec);

                createDataVersions(exec);

                createOperationMaps(exec);

                computeNotificationInfo(exec);
            }
        }

        computeObjectCounts();
    }

    void computeIds() {
        for (Platform platform : context.system.getMapping().getPlatforms()) {

            platform.setId("PLATFORM_" + platform.getName().toUpperCase());

            for (Executable exe : platform.getExecutables()) {
                exe.setId("EXEC_" + exe.getName().toUpperCase());

                for (Instance instance : exe.getInstances()) {
                    final String prefix;
                    if (context.isLDP)
                        prefix = "SARC";
                    else
                        prefix = instance.getType().getTypeName().toUpperCase();
                    instance.setId(prefix + "_INSTANCE_" + instance.getName().toUpperCase());
                }

                Collection<ThreadBase> threads = new ArrayList<ThreadBase>();
                threads.addAll(exe.getThreads());
                threads.addAll(exe.getMwThreads());
                threads.addAll(exe.getDispatches());
                for (ThreadBase thread : threads) {
                    thread.setId(thread.getName().toUpperCase());
                }
            }
        }
    }

    ///////////////////////

    void createSortedOperationsForInstances(Collection<Instance> instances) {
        for (Instance instance : instances) {

            TreeMap<Long, OperationGroup> sortedOperationsMap = new TreeMap<>();

            // Register all received event operations
            for (OperationLink link : instance.getReceivedEventLinks()) {
                registerOperation(link.getId(), link, sortedOperationsMap);
            }
            // Register all read data operations
            for(OperationLink link: instance.getReadDataLinks()) {
            	registerOperation(link.getId(), link, sortedOperationsMap);
            }
            
            // Register all indirect provided services operations
            for (OperationLink link : instance.getProvidedRequestResponsesLinks()) {
                if (!link.getIsDirect()) {
                    registerOperation(link.getId(), link, sortedOperationsMap);
                }
            }
            // Register all required services callback operations
            for (OperationLink link : instance.getRequiredRequestResponsesLinks()) {
                if (link.getRequestResponse().getIsAsynchronous()) {
                    // Note: All callbacks return to the same client, because there is only one client per servicelink.
                    // => only one OperationLink is needed in this OperationGroup
                    registerOperationOnlyOnce(link.getId() + 1, link, sortedOperationsMap);
                }
            }
            instance.getSortedOperations().addAll(sortedOperationsMap.values());
        }
    }

    void createSortedOperationsForThreads(Executable exec) {
        for (Thread thread : exec.getThreads()) {
            TreeMap<Long, OperationGroup> threadSortedOperationsMap = new TreeMap<>();

            for (Instance instance : exec.getInstances()) {
                if (!instance.getWrittenDataLinks().isEmpty()) {
                    thread.setPublishesData(true);
                }

                // Merge instance-specific operation groups into thread-level operation groups
                for (OperationGroup instanceOpGroup : instance.getSortedOperations()) {

                    long reqId = instanceOpGroup.getReqId();
                    OperationGroup threadOpGroup = threadSortedOperationsMap.get(reqId);
                    if (threadOpGroup == null) {
                        threadOpGroup = new QOperationGroup();
                        threadOpGroup.setReqId(reqId);
                        threadSortedOperationsMap.put(reqId, threadOpGroup);
                    }
                    threadOpGroup.getOperations().addAll(instanceOpGroup.getOperations());
                }
            }
        }
    }

    private OperationGroup findOperationGroup(long reqId, OperationLink operation,
            TreeMap<Long, OperationGroup> sortedOperationsMap) {
        OperationGroup opGroup = sortedOperationsMap.get(reqId);
        if (opGroup == null) {
            opGroup = new QOperationGroup();
            opGroup.setReqId(reqId);
            opGroup.setIsReceivedEvent(operation.getIsReceivedEvent());
            opGroup.setIsRequiredRequestResponse(operation.getIsRequiredRequestResponse());
            opGroup.setIsProvidedRequestResponse(operation.getIsProvidedRequestResponse());
            opGroup.setIsDataRead(operation.getIsDataRead());
            opGroup.setIsOnSameTask(operation.getIsOnSameTask());
            sortedOperationsMap.put(reqId, opGroup);
        }
        return opGroup;
    }

    /**
     * Add 'operation' to the group identified by 'reqId', creating it if needed.
     */
    private void registerOperation(long reqId, OperationLink operation, TreeMap<Long, OperationGroup> sortedOperationsMap) {
        OperationGroup opGroup = findOperationGroup(reqId, operation, sortedOperationsMap);
        opGroup.getOperations().add(operation);
    }

    /**
     * Add 'operation' to the group identified by 'reqId', creating it if needed, only if it contains no other operation.
     */
    private void registerOperationOnlyOnce(long reqId, OperationLink operation,
            TreeMap<Long, OperationGroup> sortedOperationsMap) {
        OperationGroup opGroup = findOperationGroup(reqId, operation, sortedOperationsMap);
        if (opGroup.getOperations().isEmpty()) {
            opGroup.getOperations().add(operation);
        }
    }

    ///////////////

    void listComponentTypes(Executable exec) {
        TreeSet<Component> componentTypes =
                new TreeSet<>(
                        new Comparator<Component>() {
                            public int compare(Component o1, Component o2) {
                                return o1.getFullName().compareTo(o2.getFullName());
                            };
                        });
        for (Instance instance : exec.getInstances()) {
            componentTypes.add(instance.getType());
            if (instance.getType().getIsPythonComponent()) {
                exec.getPythonComponents().add(instance.getType());
            }
        }
        exec.getComponentTypes().addAll(componentTypes);
    }

    //////////////////////

    void createContainers(Executable exec) {
        for (Component cmp : exec.getComponentTypes()) {
            Container container = new QContainer();
            container.setComponent(cmp);
            container.setParent(exec);
            for (Instance instance : exec.getInstances()) {
                if (instance.getType() == cmp) {
                    container.getInstances().add(instance);
                }
            }
            exec.getContainers().add(container);
        }
    }

    //////////////////////

    /** Determine all languages present in executable */
    void computeExecLanguages(Executable exec) throws IOException {
        boolean _compatibility = false;
        Set<Language> _languages = new LinkedHashSet<Language>();
        StringBuffer _msg = new StringBuffer();

        // Gather all languages
        for (Component cmp : exec.getComponentTypes()) {
            Language _lang = Language.valueOf(cmp.getLanguage());

            _languages.add(_lang);
            if(_lang == Language.ADA)    exec.setHasAdaInstance(true);
            if(_lang == Language.C)      exec.setHasCInstance(true);
            if(_lang == Language.CPP)    exec.setHasCppInstance(true);
            if(_lang == Language.JAVA)   exec.setHasJavaInstance(true);
            if(_lang == Language.PYTHON) exec.setHasPythonInstance(true);
            if(_lang == Language.RUST)   exec.setHasRustInstance(true);
        }
        // An executable with no components (dispatcher) is in C
        if(_languages.size() == 0) {
            _languages.add(Language.C);
        }

        _compatibility = this.checkCompatibleLanguages(exec);
        if(_compatibility) {
            _msg.append("Executable '" + exec.getName() + "' is in");
            for (Language _l : _languages) {
                _msg.append(" " + _l.name());
            }
            info(_msg.toString());
        } else {
            _msg.append("Executable '" + exec.getName() + "' cannot mix following languages:");
            for (Language _l : _languages) {
                _msg.append(" " + _l.name());
            }
            this.errorModel(_msg.toString());
        }
    }

    private void computeCommand(Executable exec) {
    	exec.setCommand("./Exec" + exec.getName());
    }

    private boolean checkCompatibleLanguages(Executable exec) {
        boolean _incompatibility = true;

        // Cannot mix JAVA with any other language
        if(exec.getHasJavaInstance()) {
            _incompatibility =  exec.getHasAdaInstance()
                             || exec.getHasCInstance()
                             || exec.getHasCppInstance()
                             || exec.getHasPythonInstance()
                             || exec.getHasRustInstance();
        }

        // Cannot mix PYTHON with JAVA
        else if(exec.getHasPythonInstance()) {
            _incompatibility =  exec.getHasJavaInstance();
        }

        // At this point, exec has only ADA, C or C++, Rust: all 4 can be mixed
        else {
            _incompatibility = false;
        }

        return !_incompatibility;
    }

    //////////////////////

    void createDataVersions(Executable exec) {
        for (DataVersion dataVR : exec.getParent().getDataVersions()) {
            if (isReferencedInDispatches(dataVR, exec.getDispatches())
                    || isReferencedInDeployedInstances(dataVR, exec)) {
                exec.getDataVersions().add(dataVR);
            }
        }
    }

    // Vrai ssi les données du dépôt doivent être aiguillées vers les
    // instances de la plateforme

    private boolean isReferencedInDispatches(DataVersion dataVR, Collection<Dispatch> collection) {
        for (Dispatch dispatch : collection) {
            for (DispatchedLink link : dispatch.getDispatchedLinks()) {
                if (link.getId() == dataVR.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Vrai ssi l'exécutable embarque (au moins) une instance de composant à
    // même d'utiliser le dépôt de données

    private boolean isReferencedInDeployedInstances(DataVersion dataVR, Executable exec) {

        // Identification de toutes les instances interagissant avec le dépôt
        // de données
        HashSet<Instance> instances = new HashSet<Instance>(exec.getInstances());
        
        for (DataLink link : context.system.getAssembly().getDataLinks()) {
            if (link.getId() == dataVR.getId()) {
                for (DataLinkElement element : link.getWriters()) {
                    if (instances.contains(element.getPort().getInstance()))
                        return true;
                }
                for (DataLinkElement element : link.getReaders()) {
                    if (instances.contains(element.getPort().getInstance()))
                        return true;
                }
            }
        }
        for (EventLink link : context.system.getAssembly().getEventLinks()) {
            if (link.getId() == dataVR.getId()) {
                for (EventLinkSender element : link.getSenders()) {
                    if (instances.contains(element.getPort().getInstance()))
                        return true;
                }
                for (EventLinkReceiver element : link.getReceivers()) {
                    if (instances.contains(element.getPort().getInstance()))
                        return true;
                }
            }
        }
        for (RequestResponseLink link : context.system.getAssembly().getRequestResponseLinks()) {
            if (link.getId() == dataVR.getId()) {
                for (RequestResponseLinkReceiver element : link.getServers()) {
                    if (instances.contains(element.getPort().getInstance()))
                        return true;
                }
                RequestResponseLinkReceiver element = link.getClient();
                if (instances.contains(element.getPort().getInstance()))
                    return true;
            }
        }

        return false;
    }

    //////////////////////

    void createOperationMaps(Executable exec) throws IOException {
        for (Container c : exec.getContainers()) {

            // version traditionnelle
            OperationsMap om = new QOperationsMap();
            initOperationsMap(om, c.getComponent(), c.getInstances());
            c.setOperationMap(om);

            // version rénovée
            // pour data
            Collection<DataConnection> dataConnections = c.getDataConnections();
            for (OperationData op : c.getComponent().getData()) {
                QDataConnection entry = new QDataConnection();
                entry.setOperation(op);
                dataConnections.add(entry);
            }
            if (!dataConnections.isEmpty()) {
                for (Instance instance : c.getInstances()) {
                    fillDataConnections(dataConnections, instance);
                }
            }
            // TODO: pour events et services
        }
    }

    private void initOperationsMap(
            OperationsMap map, Component component, Collection<Instance> instances)
            throws IOException {

        // An OperationsMap is likely to be asked on any Action
        // authorized by the Contract, independently of Instances actually
        // using them.

        initMap(map.getRequiredRequestResponses(), component.getRequiredRequestResponses());
        initMap(map.getProvidedRequestResponses(), component.getProvidedRequestResponses());
        initMap(map.getSentEvents(), component.getSentEvents());
        initMap(map.getReceivedEvents(), component.getReceivedEvents());
        initMap(map.getWrittenData(), component.getWrittenData());
        initMap(map.getReadData(), component.getReadData());

        // Then, each Instance has to fill in which Action it actually uses.

        for (Instance instance : instances) {
            fillMap(
                    map.getRequiredRequestResponses(),
                    instance,
                    instance.getRequiredRequestResponsesLinks(),
                    false);
            fillMap(
                    map.getProvidedRequestResponses(),
                    instance,
                    instance.getProvidedRequestResponsesLinks(),
                    false);
            fillMap(map.getSentEvents(), instance, instance.getSentEventLinks(), false);
            fillMap(map.getReceivedEvents(), instance, instance.getReceivedEventLinks(), false);
            fillMap(map.getWrittenData(), instance, instance.getWrittenDataLinks(), false);
            fillMap(map.getReadData(), instance, instance.getReadDataLinks(), true);
        }
    }

    /** Identify the set of Actions (of a specific kind) for which we have to generate code */
    private void initMap(
            Collection<OperationsMapEntry> map, Collection<? extends Operation> operations) {
        for (Operation op : operations) {
            OperationsMapEntry entry = new QOperationsMapEntry();
            entry.setOperation(op);
            map.add(entry);
        }
    }

    /**
     * Record all the Operations (of a specific kind) a given Instance is involved in, but inversing
     * the dependency between objects in order to fit the generation pattern.
     *
     * @param map
     * @param instance one of the Instances which uses a given Implementation
     * @param links set of the Operation in which a given Instance is involved
     */
    private void fillMap(
            Collection<OperationsMapEntry> mapEntries,
            Instance instance,
            Collection<OperationLink> links,
            boolean isRead)
            throws IOException {

        if (!links.isEmpty()) {
            LinkedHashMap<String, OperationsMapEntry> map = new LinkedHashMap<>();
            for (OperationsMapEntry e : mapEntries) {
                map.put(e.getOperation().getName(), e);
            }

            // For each Operation in which the Instance is involved
            for (OperationLink link : links) {

                // Get the Action
                OperationsMapEntry entry = map.get(link.getPort().getOperation().getName());

                // Get the Actor corresponding to the given Instance
                LinkedInstance linkedInstance = findLinkedInstance(instance, entry);

                if (linkedInstance == null) {
                    linkedInstance = new QLinkedInstance();
                    linkedInstance.setInstance(instance);
                    if (isRead) {
                        PortData portData = (PortData) (link.getPort());
                        linkedInstance.setObservation(portData.getObservationRead());
                    } else {
                        linkedInstance.setObservation(link.getPort().getObservation());
                    }
                    entry.getLinkedInstances().add(linkedInstance);
                    entry.getInstances().add(instance);
                }

                boolean found = false;
                if (link.getRequestResponse() != null) {
                    /*
                     * Note: Si le même serveur apparaît déjà dans un OperationLink de LinkedInstance.links, ne pas en créer un
                     * nouveau. Les autres seront créés dans la liste 'failoverLinks' (cf. OperationLinks.resolveRequiredRequestResponse()).
                     *
                     * Ces 2 traitements devraient probablement être regroupés.
                     */
                    for (OperationLink existingLink : linkedInstance.getLinks()) {
                        if (existingLink.getServer() != null
                                && existingLink.getServer() == link.getServer()) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    // Record the relation between the Operation and the Actor
                    linkedInstance.getLinks().add(link);

                    // For data operations, compute 'referenceLink' and 'otherLinks' attributes
                    if (link.getData() != null) {
                        if (link.getIsReference()) {
                            // If the reference is already set by a link that is not tagged as
                            // reference
                            // move it to its real place : otherLink
                            if (linkedInstance.getReferenceLink() != null) {
                                linkedInstance
                                        .getOtherLinks()
                                        .add(linkedInstance.getReferenceLink());
                            }
                            linkedInstance.setReferenceLink(link);
                        } else if (linkedInstance.getReferenceLink() == null) {
                            // If no reference exists, use it as reference as long a the real
                            // reference is not seen
                            linkedInstance.setReferenceLink(link);
                        } else {
                            linkedInstance.getOtherLinks().add(link);
                        }
                    }
                }
            }
        }

        // Specifically identify the Instance as unconcerned by all the Actions it does not already
        // appear in
        for (OperationsMapEntry opAndInstances : mapEntries) {
            LinkedInstance linkedInstance = findLinkedInstance(instance, opAndInstances);
            if (linkedInstance == null) {
                opAndInstances.getUnlinkedInstances().add(instance);
            }
        }
    }

    private LinkedInstance findLinkedInstance(
         Instance instance, OperationsMapEntry opAndInstances) {
        for (LinkedInstance i : opAndInstances.getLinkedInstances()) {
            if (i.getInstance() == instance) {
                return i;
            }
        }
        return null;
    }

    private LinkedInstanceData findLinkedInstanceData(Instance instance, DataConnection entry) {
        assert entry != null;
        for (LinkedInstanceData i : entry.getLinkedInstances()) {
            if (i.getPort().getInstance() == instance) {
                return i;
            }
        }
        return null;
    }

    private void fillDataConnections(Collection<DataConnection> mapEntries, Instance instance)
            throws IOException {

        LinkedHashMap<String, DataConnection> map = new LinkedHashMap<>();
        for (DataConnection e : mapEntries) {
            map.put(e.getOperation().getName(), e);
        }

        // For each Operation in which the Instance is involved
        for (DataLink link : context.system.getAssembly().getDataLinks()) {

            for (DataLinkElement le : link.getReaders()) {

                PortData port = le.getPort();
                if (port.getInstance() == instance) {
                    // Get the Action
                    DataConnection entry = map.get(port.getOperation().getName());

                    // Get the Actor corresponding to the given Instance
                    LinkedInstanceData linkedInstance = findLinkedInstanceData(instance, entry);

                    if (linkedInstance == null) {
                        linkedInstance = new QLinkedInstanceData();
                        linkedInstance.setPort(port);
                        entry.getLinkedInstances().add(linkedInstance);
                    }

                    // Record the relation between the Operation and the Actor
                    if (link == port.getReference()) {
                        linkedInstance.setReferenceLink(link);
                    } else {
                        linkedInstance.getOtherLinks().add(link);
                    }
                }
            }
        }
        // // Specifically identify the Instance as unconcerned by all the Actions it does not
        // already appear in
        // for (OperationsMapEntry opAndInstances : mapEntries) {
        // LinkedInstance linkedInstance = findLinkedInstance(instance, opAndInstances);
        // if (linkedInstance == null) {
        // opAndInstances.getUnlinkedInstances().add(instance);
        // }
        // }
    }

    /** Compute LinkedInstance.notificationReqIds and .notifiedThreads */
    protected void computeNotificationInfo(Executable exec) {
        for (Container container : exec.getContainers()) {
            for (OperationsMapEntry entry : container.getOperationMap().getWrittenData()) {
                for (LinkedInstance li : entry.getLinkedInstances()) {
                    for (OperationLink datalink : li.getLinks()) {
                        addNotificationInfo(datalink.getDataLink(), li);
                    }
                }
            }
        }
    }

    /**
     * Compute the set of reqIds, and threads to be notified for this datalink.
     * @param datalink the datalink to consider
     * @param li where to put the results
     */
    private void addNotificationInfo(DataLink dataLink, LinkedInstance li) {
        for (EventLink link : context.system.getAssembly().getEventLinks()) {
            if (link.getNotifiedData() == dataLink) {
                for (EventLinkReceiver receiver : link.getReceivers()) {
                    Thread thread = receiver.getPort().getInstance().getThread();
                    if (thread != null) { // if the destination instance is a real instance and not external
                        li.getNotificationReqIds().add(link.getId());
                        
                        QNotificationLink notification = new QNotificationLink();
                        notification.setNotificationReqId(link.getId());
                        notification.setNotifiedThread(thread);
                        li.getNotificationLinks().add(notification);
                    }
                }
            }
        }
    }
    
    void computeObjectCounts() {

        Mapping mapping = context.system.getMapping();

        long maxThreadId = 0;
        for (Platform platform : mapping.getPlatforms()) {
            for (Executable exec : platform.getExecutables()) {
                // first, get the max from all threads
                for (Thread t : exec.getThreads()) {
                    maxThreadId = Math.max(maxThreadId, (long) t.getIdNo());
                }
                // take into account the additional threads created for 'external' components
                for (Instance i : exec.getInstances()) {
                    ThreadBase t = i.getExternalThread();
                    if (t != null) {
                        maxThreadId = Math.max(maxThreadId, (long) t.getIdNo());
                    }
                }
            }
        }
        mapping.setMaxThreadId(maxThreadId);

        mapping.setNbInstances(context.system.getAssembly().getInstances().size());

        for (Platform platform : mapping.getPlatforms()) {
            for (Executable exec : platform.getExecutables()) {

                mapping.setNbExecutables(mapping.getNbExecutables() + 1);

                // isDispatcher
                exec.setIsDispatcher(exec == exec.getParent().getDispatcher());

                for (Thread thread : exec.getThreads()) {
                    for (Instance instance : thread.getInstances()) {
                        // hasSupervisor
                        if (instance.getType().getIsSupervisor()) {
                            thread.setHasSupervisor(true);
                        }
                        // hasAsyncTimeout
                        for (EntryPoint ep : instance.getCallbackEntryPoints()) {
                            if (ep.getRequestResponse().getIsTimed()) {
                                instance.setHasAsyncTimeout(true);
                                thread.setHasAsyncTimeout(true);
                            }
                        }
                        
                        for(OperationLink requiredRequestResponseLink : instance.getRequiredRequestResponsesLinks()) {
                        	// hasSyncRequireRequestResponse (client)
                        	if(!requiredRequestResponseLink.getRequestResponse().getIsAsynchronous()) {
                        		thread.setHasSyncRequiredRequestResponses(true);
                        	}
                        	
                        	// hasASyncRequireRequestResponse (client and timeout)
                        	if(requiredRequestResponseLink.getRequestResponse().getIsTimed() && 
                        			requiredRequestResponseLink.getRequestResponse().getIsAsynchronous()) {
                        		thread.setHasAsyncRequireRequestResponses(true);
                        	}
                        }
                    }	
                }
            }
        }
    }

}