/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import com.thalesgroup.ecoa.model.Assembly;
import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.deployment.IOInterface;
import technology.ecoa.model.deployment.IOOperation;
import technology.ecoa.model.deployment.IOSection;
import com.thalesgroup.softarc.gen.technicalassembly.Wire.Kind;
import com.thalesgroup.softarc.tools.InconsistentModelError;

class RootInstance extends CompositeInstance {

    RootInstance(Assembly internalAssembly, GenTechnicalAssembly gen, File assemblyFile) throws Exception {

        super(null, null, internalAssembly, gen, assemblyFile);
    }

    void removeIdenticalWires() {
        HashSet<String> signatures = new HashSet<String>();
        ArrayList<Wire> newList = new ArrayList<Wire>();
        for (Wire w : wires) {
            String sig = w.toString();
            if (signatures.add(sig) == true) {
                // signatures did not already contain sig
                newList.add(w);
            }
        }
        wires = newList;
    }

    /*
     * ------------------------------------------------------------------ External Operations handling
     * ------------------------------------------------------------------
     */

    private HashSet<String> usedExternalOperations = new HashSet<>();

    public void removeUnusedExternalOperations(DEApplication deployment) throws Exception {

        buildUsedExternalOperations(deployment);

        if (!usedExternalOperations.isEmpty() && componentType == null) {
            _gen.errorModel(
                    "Attribute '/assembly/@componenttype' must be defined in %s, because <external_io> section is not empty in deployment.",
                    _gen.workspace.getAssemblyFile(deployment.getAssembly()));
        }

        for (Iterator<Wire> i = wires.iterator(); i.hasNext();) {
            Wire w = i.next();
            if (isUnusedExternalOperation(w.source) || isUnusedExternalOperation(w.target)) {
                i.remove();
            }
        }
    }

    private void buildUsedExternalOperations(DEApplication deployment) {

        IOSection io = deployment.getExternalIo();
        if (io != null) {
            Stream.concat(Stream.concat(io.getInPort().stream(), io.getOutPort().stream()), io.getInOutPort().stream())
                    .forEach(c -> {
                        for (IOOperation op : c.getOperation()) {
                            usedExternalOperations.add(op.getName());
                        }
                    });
        }
    }

    private boolean isUnusedExternalOperation(Operation operation) {
        if (operation.instance == this && !usedExternalOperations.contains(operation.name)) {
            _gen.warning("external operation '%s' is not mapped on any channel", operation.name);
            return true;
        }
        return false;
    }

    public void checkExternalRequestResponseOperations(DEApplication deployment) throws Exception {

        checkExternalRequestResponseLinks();

        IOSection io = deployment.getExternalIo();
        if (io != null) {
            LinkedHashMap<String, LinkedHashSet<String>> inChannelsOfRequestResponse = new LinkedHashMap<String, LinkedHashSet<String>>();
            LinkedHashMap<String, LinkedHashSet<String>> outChannelsOfRequestResponse = new LinkedHashMap<String, LinkedHashSet<String>>();

            for (IOInterface c : io.getInPort()) {
                for (IOOperation op : c.getOperation()) {
                    if (componentType.getRequestResponsesRequired().get(op.getName()) != null) {
                        addExternalChannelForRequestResponse(inChannelsOfRequestResponse, c.getName(), op.getName(), op.getId());
                    }
                }
            }
            for (IOInterface c : io.getOutPort()) {
                for (IOOperation op : c.getOperation()) {
                    if (componentType.getRequestResponsesRequired().get(op.getName()) != null) {
                        addExternalChannelForRequestResponse(outChannelsOfRequestResponse, c.getName(), op.getName(), op.getId());
                    }
                }
            }

            LinkedHashSet<String> allRequestResponses = new LinkedHashSet<String>(inChannelsOfRequestResponse.keySet());
            allRequestResponses.addAll(outChannelsOfRequestResponse.keySet());
            for (String key : allRequestResponses) {
                LinkedHashSet<String> inChannels = inChannelsOfRequestResponse.get(key);
                LinkedHashSet<String> outChannels = outChannelsOfRequestResponse.get(key);
                if (inChannels == null || outChannels == null || inChannels.size() > 1 || outChannels.size() > 1) {
                    _gen.errorModel(
                            "External service operation %s shall be associated with an input-output port, or with one input port + one output port",
                            key);
                }
            }
        }
    }

    private void checkExternalRequestResponseLinks() throws Exception {
        LinkedHashSet<Operation> set = new LinkedHashSet<Operation>();
        for (Wire w : wires) {
            if (w.kind.equals(Kind.SERVICE)) {
                if (w.target.instance == this) {
                    if (set.add(w.target) == false) {
                        throw new InconsistentModelError(String.format(
                                "External service operation '%s' (defined as server) shall not be linked to more than one client",
                                w.target.name));
                    }
                }
                if (w.source.instance == this) {
                    if (set.add(w.source) == false) {
                        throw new InconsistentModelError(String.format(
                                "External service operation '%s' (defined as client) shall not be linked to more than one server",
                                w.source.name));
                    }
                }
            }
        }
    }

    private void addExternalChannelForRequestResponse(LinkedHashMap<String, LinkedHashSet<String>> map, String channelName, String opName,
            String opId) {
        String key = "'" + opName + "' with id '" + opId + "'";
        LinkedHashSet<String> set = map.get(key);
        if (set == null) {
            set = new LinkedHashSet<String>();
            map.put(key, set);
        }
        set.add(channelName);
    }

    /**
     * For each external service operation, duplicate it for each client found in the deployment. Each client has a message in an
     * IN channel, and a message in an OUT channel, with the same operation and the same external id. Note: existence & unicity
     * checks are done in checkExternalRequestResponseOperations().
     * 
     * @param deployment the deployment
     */
    public void duplicateExternalRequestResponseLinks(DEApplication deployment) {
        IOSection io = deployment.getExternalIo();
        if (io != null) {
            ArrayList<Wire> toBeRemoved = new ArrayList<Wire>();
            ArrayList<Wire> toBeAdded = new ArrayList<Wire>();
            final List<IOInterface> allInPorts = new ArrayList<>(io.getInPort());
            final List<IOInterface> allOutPorts = new ArrayList<>(io.getOutPort());
            allInPorts.addAll(io.getInOutPort());
            allOutPorts.addAll(io.getInOutPort());

            /* for each wire corresponding to a service operation with an external client */
            for (Wire w : wires) {
                if (w.kind.equals(Kind.SERVICE) && w.source.instance == this) {
                    toBeRemoved.add(w);
                    String opName = w.source.name;
                    for (IOInterface inChannel : allInPorts) {
                        for (IOOperation inOp : inChannel.getOperation()) {
                            if (inOp.getName().equals(opName)) {
                                /* Found the IN channel for the request. Now find the OUT channel for the response. */
                                for (IOInterface outChannel : allOutPorts) {
                                    for (IOOperation outOp : outChannel.getOperation()) {
                                        if (outOp.getName().equals(opName) && outOp.getId().equals(inOp.getId())) {
                                            /*
                                             * Found an operation, in an OUT channel, with the same name and id. Keep track of
                                             * these 2 channels.
                                             */
                                            Wire newWire = new Wire(w);
                                            newWire.inChannel = inChannel.getName();
                                            newWire.outChannel = outChannel.getName();
                                            toBeAdded.add(newWire);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            wires.removeAll(toBeRemoved);
            wires.addAll(toBeAdded);
        }
    }

}
