/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s40.importassembly;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguageHandler;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguagesHandler;

import java.io.IOException;
import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASOpRefClient;
import technology.ecoa.model.assembly.ASOpRefRead;
import technology.ecoa.model.assembly.ASOpRefReceive;
import technology.ecoa.model.assembly.ASOpRefSend;
import technology.ecoa.model.assembly.ASOpRefServer;
import technology.ecoa.model.assembly.ASOpRefWrite;
import technology.ecoa.model.assembly.ASRequestResponseLink;
import technology.ecoa.model.assembly.ASWhenCondition;
import technology.ecoa.model.deployment.EStartMode;

import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.InstanceAttribute;
import com.thalesgroup.softarc.sf.Mapping;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.PortData;
import com.thalesgroup.softarc.sf.PortEvent;
import com.thalesgroup.softarc.sf.PortRequestResponse;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.Trigger;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.Variable;
import com.thalesgroup.softarc.sf.WhenCondition;
import com.thalesgroup.softarc.sf.impl.QDataLink;
import com.thalesgroup.softarc.sf.impl.QDataLinkElement;
import com.thalesgroup.softarc.sf.impl.QEventLink;
import com.thalesgroup.softarc.sf.impl.QEventLinkReceiver;
import com.thalesgroup.softarc.sf.impl.QEventLinkSender;
import com.thalesgroup.softarc.sf.impl.QInstance;
import com.thalesgroup.softarc.sf.impl.QMapping;
import com.thalesgroup.softarc.sf.impl.QOperationEvent;
import com.thalesgroup.softarc.sf.impl.QPortData;
import com.thalesgroup.softarc.sf.impl.QPortEvent;
import com.thalesgroup.softarc.sf.impl.QPortRequestResponse;
import com.thalesgroup.softarc.sf.impl.QRequestResponseLink;
import com.thalesgroup.softarc.sf.impl.QRequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.impl.QWhenCondition;

public class ImportAssembly extends AbstractPass {

    @Override
    public void execute() throws IOException {

        Assembly assembly = context.system.getAssembly();

        for (Instance instance : assembly.getInstances()) {
            registerInstance(instance);
        }
        createExternInstance(assembly);

        addVirtualEvents();

        createPorts(assembly);
        importLinks(assembly);

        setDataLinkDefaultValues();

        resolveAttributes();
        // TR-SARC-GEN-REQ-131_135 done in GenThread
        
        createMapping();
    }

    void addVirtualEvents() {
        for (Component component : context.system.getComponentsIncludingInterface()) {
            if (!component.getIsLibrary() && !component.getIsTimer()) {
                QOperationEvent notify = createVirtualEvent("sarc_notify", component);
                component.getSentEvents().add(notify);
                component.getOperations().add(notify);
            }
            for (Trigger t : component.getTriggers()) {
                QOperationEvent notify = createVirtualEvent("sarc_trigger_" + t.getName(), component);
                component.getSentEvents().add(notify);
                component.getOperations().add(notify);
            }
        }
    }

    QOperationEvent createVirtualEvent(String name, Component component) {
        QOperationEvent op = new QOperationEvent();
        op.setName(name);
        op.setVirtual(true);
        op.setXmlID("op:" + component.getFullName() + "/" + name);
        return op;
    }

    void createExternInstance(Assembly assembly) {
        // TR-SARC-GEN-REQ-123
        if (context.system.getInterface() != null) {
            Instance instance = new QInstance();
            // technology.ecoa.model.Instance.SPECIAL_INSTANCE_NAME_EXTERN, null, null,
            // (ComponentTypeAdaptor) dest.getInterface(), wrapAssembly, this.generator, DefaultWrapperFactory.instance,
            // null);
            instance.setName("extern");

            instance.setType(context.system.getInterface());
            instance.setIsExtern(true);

            assembly.setExternInstance(instance);
            registerInstance(instance);
        }
    }

    void createPorts(Assembly assembly) {
        // TR-SARC-GEN-REQ-124
        for (Instance i : assembly.getInstancesIncludingExtern()) {
            for (OperationEvent o : i.getType().getSentEvents()) {
                createPortEvent(i, o);
            }
            for (OperationEvent o : i.getType().getReceivedEvents()) {
                createPortEvent(i, o);
            }
        }

        // TR-SARC-GEN-REQ-125
        for (Instance i : assembly.getInstancesIncludingExtern()) {
            for (OperationData o : i.getType().getData()) {
                createPortData(i, o);
            }
        }

        for (Instance i : assembly.getInstancesIncludingExtern()) {
            for (OperationRequestResponse o : i.getType().getRequiredRequestResponses()) {
                createPortRequestResponse(i, o);
            }
            for (OperationRequestResponse o : i.getType().getProvidedRequestResponses()) {
                createPortRequestResponse(i, o);
            }
        }

    }

    void importLinks(Assembly assembly) throws IOException {

        LinkedHashMap<EventLink, Long> notificationMap = new LinkedHashMap<>();
        LinkedHashMap<Long, DataLink> dataLinksMap = new LinkedHashMap<>();

        for (Object l : context.ASFILE.getLinks().getDataLinkOrEventLinkOrRequestLink()) {
            if (l instanceof ASEventLink) {
                // TR-SARC-GEN-REQ-126
                ASEventLink link = (ASEventLink) l;
                EventLink el = new QEventLink();
                el.setId(link.getId());
                for (ASOpRefSend ls : link.getSender()) {
                    for (PortEvent portEvent : findInstance(ls.getInstance()).getPortsEvent()) {
                        if (portEvent.getOperation().getName().equals(ls.getOperation())) {
                            QEventLinkSender sender = new QEventLinkSender();
                            sender.setPort(portEvent);
                            addWhenConditions(ls.getWhen(), sender.getWhenconditions());
                            el.getSenders().add(sender);
                            if (portEvent.getInstance() == assembly.getExternInstance()) {
                                el.setIncludeExtern(true);
                            }
                            break;
                        }
                    }
                }
                for (ASOpRefReceive lr : link.getReceiver()) {
                    // TR-SARC-GEN-REQ-127
                    for (PortEvent port : findInstance(lr.getInstance()).getPortsEvent()) {
                        if (port.getOperation().getName().equals(lr.getOperation())) {
                            QEventLinkReceiver receiver = new QEventLinkReceiver();
                            receiver.setFifoSize(lr.getFifoSize());
                            receiver.setActivating(lr.isActivating());
                            receiver.setPort(port);
                            addWhenConditions(lr.getWhen(), receiver.getWhenconditions());
                            el.getReceivers().add(receiver);
                            if (port.getInstance() == assembly.getExternInstance()) {
                                el.setIncludeExtern(true);
                            }
                            break;
                        }
                    }
                }
                if (link.getNotifiedData() != null) {
                    notificationMap.put(el, link.getNotifiedData());
                }
                assembly.getEventLinks().add(el);
            } else if (l instanceof ASDataLink) {
                // TR-SARC-GEN-REQ-128
                ASDataLink link = (ASDataLink) l;
                QDataLink dl = new QDataLink();
                dl.setId(link.getId());

                dl.setDirect(link.isUncontrolledAccess());

                for (ASOpRefRead lr : link.getReader()) {
                    for (PortData portData : findInstance(lr.getInstance()).getPortsData()) {
                        if (portData.getOperation().getName().equals(lr.getOperation())) {
                            QDataLinkElement el = new QDataLinkElement();
                            el.setPort(portData);
                            addWhenConditions(lr.getWhen(), el.getWhenconditions());
                            dl.getReaders().add(el);
                            if (portData.getInstance() == assembly.getExternInstance()) {
                                dl.setIncludeExtern(true);
                            }
                            break;
                        }
                    }
                }
                for (ASOpRefWrite lr : link.getWriter()) {
                    for (PortData portData : findInstance(lr.getInstance()).getPortsData()) {
                        if (portData.getOperation().getName().equals(lr.getOperation())) {
                            QDataLinkElement el = new QDataLinkElement();
                            el.setPort(portData);
                            addWhenConditions(lr.getWhen(), el.getWhenconditions());
                            dl.getWriters().add(el);
                            if (portData.getInstance() == assembly.getExternInstance()) {
                                dl.setIncludeExtern(true);
                            }
                            break;
                        }
                    }
                }

                // TR-SARC-GEN-REQ-129
                if (link.getDefaultValue() != null) {
                    dl.setDefaultValue(link.getDefaultValue().getValue());
                    String typeName = link.getDefaultValue().getType();
                    if (typeName != null && !typeName.isEmpty()) {
                        TypeDefinition td = lookupTypeName(typeName);
                        if (td == null) {
                            errorModel("unknown type '%s' used as for default value in datalink", typeName);
                        }
                        dl.setDefaultValueType(td);
                    }
                }

                // TR-SARC-GEN-REQ-130
                for (ASOpRefWrite lr : link.getWriter()) {
                    if (lr.isReference()) {
                        for (DataLinkElement el : dl.getWriters()) {
                            el.getPort().setReference(dl);
                        }
                    }
                }
                assembly.getDataLinks().add(dl);
                dataLinksMap.put(dl.getId(), dl);

            } else if (l instanceof ASRequestResponseLink) {
                ASRequestResponseLink link = (ASRequestResponseLink) l;
                RequestResponseLink sl = new QRequestResponseLink();
                sl.setId(link.getId());
                {
                    ASOpRefClient lc = link.getClient();
                    Instance findInstance = findInstance(lc.getInstance());
                    for (PortRequestResponse port : findInstance.getPortsRequestResponse()) {
                        if (port.getOperation().getName().equals(lc.getOperation())) {
                            QRequestResponseLinkReceiver receiver = new QRequestResponseLinkReceiver();
                            receiver.setFifoSize(lc.getFifoSize());
                            receiver.setActivating(lc.isCallbackActivating());
                            receiver.setPort(port);
                            receiver.setInChannelName(lc.getInChannel());
                            receiver.setOutChannelName(lc.getOutChannel());
                            addWhenConditions(lc.getWhen(), receiver.getWhenconditions());
                            sl.setClient(receiver);
                            if (port.getInstance() == assembly.getExternInstance()) {
                                sl.setIncludeExtern(true);
                            }
                            break;
                        }

                    }
                    if (sl.getClient() == null) {
                        errorModel("client not found for link %d, oper %s.%s", (int) sl.getId(), lc.getInstance(),
                                lc.getOperation());
                    }
                }
                for (ASOpRefServer ls : link.getServer()) {
                    for (PortRequestResponse port : findInstance(ls.getInstance()).getPortsRequestResponse()) {
                        if (port.getOperation().getName().equals(ls.getOperation())) {
                            QRequestResponseLinkReceiver receiver = new QRequestResponseLinkReceiver();
                            receiver.setFifoSize(ls.getFifoSize());
                            receiver.setActivating(ls.isActivating());
                            receiver.setPort(port);
                            addWhenConditions(ls.getWhen(), receiver.getWhenconditions());
                            sl.getServers().add(receiver);
                            if (port.getInstance() == assembly.getExternInstance()) {
                                sl.setIncludeExtern(true);
                            }
                            break;
                        }
                    }
                }
                assembly.getRequestResponseLinks().add(sl);
            }
        }

        // résolution des liens directs EventLink->DataLink pour notification
        for (Entry<EventLink, Long> entry : notificationMap.entrySet()) {
            entry.getKey().setNotifiedData(dataLinksMap.get(entry.getValue()));
        }
    }

    private void addWhenConditions(List<ASWhenCondition> list, Collection<WhenCondition> outList) throws IOException {
        for (ASWhenCondition w : list) {
            Instance supervisorInstance = findInstance(w.getInstance());
            QWhenCondition condition = new QWhenCondition();
            condition.setValue(w.getValue());
            for (Variable v : supervisorInstance.getVariables()) {
                if (v.getName().equals(w.getVariable())) {
                    condition.setVariable(v);
                    condition.setId(v.getId());
                    break;
                }
            }
            if (condition.getVariable() == null) {
                errorModel("variable '%s' used in <whencondition> element is not defined on instance %s", w.getVariable(),
                        supervisorInstance.getName());
            }
            outList.add(condition);
        }
    }

    // TR-SARC-GEN-REQ-124
    void createPortEvent(Instance i, OperationEvent o) {
        PortEvent port = new QPortEvent();
        port.setInstance(i);
        port.setOperation(o);
        port.setEvent(o);
        port.setXmlID("port:" + i.getName() + "/" + o.getName());
        i.getPortsEvent().add(port);
        i.getPorts().add(port);
    }

    // TR-SARC-GEN-REQ-125
    void createPortData(Instance i, OperationData o) {
        PortData port = new QPortData();
        port.setInstance(i);
        port.setOperation(o);
        port.setData(o);
        port.setXmlID("port:" + i.getName() + "/" + o.getName());
        port.setReference(null);
        i.getPortsData().add(port);
        i.getPorts().add(port);
    }

    void createPortRequestResponse(Instance i, OperationRequestResponse o) {
        QPortRequestResponse port = new QPortRequestResponse();
        port.setInstance(i);
        port.setOperation(o);
        port.setRequestResponse(o);
        port.setXmlID("port:" + i.getName() + "/" + o.getName());
        i.getPortsRequestResponse().add(port);
        i.getPorts().add(port);
    }

    private LinkedHashMap<String, Instance> allInstancesMap = new LinkedHashMap<>();

    private void registerInstance(Instance i) {
        allInstancesMap.put(i.getName(), i);
        context.system.getAssembly().getInstancesIncludingExtern().add(i);
    }

    private Instance findInstance(String name) throws IOException {
        Instance result = allInstancesMap.get(name);
        if (result == null)
            errorModel("instance %s not found in TechnicalAssembly.xml", name);
        return result;
    }

    void setDataLinkDefaultValues() throws IOException {
        // TR-SARC-GEN-REQ-145
        for (DataLink d : context.system.getAssembly().getDataLinks()) {

            if (!d.getDefaultValue().isEmpty()) {

                if (d.getDefaultValueType() == null) {
                    findDefaultValueType(d, d.getWriters());

                    if (d.getDefaultValueType() == null) {
                        findDefaultValueType(d, d.getReaders());

                        if (d.getDefaultValueType() == null) {
                            errorModel("cannot determine the type of default value for datalink %s", d.getId());

                        }
                    }
                }
            }
        }
    }

    private void findDefaultValueType(DataLink d, Collection<DataLinkElement> elements) {
        for (DataLinkElement p : elements) {
            TypeDefinition type = p.getPort().getData().getType();
            if (type != null) {
                d.setDefaultValueType(type);
                break;
            }
        }
    }

    private TypeDefinition lookupTypeName(String reference) {
        for (Component c : context.system.getComponentsIncludingInterface()) {
            for (TypeDefinition td : c.getTypes()) {
                if (reference.equals(c.getTypeName() + '.' + td.getName())) {
                    return td;
                }
            }
        }
        return null;
    }

    void resolveAttributes() throws IOException {
        for (Instance i : context.system.getAssembly().getInstances()) {
            if (i.getAttributes().isEmpty())
                continue;
            HashMap<String, InstanceAttribute> instanceAttributesMap = new HashMap<>();
            LanguageHandler lh = LanguagesHandler.get(i.getType().getApiVariant());
            for (InstanceAttribute a : i.getAttributes()) {
                instanceAttributesMap.put(lh.avoidKeywords(a.getName()), a);
            }
            // Instance attributes shall be reordered in the order defined by the component type.
            i.getAttributes().clear();
            for (Parameter p : i.getType().getAttributes()) {
            	InstanceAttribute a = instanceAttributesMap.remove(p.getName());
                if (a == null) {
                    // value found in type but not in instance
                    errorModel("attribute %s defined on %s has no value in instance %s", p.getName(), i.getType().getXmlID(),
                            i.getName());
                } else {
                	a.setName(p.getName());
                    a.setType(p.getType());
                    a.setQType(p.getType().getQName());
                    i.getAttributes().add(a);
                }
            }
            // remaining values (found in instance but not in type)
            for (InstanceAttribute a : instanceAttributesMap.values()) {
                errorModel("attribute %s used in %s is not defined in instance %s", a.getName(), i.getName(), i.getType());
            }
        }
    }

    private void createMapping() {
        // Création de l'objet Mapping dans le formalisme
        Mapping m = new QMapping();
        context.system.setMapping(m);

        m.setDeploymentName(context.workspace.currentDeploymentName);

        m.setAutoStart(context.DEFILE.getStartMode() != EStartMode.NONE);
        m.setFastStart(context.DEFILE.getStartMode() == EStartMode.FAST);
    }

}
