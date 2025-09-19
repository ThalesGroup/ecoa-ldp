/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s51.entrypoints;

import java.util.Collection;
import java.util.Comparator;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.IdAllocator;
import com.thalesgroup.softarc.gen.common.MiddlewareConstants;
import com.thalesgroup.softarc.gen.common.NameValidator;
import java.io.IOException;
import com.thalesgroup.softarc.sf.EntryPoint;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.Mapping;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.Port;
import com.thalesgroup.softarc.sf.PortData;
import com.thalesgroup.softarc.sf.PortEvent;
import com.thalesgroup.softarc.sf.PortRequestResponse;
import com.thalesgroup.softarc.sf.Topic;
import com.thalesgroup.softarc.sf.TracePoint;
import com.thalesgroup.softarc.sf.impl.QEntryPoint;
import com.thalesgroup.softarc.sf.impl.QParameter;
import com.thalesgroup.softarc.sf.impl.QTracePoint;

// Création des objets EntryPoint et TracePoint 

public class EntryPoints extends AbstractPass {

    Mapping mapping;
    private IdAllocator allocator;
    private NameValidator nameValidator = new NameValidator(MiddlewareConstants.SARC_NAME_MAX_LENGTH - 1);

    @Override
    public void execute() throws IOException {

        this.mapping = context.system.getMapping();

        this.allocator = new IdAllocator("trace_points", 0);

        createSoftarcRuntimeTracePoint();

        for (Instance i : context.system.getAssembly().getInstances()) {
            createInstanceTracePoint(i);
            createTopics(i);
        }

        for (Instance i : context.system.getAssembly().getInstances()) {
            createOperationEntryPoints(i);
        }

        for (Instance i : context.system.getAssembly().getInstances()) {
            createLifecycleEntryPoints(i);
        }

        sortTracePointsById();
    }

    private void createSoftarcRuntimeTracePoint() throws IOException {
        mapping.getTracePoints().add(createTracePoint(".softarc.runtime", 6));
    }

    private TracePoint createTracePoint(String fullname, long initialState) throws IOException {
        TracePoint obs = new QTracePoint();
        // obs.setId(allocator.allocateFromString(fullname));
        obs.setId(allocator.allocate());
        String name = nameValidator.validate(fullname);
        if (!name.equals(fullname)) {
            gen.warning("trace point name '%s' is replaced by '%s'", fullname, name);
        }
        obs.setName(name);
        obs.setInitialState(initialState);
        return obs;
    }

    private void createInstanceTracePoint(Instance i) throws IOException {
        TracePoint obs = createTracePoint(i.getName(), i.getInstanceVerbosityLevel());
        mapping.getTracePoints().add(obs);
        i.setTracePoint(obs);
    }

    private void createLifecycleEntryPoints(Instance i) throws IOException {
        // Lifecycle entry points and trace points
        i.setInitialize(createLifecycleEntryPoint(i, "initialize"));
        i.setStart(createLifecycleEntryPoint(i, "start"));
        i.setStop(createLifecycleEntryPoint(i, "stop"));
        i.setReset(createLifecycleEntryPoint(i, "reset"));
        i.setShutdown(createLifecycleEntryPoint(i, "shutdown"));
    }

    private void createTopics(Instance i) throws IOException {
        // Topic trace points (for "trace_topic" API)
        // They shall immediately follow the instance-level trace point.
        for (Topic tp : i.getType().getTopics()) {
            String fullname = i.getName() + '.' + tp.getName();
            TracePoint obs = createTracePoint(fullname, i.getTopicsVerbosityLevel());
            mapping.getTracePoints().add(obs);
        }
    }

    private void createOperationEntryPoints(Instance i) throws IOException {
        for (PortEvent p : i.getPortsEvent()) {
            OperationEvent op = p.getEvent();
            if (op.getIsSent() && !op.getVirtual()) {
                p.setObservation(createObservation(i, op.getName(), ".send", op.getInParameters()));
            }
            if (op.getIsReceived()) {
                EntryPoint ep = createEntryPoint(p, op.getName());
                ep.setEvent(op);
                ep.setObservationBegin(createObservation(i, op.getName(), ".begin", op.getInParameters()));
                ep.setObservationEnd(createObservation(i, op.getName(), ".end", null));
                i.getEventEntryPoints().add(ep);
            }
        }
        for (PortData p : i.getPortsData()) {
            OperationData op = p.getData();
            if (op.getIsWritten()) {
                QParameter param = new QParameter();
                param.setType(op.getType());
                param.setTypeName(op.getTypeName());
                p.setObservation(createObservationData(i, op.getName(), ".publish", param));
            }
            if (op.getIsRead()) {
                QParameter param = new QParameter();
                param.setType(op.getType());
                param.setTypeName(op.getTypeName());
                p.setObservationRead(createObservationData(i, op.getName(), ".read", param));
            }
        }
        for (PortRequestResponse p : i.getPortsRequestResponse()) {
            OperationRequestResponse op = p.getRequestResponse();
            if (op.getIsProvided()) {
                EntryPoint ep = createEntryPoint(p, op.getName());
                ep.setRequestResponse(op);
                ep.setObservationBegin(createObservation(i, op.getName(), ".begin", op.getInParameters()));
                if (op.getIsDeferred()) {
                    ep.setObservationEnd(createObservation(i, op.getName(), ".end", null));
                    p.setObservation(createObservation(i, op.getName(), ".reply", op.getOutParameters()));
                } else {
                    ep.setObservationEnd(createObservation(i, op.getName(), ".end", op.getOutParameters()));
                }
                
                i.getRequestResponseEntryPoints().add(ep);
            }
        }
        for (PortRequestResponse p : i.getPortsRequestResponse()) {
            OperationRequestResponse op = p.getRequestResponse();
            if (op.getIsRequired()) {
                p.setObservation(createObservation(i, op.getName(), ".call", op.getInParameters()));
                if (op.getIsAsynchronous()) {
                    EntryPoint ep = createEntryPoint(p, op.getName());
                    ep.setRequestResponse(op);
                    i.getCallbackEntryPoints().add(ep);
                    // ep.setObservationBegin(createObservation(i, op.getName(), ".begin", op.getOutParameters()));
                    // ep.setObservationEnd(createObservation(i, op.getName(), ".end", null));
                    // ep.setObservationBegin(createObservation(i, op.getName(), ".timeout_begin", null));
                    // ep.setObservationEnd(createObservation(i, op.getName(), ".timeout_end", null));
                } else {
                    // ep.setObservationEnd(createObservation(i, op.getName(), ".timeout_end", null));
                }
            }
        }
    }

    private TracePoint createObservation(Instance i, String opName, String suffix, Collection<Parameter> params)
            throws IOException {

        if (!mapping.getGenerateObservability()) {
            return null;
        }

        final String notifyPrefix = "sarc_notify_";
        if (opName.startsWith(notifyPrefix)) {
            opName = opName.substring(notifyPrefix.length());
        }
        String fullname = i.getName() + '.' + opName + suffix;
        TracePoint obs = createTracePoint(fullname, i.getObservabilityLevel());
        obs.setIsBinary(true);
        mapping.getTracePoints().add(obs);
        addParameters(params, obs);
        return obs;
    }

    private TracePoint createObservationData(Instance i, String opName, String suffix, Parameter param) throws IOException {

        if (!mapping.getGenerateObservability()) {
            return null;
        }

        String fullname = i.getName() + '.' + opName + suffix;
        TracePoint obs = createTracePoint(fullname, i.getObservabilityLevel());
        obs.setIsBinary(true);
        mapping.getTracePoints().add(obs);
        addParameter(param, obs);
        obs.getParameters().iterator().next().setName("");
        return obs;
    }

    private void addParameters(Collection<Parameter> params, TracePoint obs) {
        if (params != null) {
            for (Parameter param : params) {
                addParameter(param, obs);
            }
        }
    }

    private void addParameter(Parameter param, TracePoint obs) {
        obs.getParameters().add(param);
    }

    private EntryPoint createEntryPoint(Port p, String name) {
        EntryPoint result = createEntryPoint(p.getInstance(), name);
        result.setPort(p);
        return result;
    }

    private EntryPoint createEntryPoint(Instance i, String name) {
        EntryPoint result = new QEntryPoint();
        result.setName(name);
        result.setXmlID("entrypoint:" + i.getName() + '/' + name);
        i.getEntryPoints().add(result);
        return result;
    }

    private EntryPoint createLifecycleEntryPoint(Instance i, String name) throws IOException {
        EntryPoint result = createEntryPoint(i, name);
        String tracePointName = "_" + name;
        result.setObservationBegin(createObservation(i, tracePointName, ".begin", null));
        result.setObservationEnd(createObservation(i, tracePointName, ".end", null));
        i.getLifecycleEntryPoints().add(result);
        return result;
    }

    /**
     * S'assurer que tous les TracePoints sont triés par id croissant.
     */
    private void sortTracePointsById() {

        mapping.getTracePoints().sort(new Comparator<TracePoint>() {
            @Override
            public int compare(TracePoint o1, TracePoint o2) {
                return Long.compare(o1.getId(), o2.getId());
            }
        });
    }

}
