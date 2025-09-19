/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASImplicitLinks;
import technology.ecoa.model.assembly.ASImplicitLinksOperations;
import technology.ecoa.model.assembly.ASOpRef;
import technology.ecoa.model.assembly.ASOpRefClient;
import technology.ecoa.model.assembly.ASOpRefRead;
import technology.ecoa.model.assembly.ASOpRefReceive;
import technology.ecoa.model.assembly.ASOpRefSend;
import technology.ecoa.model.assembly.ASOpRefServer;
import technology.ecoa.model.assembly.ASOpRefWrite;
import technology.ecoa.model.assembly.ASRequestResponseLink;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.componenttype.CTWrittenData;

public class ImplicitLinks {

    private final CompositeInstance composite;

    ImplicitLinks(CompositeInstance composite) {
        this.composite = composite;
    }

    /**
     * Create new operation links, based on the <implicitLinks> elements that may be present in the assembly. The new links are
     * direclty added to composite._internalAssembly.{event,data,service}Links
     */
    void createNewLinks() {

        // manual_ops := the list of all instanciated operations in normal Operation Links (EventLink, DataLink, RequestLink
        // elements)
        // For each element IL of type ImplicitLinks:
        // . auto_ops := Empty list
        // . For each element OS of type OperationSet in IL:
        // .... For each instance of the assembly, including "extern", which name matches OS.instance:
        // ....... For each operation which name starts with OS.prefix:
        // .......... If this operation is not in manual_ops, add it to auto_ops
        // . If all operations in auto_ops are not compatible, raise an error
        // . If nature=event, create an EventLink with all operations of auto_ops
        // . If nature=data, create a DataLink with all operations of auto_ops
        // . If nature=request, for each client of auto_ops, create a RequestLink with this client and all the servers of auto_ops

        List<ASImplicitLinks> implicitLinksDefinitions = composite._internalAssembly.assembly.getLinks().getImplicitLinks();
        if (implicitLinksDefinitions.isEmpty())
            return; // saving time...

        // manual_ops := the list of all instanciated operations in normal Operation Links (EventLink, DataLink, RequestLink
        // elements)
        HashSet<Operation> manualOps = findAllOperationsUsedInExplicitLinks();

        ArrayList<InstanceAdapter> instancesIncludingExtern = new ArrayList<>(composite._instances.size() + 1);
        instancesIncludingExtern.addAll(composite._instances);
        instancesIncludingExtern.add(composite);

        // For each element IL of type ImplicitLinks:
        for (ASImplicitLinks il : implicitLinksDefinitions) {

            // auto_ops := Empty list
            HashMap<String, HashSet<Operation>> autoOps = new HashMap<>();

            // For each element OS of type OperationSet in IL:
            for (ASImplicitLinksOperations implicitOp : il.getOperations()) {
                // For each instance of the assembly, including "extern", which name matches OS.instance:
                Collection<InstanceAdapter> instances = selectInstances(implicitOp.getInstance(), instancesIncludingExtern);
                for (InstanceAdapter i : instances) {
                    // For each operation which name starts with OS.prefix:
                    for (Operation op : i.operations.values()) {
                        if (op.name.startsWith(implicitOp.getPrefix())) {
                            // If this operation is not in manual_ops, add it to auto_ops
                            if (!manualOps.contains(op)) {
                                String commonName = op.name.substring(implicitOp.getPrefix().length());
                                HashSet<Operation> operationsOfThisName = autoOps.get(commonName);
                                if (operationsOfThisName == null) {
                                    operationsOfThisName = new HashSet<>();
                                    autoOps.put(commonName, operationsOfThisName);
                                }
                                operationsOfThisName.add(op);
                            }
                        }
                    }
                }
            }

            for (HashSet<Operation> set : autoOps.values()) {
                createNewOperationLink(set, il.isActivating());
            }
        }
    }

    private void createNewOperationLink(HashSet<Operation> set, boolean isActivating) {
        if (set.size() > 1) {

            // If all operations are not compatible, raise an error
            EDR commonTypeOfOperations = getCommonOperationType(set);

            switch (commonTypeOfOperations) {
            // If nature=event, create an EventLink with all operations of auto_ops
            case Event: {
                ASEventLink link = new ASEventLink();
                composite._internalAssembly.eventLinks.add(link);
                for (Operation op : set) {
                    if (op.isOfApparentType(composite, CTSentEvent.class)) {
                        var e = new ASOpRefSend();
                        setOpRef(op, e);
                        link.getSender().add(e);
                    }
                    if (op.isOfApparentType(composite, CTReceivedEvent.class)) {
                        var e = new ASOpRefReceive();
                        setOpRef(op, e);
                        e.setActivating(isActivating);
                        link.getReceiver().add(e);
                    }
                }
            }
                break;
            // If nature=data, create a DataLink with all operations of auto_ops
            case Data: {
                ASDataLink link = new ASDataLink();
                composite._internalAssembly.dataLinks.add(link);
                for (Operation op : set) {
                    if (op.isOfApparentType(composite, CTWrittenData.class)) {
                        var e = new ASOpRefWrite();
                        setOpRef(op, e);
                        e.setActivating(isActivating);
                        link.getWriter().add(e);
                    }
                    if (op.isOfApparentType(composite, CTReadData.class)) {
                        var e = new ASOpRefRead();
                        setOpRef(op, e);
                        e.setActivating(isActivating);
                        link.getReader().add(e);
                    }
                }
            }
                break;
            // If nature=request, for each client of auto_ops, create a RequestLink with this client and all the servers
            // of auto_ops
            case RequestResponse: {
                for (Operation op : set) {
                    if (op.isOfApparentType(composite, CTRequestSent.class)) {
                        var link = new ASRequestResponseLink();
                        composite._internalAssembly.serviceLinks.add(link);
                        var e = new ASOpRefClient();
                        setOpRef(op, e);
                        e.setCallbackActivating(isActivating);
                        link.setClient(e);
                        for (Operation opServer : set) {
                            if (opServer.isOfApparentType(composite, CTRequestReceived.class)) {
                                var eServer = new ASOpRefServer();
                                setOpRef(opServer, eServer);
                                eServer.setActivating(isActivating);
                                link.getServer().add(eServer);
                            }
                        }
                    }
                }
            }
                break;
            }
        }
    }

    private void setOpRef(Operation op, ASOpRef e) {
        e.setInstance(op.instance.getRelativeName(composite));
        e.setOperation(op.name);
    }

    private Collection<InstanceAdapter> selectInstances(String pattern, Collection<InstanceAdapter> fromList) {

        if (pattern.equals("*")) {
            return fromList;
        } else if (pattern.contains("*")) {
            pattern.replace("*", ".*?");
            Pattern p = Pattern.compile(pattern);
            return fromList.stream().filter(i -> p.matcher(i.getRelativeName(composite)).matches())
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            var list = fromList.stream().filter(i -> pattern.equals(i.getRelativeName(composite)))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (list.isEmpty()) {
                composite._gen.warning("In assembly '%s': Invalid instance name in implicit link: '%s'",
                        composite._assemblyFile.getPath(), pattern);
            }
            return list;
        }
    }

    private EDR getCommonOperationType(Collection<Operation> operations) {

        Operation firstOp = operations.stream().findFirst().get();
        EDR result = firstOp.type;

        // look for a different operation type
        Optional<Operation> otherType = operations.stream().skip(1).filter(op -> (op.type != result)).findFirst();

        if (otherType.isPresent()) {
            composite._gen.errorModel("In assembly '%s': Incompatible operations '%s' (%s) and '%s' (%s) in an implicit link",
                    composite._assemblyFile.getPath(), firstOp.name, result, otherType.get().name, otherType.get().type);
        }
        return result;
    }

    private HashSet<Operation> findAllOperationsUsedInExplicitLinks() {

        ArrayList<ASOpRef> opRefs = new ArrayList<>(256);
        for (ASDataLink link : composite._internalAssembly.dataLinks) {
            opRefs.addAll(link.getWriter());
            opRefs.addAll(link.getReader());
        }
        for (ASRequestResponseLink link : composite._internalAssembly.serviceLinks) {
            opRefs.addAll(link.getServer());
            opRefs.add(link.getClient());
        }
        for (ASEventLink link : composite._internalAssembly.eventLinks) {
            opRefs.addAll(link.getSender());
            opRefs.addAll(link.getReceiver());
        }

        HashSet<Operation> result = new HashSet<>(opRefs.size()); // result will be no larger than opRefs
        for (var opRef : opRefs) {
            result.add(composite.resolveOperation(opRef));
        }
        return result;
    }
}
