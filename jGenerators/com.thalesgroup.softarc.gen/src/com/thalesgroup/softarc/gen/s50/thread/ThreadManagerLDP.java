/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.IdAllocator;
import com.thalesgroup.softarc.gen.s50.thread.sizing.RequestQueueSizer;
import com.thalesgroup.softarc.gen.s50.thread.sizing.VrSetSizer;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.DataVersion;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Executable;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.Mapping;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Platform;
import com.thalesgroup.softarc.sf.Request;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.Thread;
import com.thalesgroup.softarc.sf.ThreadBase;
import com.thalesgroup.softarc.sf.Trigger;
import com.thalesgroup.softarc.sf.TriggerInstance;
import com.thalesgroup.softarc.sf.impl.QDataVersion;
import com.thalesgroup.softarc.sf.impl.QExecutable;
import com.thalesgroup.softarc.sf.impl.QOsProperties;
import com.thalesgroup.softarc.sf.impl.QPlatform;
import com.thalesgroup.softarc.sf.impl.QThread;
import com.thalesgroup.softarc.sf.impl.QThreadBase;
import com.thalesgroup.softarc.sf.impl.QTriggerInstance;

import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.deployment.DEDeployedInstance;
import technology.ecoa.model.deployment.DEExecutable;
import technology.ecoa.model.deployment.DETask;
import technology.ecoa.model.deployment.Endianness;

public class ThreadManagerLDP {

    // Générateur de référence
    private final AbstractPass generator;

    // Modèle DEPLOYMENT
    private final DEApplication deployment;

    // Modèle MAPPING en construction
    public final Mapping mapping;

    // Dimensionnement des opérations
    private final Map<Long, OperationContext> operationsContexts;

    // Compteur de thread (unique dans l'application)
    private IdAllocator threadNumber = new IdAllocator("Thread", 0);

    // Compteur de plateforme (unique dans le système)
    private IdAllocator platformsNumber = new IdAllocator("Platform", 0);

    // Compteur d'exécutable (unique dans le système)
    private int executablesNumber = 0;

    // Compteur d'instance de composant (unique dans le système)
    private int deployedInstancesNumber = 0;

    // Operation manager utility
    private final OperationManager operationManager;

    private Assembly assembly;

    public ThreadManagerLDP(
            AbstractPass generator,
            Map<Long, OperationContext> operationsContexts,
            OperationManager operationManager) {

        this.generator = generator;

        this.deployment = generator.context.DEFILE;
        this.assembly = generator.context.system.getAssembly();

        this.mapping = generator.context.system.getMapping();

        this.operationsContexts = operationsContexts;

        this.operationManager = operationManager;
    }

    private void copyPlatformData(Platform dst, DEApplication src) throws IOException {
        dst.setName(src.getName());
    }

    void initializeMapping() throws IOException {

        checkExternalOperations();

        // Création de la structure du Mapping par un parcours en profondeur
        // du Deployment

        /* Attribute 'production' is ignored for the LDP */
        deployment.setProduction(null);

        // Détection du caractère supervisé du système
        mapping.setIsSupervisedSystem(true);

        standardThreadAnalysis(deployment);

        // Compute per instance buffer size. This operation needs to know
        // the generated system topography.
        operationManager.computeInstancesBufferSizes(assembly);

        // Add Endianness fed to DE
        mapping.setIsLittleEndian(deployment.getEndianness() == Endianness.LITTLE);
    }

    private void standardThreadAnalysis(DEApplication platform)
            throws IOException {

        // Create an identical platform in the Thread Mapping, with same
        // attributes and attach the platform definition to the mapping
        Platform mappingPlatform = new QPlatform();
        mapping.getPlatforms().add(mappingPlatform);

        copyPlatformData(mappingPlatform, platform);
        copyOsProperties(mappingPlatform);

        mappingPlatform.setTraceOnStderr(true);
        mappingPlatform.setDisplayOnStdout(true);

        mappingPlatform.setXmlID("platform:" + mappingPlatform.getName());
        mappingPlatform.setIdNo(platformsNumber.allocate());
        mappingPlatform.setNeedsByteSwapping(
                !mappingPlatform
                        .getOsProperties()
                        .getEndianness()
                        .equals(deployment.getEndianness().name()));

        // Création de la structure de chaque binaire (exécutable et
        // aiguilleur)
        for (DEExecutable executable : getBinaries(platform)) {

            if (true) {
                Executable mappingExecutable = new QExecutable();
                mappingPlatform.getExecutables().add(mappingExecutable);
                mapping.getExecutables().add(mappingExecutable);
                mappingExecutable.setParent(mappingPlatform);

                // attribution d'un identifiant
                mappingExecutable.setIdNo(executablesNumber);
                executablesNumber += 1;

                // récupération du nom original
                mappingExecutable.setName(executable.getName());

                if (executable == platform) {
                    mappingExecutable.setIsDispatcher(true);
                    mappingPlatform.setDispatcher(mappingExecutable);
                }

                // ajout des threads MW
                if (mappingPlatform.getOsProperties().getSupervision()) {
                    if (executable == platform) {
                        createMwThread("MASTER", mappingExecutable);
                        createMwThread("RECEIVE", mappingExecutable);
                        // Supervision of child processes
                        createMwThread("WATCHDOG", mappingExecutable);
                    }
                }

                /* Creates auto-start task if necessary (ie. the exec has deployed instances) */
                if (mapping.getAutoStart() && !executable.getTask().isEmpty()) {
                    createMwThread("AUTO_START", mappingExecutable);
                }

                // Découpage en tâches
                for (DETask task : executable.getTask()) {

                    Thread thread = createThread(mappingExecutable, task);

                    for (DEDeployedInstance deployedInstance : task.getDeployedInstance()) {
                        Instance instance = getInstanceByName(deployedInstance.getRef());

                        // Insertion de l'instance dans une tâche
                        attachDeployedInstance(deployedInstance, instance, mappingExecutable, thread);

                        // Création éventuelle d'une tâche annexe pour un composant EXTERNAL
                        String prefix = null;
                        long stack_size = 0;
                        boolean is_prompt = true;
                        
                        if (instance.getType().getIsExternal()) {
                            prefix = "E_";
                            is_prompt = instance.getType().getAutoStartExternalThread();
                        }

                        if (prefix != null) {
                            ThreadBase other_thread = createMwThread(prefix + instance.getName(), mappingExecutable);
                            other_thread.setIsExternalThread(true);
                            other_thread.setIsPrompt(is_prompt);
                            other_thread.setStack(stack_size);
                        }
                    }
                }

                boolean hasAlarms = false;
                // Numérotation de tous les triggers de l'exécutable
                long triggerId = 0;
                for (Thread thread : mappingExecutable.getThreads()) {
                    for (Instance instance : thread.getInstances()) {
                        for (TriggerInstance trig : instance.getTriggers()) {
                            // Allouer un identifiant unique dans l'exécutable
                            trig.setId(triggerId);
                            triggerId++;
                            hasAlarms = true;
                        }
                    }
                }

                for (Thread thread : mappingExecutable.getThreads()) {
                    for (Instance instance : thread.getInstances()) {
                        for (OperationRequestResponse rs : getInstanceByName(instance.getName()).getType().getRequiredRequestResponses()) {
                            if (rs.getIsAsynchronous() && rs.getIsTimed()) {
                                hasAlarms = true;
                            }
                        }
                    }
                }

                // Création de la tâche dédiée TALARM si nécessaire
                if (hasAlarms) {
                    createMwThread("ALARM", mappingExecutable);
                }

                // Récupération des identifiants des tâches externes
                for (Thread thread : mappingExecutable.getThreads()) {
                    for (Instance instance : thread.getInstances()) {
                        // si componentType EXTERNAL, relier l'instance au thread supplémentaire
                        if (instance.getType().getIsExternal()) {

                            String searched_name = "E_" + instance.getName();

                            for (ThreadBase mw_thread : mappingExecutable.getMwThreads()) {
                                if (mw_thread.getName().equals(searched_name)) {
                                    instance.setExternalThread(mw_thread);
                                    break;
                                }
                            }
                            assert instance.getExternalThread() != null;
                        }
                    }
                }
            }
        }
    }

    private void copyOsProperties(Platform platform) throws IOException {
        QOsProperties osp = new QOsProperties();
        osp.setEndianness("LITTLE");
        osp.setIsBigEndian(false);
        osp.setSupportShm(true);
        osp.setHasLibc(true);
        osp.setIsWindows(false);
        osp.setIsListCompliant(false);
        osp.setIsStp(false);
        osp.setSupervision(true);
        osp.setSimulation(false);
        platform.setOsProperties(osp);
    }

    private ThreadBase createMwThread(String name, Executable executable) throws IOException {
        QThreadBase mw_thread = new QThreadBase();
        mw_thread.setName(checkThreadName(name));
        mw_thread.setXmlID("thread:" + executable.getName() + '/' + mw_thread.getName());
        mw_thread.setIdNo(this.threadNumber.allocate());
        executable.getMwThreads().add(mw_thread);
        return mw_thread;
    }

    private Instance getInstanceByName(String ref) throws IOException {
        for (Instance i : assembly.getInstancesIncludingExtern()) {
            if (i.getName().equals(ref)) {
                return i;
            }
        }
        generator.errorModel(
                "Instance '%s' is present in deployment, but not found in assembly", ref);
        return null;
    }

    /**
     * Context of the operation identified by link id, only if one of the link elements in the list
     * refer to an instance called instanceName
     *
     * @param linkId identifier of the operation which context is returned
     * @param collection list of link elements
     * @param instanceName name of the instance
     * @return linkedInstanceOperation context of the operation identified by linkId, or null
     */
    private OperationContext getLinkedInstanceOperationRequestResponse(long linkId, Collection<RequestResponseLinkReceiver> collection,
            Instance instance) {

        for (RequestResponseLinkReceiver linkElement : collection) {
            if (linkElement.getPort().getInstance() == instance) {
                return operationsContexts.get(linkId);
            }
        }
        return null;
    }

    private OperationContext getLinkedInstanceOperationData(
            long linkId, Collection<DataLinkElement> collection, Instance instance) {

        for (DataLinkElement linkElement : collection) {
            if (linkElement.getPort().getInstance() == instance) {
                return operationsContexts.get(linkId);
            }
        }
        return null;
    }

    private void computeActiveOnThread(
            Instance instance, RequestQueueSizer request_sizer, RequestQueueSizer reply_sizer)
            throws IOException {
        OperationContext oper;
        Long fifosize = null;
        long nb_republish = 0;

        // RequestResponses

        for (RequestResponseLink serviceLink : assembly.getRequestResponseLinks()) {

            LinkedList<RequestResponseLinkReceiver> serviceLinkClientsList = new LinkedList<RequestResponseLinkReceiver>();
            serviceLinkClientsList.add(serviceLink.getClient());

            // Called services

            oper = getLinkedInstanceOperationRequestResponse(serviceLink.getId(), serviceLinkClientsList, instance);
            if (oper != null) {
                fifosize = serviceLink.getClient().getFifoSize();

                if (oper.async) {
                    request_sizer.add_request(
                            serviceLink.getId() + 1, oper.out.raw_size, (long) fifosize);
                } else {
                    if (reply_sizer != null)
                        reply_sizer.add_request(
                                serviceLink.getId() + 1, oper.out.raw_size, (long) fifosize);
                }
            }

            // Provided services

            for (RequestResponseLinkReceiver server : serviceLink.getServers()) {
                if (server.getPort().getInstance() == instance) {
                    oper = operationsContexts.get(serviceLink.getId());
                    if (oper != null) {
                        fifosize = server.getFifoSize();
                        request_sizer.add_request(
                                serviceLink.getId(), oper.in.raw_size, (long) fifosize);
                    }
                }
            }
        }

        // Events

        for (EventLink eventLink : assembly.getEventLinks()) {

            for (EventLinkReceiver receiver : eventLink.getReceivers()) {
                if (receiver.getPort().getInstance() == instance) {
                    oper = operationsContexts.get(eventLink.getId());
                    if (oper != null) {
                        fifosize = receiver.getFifoSize();
                        request_sizer.add_request(
                                eventLink.getId(), oper.in.raw_size, (long) fifosize);
                    }
                }
            }
        }

        // Republished data

        for (DataLink dataLink : assembly.getDataLinks()) {

            oper =
                    getLinkedInstanceOperationData(
                            dataLink.getId(), dataLink.getWriters(), instance);
            if (oper != null) {
                nb_republish += dataLink.getReaders().size();
            }
        }

        if (nb_republish > 0) {
            instance.setHandleRepublishRequests(true);
            instance.getThread().setHandleRepublishRequests(true);
            request_sizer.add_request(15, 4, nb_republish);
        }
    }

    // Complète le Mapping sur la base de sa propre description du système

    void finalizeMapping() throws IOException {

        for (Platform platform : mapping.getPlatforms()) {

            VrSetSizer request_vrs_sizer =
                    new VrSetSizer(
                            "VRs for parameters of requests of platform " + platform.getName());

            RequestQueueSizer request_sizer = new RequestQueueSizer(request_vrs_sizer);
            RequestQueueSizer reply_sizer = new RequestQueueSizer(request_vrs_sizer);

            /*
             * do for every executable of the platform of the mapping
             */
            for (Executable executable : platform.getExecutables()) {
                /*
                 * do for every thread of the executable of the platform of the mapping
                 */
                for (Thread thread : executable.getThreads()) {
                    long maxRequestResponseOut = 0;
                    // number of synchronous callers
                    long nbRequestResponseOut = 0;
                    long maxBufferOutSize = 0;

                    request_sizer.init();
                    reply_sizer.init();

                    /*
                     * do for every deployed instance of the thread of the executable of the platform of the mapping
                     */
                    for (Instance instance : thread.getInstances()) {
                        RequestQueueSizer instance_queue_sizer = new RequestQueueSizer(null);

                        /*
                         * Update task and instance queue sizes
                         */
                        computeActiveOnThread(instance, request_sizer, reply_sizer);
                        computeActiveOnThread(instance, instance_queue_sizer, null);

                        /*
                         * Compute thread Consume service Out
                         */
                        for (RequestResponseLink serviceLink : assembly.getRequestResponseLinks()) {
                            for (RequestResponseLinkReceiver server : serviceLink.getServers()) {
                                if (server.getPort().getInstance() == instance) {
                                    OperationContext oper =
                                            operationsContexts.get(serviceLink.getId());

                                    maxBufferOutSize =
                                            Math.max(maxBufferOutSize, oper.out.raw_size);
                                }
                            }

                            if (serviceLink.getClient().getPort().getInstance() == instance) {
                                OperationContext oper = operationsContexts.get(serviceLink.getId());

                                if (oper == null) {
                                    generator.errorModel(
                                            "operation %s not found in operationsContexts !!",
                                            serviceLink.getId());
                                } else {
                                    /*
                                     * if the operation is synchronous
                                     */
                                    if (!oper.async) {
                                        nbRequestResponseOut++;
                                        /*
                                         * look for the max size among all operations output parameters sizes
                                         */
                                        maxRequestResponseOut = Math.max(maxRequestResponseOut, oper.out.raw_size);
                                    }
                                }
                            }
                        }

                        // Check global sizing

                        instance.setQueueSize(instance_queue_sizer.get_size());
                        instance.getRequests().addAll(instance_queue_sizer.get_constraints());
                    }

                    thread.getReplies().addAll(reply_sizer.get_constraints());

                    if (maxBufferOutSize > 0) {
                        thread.setMaxBufferOutSize(maxBufferOutSize);
                        thread.setHasBufferOut(true);
                    }

                    if (nbRequestResponseOut != 0) {
                        thread.setOutsizemax(maxRequestResponseOut);

                        long shmout = reply_sizer.get_size();
                        if (shmout > 0) {
                            thread.setShmoutglobalsize(shmout);
                            thread.setHasShmout(true);
                        }
                    }
                }

                computeSocketOutBuffer(executable);
            }

            // generate DataVersions for requests (events and services)
            for (Request request : request_vrs_sizer.getRepositories()) {

                DataVersion dataversion = new QDataVersion();
                platform.getDataVersions().add(dataversion);

                dataversion.setId(request.getId());
                dataversion.setXmlID("dataversion:" + platform.getName() + '/' + request.getId());
                dataversion.setNumberofversions(request.getCapacity());
                dataversion.setSizeof(request.getParameterSize());
                dataversion.setSize(request.getParameterSize());
                dataversion.setRequestsize(request.getParameterSize());
                dataversion.setDataLink(null); // this DataVersion has no DataLink
            }

            // generate Data Sharing Parameters for every data link
            //
            for (DataLink dataLink : assembly.getDataLinks()) {
                int count = 0;

                // lecteurs
                for (DataLinkElement dataReader : dataLink.getReaders()) {
                    if (true) {
                        // add NbMaxVersion version of data per reader
                        count += dataReader.getPort().getData().getMaxversions();
                    }
                }

                // version de référence
                count += 1;

                // valeur par défaut
                if (!dataLink.getDefaultValue().isEmpty()) {
                    count += 1;
                }

                // écrivains
                for (DataLinkElement dataWriter : dataLink.getWriters()) {
                    // add versions of the data: nbMax consumed + 1 written, in case group is
					// multi-site
					count += dataWriter.getPort().getData().getMaxversions();
                }

                if (count != 0) {
                    OperationContext oper = operationsContexts.get(dataLink.getId());

                    if (oper == null) {
                        generator.errorModel("operation %s not found in operationsContexts !!", dataLink.getId());
                        return; // to avoid Java warning
                    }
                    if (dataLink.getDirect()) {
                        // Direct dataLink is explicitely managed with a single version.
                        count = 1;
                    }

                    DataVersion dataversion = new QDataVersion();
                    platform.getDataVersions().add(dataversion);

                    dataversion.setId(dataLink.getId());
                    dataversion.setXmlID(
                            "dataversion:" + platform.getName() + '/' + dataLink.getId());
                    dataversion.setNumberofversions(count);

                    dataversion.setSizeof(oper.data.sizeof);
                    dataversion.setSize(oper.data.raw_size);
                    dataversion.setRequestsize(oper.in.raw_size);
                    dataversion.setDataLink(dataLink);

                }
            }
        }
    }

    boolean checkSizingOverflow() {

        boolean sizingOverflow = false;

        return sizingOverflow;
    }

    // Ensemble des binaires (exécutables et aiguilleurs) d'une plateforme
    // donnée, sans se reposer sur la notion de groupe

    private List<DEExecutable> getBinaries(DEApplication platform) {
        List<DEExecutable> result = new ArrayList<DEExecutable>();

        result.add(platform);
        result.addAll(platform.getExecutable());

        return result;
    }

    // Attache au Mapping une instance de composant

    private void attachDeployedInstance(DEDeployedInstance deployedInstance, Instance instance, Executable binary, Thread thread)
            throws IOException {

        for (Trigger trig : instance.getType().getTriggers()) {

            // Attache a l'instance, les triggers déclarés...
            TriggerInstance maTrig = new QTriggerInstance();
            // ...en leur attribuant le nom qu'ils avaient lors de leur
            // declaration dans le componenttype de l'instance
            maTrig.setName(trig.getName());
            // ...et en leur attribuant un nom de requête correspondant
            // a l'id du lien virtuel créé dans le TAS pour linker l'événement virtuel
            // émis à l'événement de réception de triggerBack
            boolean found = false;

            for (EventLink eventLink : assembly.getEventLinks()) {
                for (EventLinkReceiver asler : eventLink.getReceivers()) {
                    for (EventLinkSender asles : eventLink.getSenders()) {
                        if ((asler.getPort().getOperation() == trig.getEvent())
                                && (asler.getPort().getInstance() == instance)
                                && (asles.getPort()
                                        .getOperation()
                                        .getName()
                                        .equals("sarc_trigger_" + trig.getName()))) {
                            found = true;
                            maTrig.setRequestId(eventLink.getId());
                        }
                        if (found) break;
                    }
                    if (found) break;
                }
                if (found) break;
            }
            // Attache a l'instance
            instance.getTriggers().add(maTrig);
        }

        if (true) {
            // Intégration de l'instance dans une tâche

            // attribution d'un identifiant
            instance.setIdNo(deployedInstancesNumber);
            deployedInstancesNumber++;

            // rattachement de l'instance de composant au thread
            thread.getInstances().add(instance);
            instance.setThread(thread);
            instance.setExec(binary);
            binary.getInstances().add(instance);
        }
    }

    private Thread createThread(Executable binary, DETask task) throws IOException {
        Thread thread = new QThread();

        // attribution d'un identifiant
        thread.setIdNo(this.threadNumber.allocate());

        // démarrage auto des composants?
        thread.setIsPrompt(mapping.getAutoStart());
        // copie du nom spécifié
        thread.setName(checkThreadName(task.getName()));
        thread.setXmlID("thread:" + binary.getName() + '/' + thread.getName());

        // rattachement à l'exécutable dont il dépend
        binary.getThreads().add(thread);

        return thread;
    }
    

    private void computeSocketOutBuffer(Executable exec) {
    	long bufferSize = 0;

    	for (EventLink eventLink : assembly.getEventLinks()) {
			OperationContext oper = operationsContexts.get(eventLink.getId());
    		for(EventLinkReceiver receiver : eventLink.getReceivers()) {
    			bufferSize += oper.in.raw_size * receiver.getFifoSize();
    		}
    	}
    	
    	for (DataLink dataLink : assembly.getDataLinks()) {
    		for(DataLinkElement receiver : dataLink.getReaders()) {
    			OperationContext oper = operationsContexts.get(dataLink.getId());
    			bufferSize += oper.data.raw_size * dataLink.getThreadsLinked().size();
    		}
    	}

    	for (RequestResponseLink requestResponse : assembly.getRequestResponseLinks()) {
    		OperationContext oper = operationsContexts.get(requestResponse.getId());
    		for(RequestResponseLinkReceiver server : requestResponse.getServers()) {
    			bufferSize += oper.in.raw_size * server.getFifoSize();
    		}
    		bufferSize += oper.out.raw_size * requestResponse.getClient().getFifoSize();
    	}

    	exec.setMaxBufferOut(bufferSize);
    }
    

    private String checkThreadName(String name) {
        return name;
    }

    //


    private void checkExternalOperations() throws IOException {
        if (deployment.getExternalIo() != null) {
            generator.errorModel("External operations are not supported; section <external_io> in deployment will be ignored");
        }
    }
}
