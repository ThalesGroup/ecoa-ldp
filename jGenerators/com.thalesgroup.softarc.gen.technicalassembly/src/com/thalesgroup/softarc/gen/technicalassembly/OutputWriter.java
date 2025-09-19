/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.thalesgroup.ecoa.model.ComponentType;
import com.thalesgroup.ecoa.model.Instance;

import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTTrigger;
import technology.ecoa.model.assembly.ASAssembly;
import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASIdentifiedMemberValue;
import technology.ecoa.model.assembly.ASInstance;
import technology.ecoa.model.assembly.ASOpRef;
import technology.ecoa.model.assembly.ASOpRefClient;
import technology.ecoa.model.assembly.ASOpRefRead;
import technology.ecoa.model.assembly.ASOpRefReceive;
import technology.ecoa.model.assembly.ASOpRefSend;
import technology.ecoa.model.assembly.ASOpRefServer;
import technology.ecoa.model.assembly.ASOpRefWrite;
import technology.ecoa.model.assembly.ASMemberValue;
import technology.ecoa.model.assembly.ASOperationLink;
import technology.ecoa.model.assembly.ASRequestResponseLink;
import technology.ecoa.model.assembly.ASWhenCondition;
import technology.ecoa.model.assembly.ObjectFactory;

/**
 * This generators transforms an Assembly to a Technical Assembly, i.e an assembly: - without the concept of composite. - where
 * the operation links (datalink, eventlink and service) have a unique numeric identifier
 */
public final class OutputWriter {

    private ObjectFactory factory = new ObjectFactory();

    private RootInstance rootInstance;
    private ASAssembly technicalAssembly;
    private LinkIdServer idServer = new LinkIdServer();
    List<InstanceAdapter> allInstancesWithExtern;

    /**
     * Create the component instances of the technical assembly.
     * 
     * @param technicalAssembly The technical assembly.
     */
    private void createInstances() {
        long next_id = 0;

        for (ComponentInstance i : rootInstance._instances) {
            // if (i.fullName == Instance.SPECIAL_INSTANCE_NAME_EXTERN) continue;
            ASInstance instance = factory.createASInstance();

            instance.setImplementation(i.model.getInstance().getImplementation());
            instance.setName(i.getFullName());
            instance.setComponentType(i.model.getComponentType().getName());

            for (String attribute : i.attributes.keySet()) {
                ASMemberValue att = factory.createASMemberValue();
                att.setName(attribute);
                att.setValue(i.attributes.get(attribute));

                instance.getPropertyValue().add(att);
            }

            for (String pinfo : i.pinfos.keySet()) {
                ASMemberValue att = factory.createASMemberValue();
                att.setName(pinfo);
                att.setValue(i.pinfos.get(pinfo));

                instance.getPinfoValue().add(att);
            }

            for (String variable : i.variables.keySet()) {
                ASIdentifiedMemberValue var = factory.createASIdentifiedMemberValue();
                var.setName(variable);
                var.setValue(i.variables.get(variable));
                var.setId(next_id);
                next_id++;

                instance.getVariableInit().add(var);
            }

            technicalAssembly.getInstance().add(instance);
        }
    }

    private void addConditions(WhenSet when, ASOpRef to) {
        for (Condition c : when.whencondition) {
            ASWhenCondition w = factory.createASWhenCondition();
            w.setVariable(c.variable.variableName);
            w.setValue(c.model.getValue());
            w.setInstance(c.variable.instance.fullName);
            to.getWhen().add(w);
        }
    }

    /**
     * An InfoSource represents one consumer operation, and n wires connecting several producer operations to this consumer.
     * 
     * An InfoSource has a "signature", which is computed from the (unordered) set of producers, with their conditions if any, and
     * the (optional) default value.
     */
    private class InfoSource {
        Operation consumer;
        ArrayList<Wire> producers = new ArrayList<Wire>();
        String signature;
        boolean done = false;

        void computeSignature() {
            ArrayList<String> writersSignatures = new ArrayList<String>(producers.size());
            for (Wire w : producers) {
                writersSignatures.add(w.toSourceString());
            }
            Collections.sort(writersSignatures);
            signature = writersSignatures.toString();
            if (consumer.defaultvalue != null) {
                signature += "defaultvalue=";
                signature += consumer.defaultvalue.getValue();
            }
        }

        public boolean isEmpty() {
            return producers.isEmpty() && consumer.defaultvalue == null;
        }
    }

    /**
     * Create a ASDataLink from a OperationLink.
     * 
     * @param e A OperationLink
     */
    private void createASDataLinks() {

        // build the list of InfoSources
        ArrayList<InfoSource> sources = new ArrayList<InfoSource>();
        for (ComponentInstance i : allInstancesWithExtern) {
            for (Operation op : i.operations.values()) {
                if (op.type == EDR.Data) {
                    InfoSource sd = new InfoSource();
                    sd.consumer = op;
                    for (Wire w : rootInstance.wires) {
                        if (w.target == op) {
                            sd.producers.add(w);
                        }
                    }
                    sd.computeSignature();
                    if (!sd.isEmpty()) {
                        sources.add(sd);
                    }
                    // System.out.printf("new InfoSource for reader %s: %s\n", sd.reader, sd.signature);
                }
            }
        }
        for (InfoSource sd : sources) {
            if (!sd.done) {
                ASDataLink datalink = factory.createASDataLink();
                // find all readers which have equivalent InfoSources
                LinkedHashSet<Operation> readers = new LinkedHashSet<Operation>();
                for (InfoSource sd2 : sources) {
                    if (sd2.signature.equals(sd.signature)) {
                        readers.add(sd2.consumer);
                        sd2.done = true;
                    }
                }
                // add "reader" elements
                for (Operation op : readers) {
                    ASOpRefRead element = factory.createASOpRefRead();
                    element.setInstance(op.instance.fullName);
                    element.setOperation(op.name);
                    WhenSet conditions = new WhenSet();
                    for (Wire w : rootInstance.wires) {
                        if (w.target == op) {
                            conditions.addAll(w.targetWhen);
                        }
                    }
                    addConditions(conditions, element);
                    datalink.getReader().add(element);
                }
                // add "writer" elements
                for (Wire w : sd.producers) {
                    Operation op = (Operation) w.source;
                    ASOpRefWrite element = factory.createASOpRefWrite();
                    element.setInstance(op.instance.fullName);
                    element.setOperation(op.name);
                    // REQ: recompute "reference" attribute after link merging:
                    // a WRITE is reference if there is also a READ for the same operation in the
                    // same datalink.
                    element.setReference(readers.contains(op));
                    addConditions(w.sourceWhen, element);
                    datalink.getWriter().add(element);
                }
                datalink.setDefaultValue(sd.consumer.defaultvalue);
                // compute "direct" attribute
                datalink.setUncontrolledAccess(true);
                for (Operation op : readers) {
                    for (Wire w : rootInstance.wires) {
                        if (w.target == op) {
                            if (!w.direct)
                                datalink.setUncontrolledAccess(false);
                        }
                    }
                }

                // REQ-007
                allocateId(datalink);

                // System.out.printf("new datalink: %s -> %d readers\n", sd.signature, readers.size());
                // System.out.printf("new datalink: %s -> %s, id=%s\n", sd.signature, readers, datalink.getId().toString());
                technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().add(datalink);
            }
        }
        // for (Wire w : rootInstance.wires) {
        // if (w.kind == Kind.DATA && w.fixedId == null) {
        // System.out.printf("No id for data wire: %s\n", w.toString());
        // }
        // }
        // create data links for WRITE op with no READ
        // REQ-006
        LinkedHashSet<Operation> writers = new LinkedHashSet<Operation>();
        for (ComponentInstance i : rootInstance._instances) { // excluding instance 'extern'
            for (Operation op : i.operations.values()) {
                if (i.componentType.getWrittenDataOperation(op.name) != null) {
                    writers.add((Operation) op);
                }
            }
        }
        for (ComponentInstance i : rootInstance._instances) {
            for (Operation op : i.operations.values()) {
                for (Wire w : rootInstance.wires) {
                    if (w.source == op) {
                        writers.remove((Operation) op);
                    }
                }
            }
        }
        for (Operation op : writers) {
            ASDataLink datalink = factory.createASDataLink();
            ASOpRefWrite w = factory.createASOpRefWrite();
            w.setInstance(op.instance.fullName);
            w.setOperation(op.name);
            if (op.writeOnly) {
                w.setReference(false);
            } else {
                w.setReference(true);
                ASOpRefRead r = factory.createASOpRefRead();
                r.setInstance(op.instance.fullName);
                r.setOperation(op.name);
                datalink.getReader().add(r);
            }
            datalink.getWriter().add(w);
            // REQ-007
            allocateId(datalink);
            technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().add(datalink);
        }
    }

    /**
     * Create a ASRequestResponseLink from a OperationLink.
     * 
     * @param e A OperationLink
     * @return A ASRequestResponseLink
     */
    private void createASRequestResponseLinks() {
        for (ComponentInstance i : allInstancesWithExtern) {
            for (Operation op : i.operations.values()) {
                if (op.type == EDR.RequestResponse) {
                    if (i != rootInstance) {
                        // Non-external operations: create 1 servicelink per client
                        ASRequestResponseLink servicelink = null;
                        for (Wire w : rootInstance.wires) {
                            if (w.source == op) {
                                if (servicelink == null) {
                                    servicelink = factory.createASRequestResponseLink();
                                    ASOpRefClient element = factory.createASOpRefClient();
                                    element.setInstance(w.source.instance.fullName);
                                    element.setOperation(w.source.name);
                                    element.setCallbackActivating(w.callbackActivating);
                                    element.setFifoSize(w.sourceFifoSize);
                                    servicelink.setClient(element);
                                }
                                ASOpRefServer element = factory.createASOpRefServer();
                                element.setInstance(w.target.instance.fullName);
                                element.setOperation(w.target.name);
                                element.setActivating(w.activating);
                                element.setFifoSize(w.targetFifoSize);
                                addConditions(w.targetWhen, element);
                                servicelink.getServer().add(element);
                            }
                        }
                        if (servicelink != null) {
                            allocateId(servicelink);
                            technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().add(servicelink);
                        }
                    } else {
                        // External service operations: create 1 servicelink per wire, with "in_channel" and "out_channel"
                        // attributes.
                        for (Wire w : rootInstance.wires) {
                            if (w.source == op) {
                                ASRequestResponseLink servicelink = factory.createASRequestResponseLink();
                                {
                                    ASOpRefClient element = factory.createASOpRefClient();
                                    element.setInstance(w.source.instance.fullName);
                                    element.setOperation(w.source.name);
                                    element.setInChannel(w.inChannel);
                                    element.setOutChannel(w.outChannel);
                                    servicelink.setClient(element);
                                }
                                {
                                    ASOpRefServer element = factory.createASOpRefServer();
                                    element.setInstance(w.target.instance.fullName);
                                    element.setOperation(w.target.name);
                                    element.setActivating(w.activating);
                                    element.setFifoSize(w.targetFifoSize);
                                    servicelink.getServer().add(element);
                                }
                                allocateId(servicelink);
                                technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().add(servicelink);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a ASEventLink from a OperationLink.
     * 
     * @param e A OperationLink
     * @return A ASEventLink
     */
    private void createASEventLinks() {

        ArrayList<InfoSource> sources = new ArrayList<InfoSource>();
        for (ComponentInstance i : allInstancesWithExtern) {
            for (Operation op : i.operations.values()) {
                if (op.type == EDR.Event) {

                    ArrayList<Wire> senders = new ArrayList<Wire>();
                    for (Wire w : rootInstance.wires) {
                        if (w.target == op) {
                            senders.add(w);
                        }
                    }

                    if (!senders.isEmpty()) {
                        InfoSource sd = new InfoSource();
                        sd.consumer = op;
                        sd.producers = senders;
                        sources.add(sd);
                        sd.computeSignature();
                        // System.out.printf("new event for receiver %s: %s\n", op, sd.signature);
                    }
                }
            }
        }
        // for each different SET found
        for (InfoSource sd : sources) {
            if (!sd.done) {
                ASEventLink link = factory.createASEventLink();

                LinkedHashSet<Operation> receivers = new LinkedHashSet<Operation>();
                LinkedHashSet<Wire> senders = new LinkedHashSet<Wire>();
                for (InfoSource sd2 : sources) {
                    if (sd2.signature.equals(sd.signature)) {
                        receivers.add(sd2.consumer);
                        senders.addAll(sd2.producers);
                        sd2.done = true;
                    }
                }
                for (Operation op : receivers) {
                    ASOpRefReceive element = factory.createASOpRefReceive();
                    element.setInstance(op.instance.fullName);
                    element.setOperation(op.name);
                    element.setActivating(sd.producers.get(0).activating);
                    long fifoSize = 0;
                    for (Wire sender : senders) {
                        if (sender.target.equals(op)) {
                            fifoSize = Math.max(fifoSize, sender.targetFifoSize);
                        }
                    }

                    element.setFifoSize(fifoSize);
                    WhenSet conditions = new WhenSet();
                    for (Wire w : senders) {
                        if (w.target == op) {
                            conditions.addAll(w.targetWhen);
                        }
                    }
                    addConditions(conditions, element);
                    link.getReceiver().add(element);
                }
                for (Wire w : sd.producers) {
                    Operation op = (Operation) w.source;
                    ASOpRefSend element = factory.createASOpRefSend();
                    element.setInstance(op.instance.fullName);
                    element.setOperation(op.name);
                    // add when elements on sender elements
                    addConditions(w.sourceWhen, element);
                    link.getSender().add(element);
                }
                // System.out.printf("new eventlink : %s -> %d receivers\n", sd.signature, readers.size());
                // REQ-007
                allocateId(link);
                technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().add(link);
            }
        }
    }

    /**
     * Create virtual link for each trigger
     */
    private void triggerLink(InstanceAdapter i, Collection<ASOperationLink> virtualLinks) {
        String prefix = "sarc_trigger";
        Map<String, CTTrigger> triggerList = i.componentType.getTriggers();

        if (triggerList != null) {
            for (Map.Entry<String, CTTrigger> entry : triggerList.entrySet()) {
                CTTrigger trig = entry.getValue();

                // Creation of a specific event link
                ASEventLink newEventLink = new ASEventLink();

                ASOpRefSend sender = new ASOpRefSend();
                sender.setInstance(i.fullName);
                sender.setOperation(prefix + '_' + trig.getName());

                // Verification of the existence of a corresponding received event has already
                // been done in Check_CTCI (gentype target)
                ASOpRefReceive receiver = new ASOpRefReceive();
                receiver.setInstance(i.fullName);
                receiver.setOperation(trig.getEvent());
                receiver.setActivating(true);
                receiver.setFifoSize(1L);

                newEventLink.getSender().add(sender);
                newEventLink.getReceiver().add(receiver);
                allocateId(newEventLink);
                virtualLinks.add(newEventLink);
            }
        }
    }

    /**
     * Split up a DataLink into as many EventLinks as there are readers which requested notification upon data update. These event
     * links are pushed back at the end of a 'virtual links' container.
     */
    private void notifyData(ASDataLink dataLink, Collection<ASOperationLink> virtualLinks) {
        final String prefix = "sarc_notify";

        ArrayList<ASOpRefSend> senders = new ArrayList<ASOpRefSend>();

        // Collect all potential writers
        for (ASOpRefWrite writerLe : dataLink.getWriter()) {
            ASOpRefSend sender = new ASOpRefSend();
            sender.setInstance(writerLe.getInstance());
            sender.setOperation(prefix);
            sender.getWhen().addAll(writerLe.getWhen());
            senders.add(sender);
        }

        if (senders.size() > 0) {
            // Inspect all readers which requested for notification
            for (ASOpRefRead reader : dataLink.getReader()) {

                if (!reader.getInstance().equals(Instance.SPECIAL_INSTANCE_NAME_EXTERN)) {
                    // Retrieve information concerning data notification
                    ComponentType component = rootInstance.getInstanceByFullname(reader.getInstance()).componentType;
                    CTReadData operation = component.getReadDataOperation(reader.getOperation());

                    // If notification is requested, push back a specific virtual
                    // event link for this reader
                    if (operation != null && operation.isNotifying()) {
                        ASOpRefReceive receiver = new ASOpRefReceive();
                        receiver.setInstance(reader.getInstance());
                        receiver.setOperation(prefix + '_' + reader.getOperation());
                        receiver.setActivating(reader.isActivating());
                        receiver.setFifoSize(reader.getFifoSize());
                        receiver.getWhen().addAll(reader.getWhen());
                        ASEventLink newEventLink = new ASEventLink();
                        // associate neweventlink to original datalink
                        newEventLink.setNotifiedData(dataLink.getId());
                        newEventLink.getSender().addAll(senders); // senders = all writers
                        newEventLink.getReceiver().add(receiver); // 1 receiver only
                        allocateId(newEventLink);
                        virtualLinks.add(newEventLink);
                    }
                }
            }
        }
    }

    private void addVirtualLinks() {

        Collection<ASOperationLink> virtualLinks = new ArrayList<>();

        // Creation of virtual links for Triggers
        for (InstanceAdapter i : rootInstance._instances) {
            triggerLink(i, virtualLinks);
        }

        // add event links for notification
        // REQ-005
        for (Object l : technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink()) {
            if (l instanceof ASDataLink) {
                notifyData((ASDataLink) l, virtualLinks);
            }
        }

        technicalAssembly.getLinks().getDataLinkOrEventLinkOrRequestLink().addAll(virtualLinks);
    }

    /**
     * Create the links of the technical assembly.
     * 
     * @param technicalAssembly The technical assembly.
     * @param assembly The original assembly.
     */
    private void createLinks() {

        technicalAssembly.setLinks(factory.createASLinks());

        createASDataLinks();
        createASEventLinks();
        createASRequestResponseLinks();

        addVirtualLinks();
    }

    private void allocateId(ASOperationLink link) {
        ArrayList<ASOpRef> linkElements = new ArrayList<ASOpRef>();
        if (link instanceof ASEventLink) {
            linkElements.addAll(((ASEventLink) link).getSender());
            linkElements.addAll(((ASEventLink) link).getReceiver());
        }
        if (link instanceof ASDataLink) {
            linkElements.addAll(((ASDataLink) link).getWriter());
            linkElements.addAll(((ASDataLink) link).getReader());
        }
        if (link instanceof ASRequestResponseLink) {
            linkElements.add(((ASRequestResponseLink) link).getClient());
            linkElements.addAll(((ASRequestResponseLink) link).getServer());
        }
        TreeSet<String> set = new TreeSet<String>();
        for (ASOpRef le : linkElements) {
            set.add(le.getInstance());
            set.add(le.getOperation());
        }
        link.setId((idServer.getLinkId(set.hashCode())));
    }

    public ASAssembly createTechnicalAssembly(RootInstance rootInstance) throws IOException {

        technicalAssembly = factory.createASAssembly();
        this.rootInstance = rootInstance;

        allInstancesWithExtern = new ArrayList<InstanceAdapter>(rootInstance._instances);
        allInstancesWithExtern.add(rootInstance);

        if (rootInstance.getInternalAssembly().getToplevelComponentType() != null) {
            technicalAssembly.setComponentType(rootInstance.getInternalAssembly().getToplevelComponentType().getName());
        }

        createInstances();
        createLinks();

        return technicalAssembly;
    }

}
