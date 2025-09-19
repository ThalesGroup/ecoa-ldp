/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.thalesgroup.softarc.tools.InconsistentModelError;
import com.thalesgroup.softarc.tools.Requirement;

import technology.ecoa.model.assembly.ASAssembly;
import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASInstance;
import technology.ecoa.model.assembly.ASRequestResponseLink;
import technology.ecoa.model.componenttype.CTComponentType;
import technology.ecoa.model.componenttype.CTEvent;
import technology.ecoa.model.componenttype.CTOperation;
import technology.ecoa.model.componenttype.CTOperations;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.componenttype.CTWrittenData;

@Requirement(ids = { "GenFramework-SRS-REQ-123", "GenFramework-SRS-REQ-124", "GenFramework-SRS-REQ-125" })
public final class Assembly {

    private ComponentType toplevelComponentType;

    public Assembly(ASAssembly assembly, File file, ModelLoader loader) throws IOException {

        this.assembly = assembly;

        // Regular links (explicitly declared ones)
        for (Object link : assembly.getLinks().getDataLinkOrEventLinkOrRequestLink()) {
            if (link instanceof ASEventLink) {
                eventLinks.add((ASEventLink) link);
            } else if (link instanceof ASDataLink) {
                dataLinks.add((ASDataLink) link);
            } else if (link instanceof ASRequestResponseLink) {
                serviceLinks.add((ASRequestResponseLink) link);
            }
        }

        // Instances
        for (ASInstance ci : assembly.getInstance()) {
            String ct = ci.getComponentType();
            if (ct == null || ct.isEmpty()) {
                throw new InconsistentModelError("missing componentType for instance: " + ci.getName());
            }
            compTypeNames.add(ct);
        }

        // Referenced component types
        if (loader != null) {
            for (String ct : compTypeNames) {
                compTypes.put(ct, loader.loadComponentTypeDeeply(ct));
            }

            for (ASInstance instance : assembly.getInstance()) {
                ComponentType component = compTypes.get(instance.getComponentType());
                components.put(instance.getName(), component);

                Implementation implementation = null;
                implementation = loader.loadImplementation(instance.getComponentType(), instance.getImplementation());
                instances.put(instance.getName(), new Instance(instance, component, implementation));
            }
        }

        // Determine a type for <defaultvalue> nodes
        for (ASDataLink link : this.dataLinks) {
            if (link.getDefaultValue() != null && link.getDefaultValue().getType() == null) {
                // Search among all types for the ones that could fit
            }
        }

        // Assembly shall have an associated component type if external operations are used
        if (assembly.getComponentType() != null && loader != null) {
            // load this component type
            toplevelComponentType = loader.loadComponentTypeDeeply(assembly.getComponentType());

            // create a "reversed" component type, and associate it to instance "extern"
            ComponentType component = reverse(toplevelComponentType);
            // sub libraries must be loaded because they are needed by the checker
            component.loadSubLibraries(loader);
            components.put(Instance.SPECIAL_INSTANCE_NAME_EXTERN, component);

            // create instance "extern"
            ASInstance i = new ASInstance();
            i.setName(Instance.SPECIAL_INSTANCE_NAME_EXTERN);
            instances.put(Instance.SPECIAL_INSTANCE_NAME_EXTERN, new Instance(i, component, null));
        }
    }

    private ComponentType reverse(ComponentType ct) {
        CTComponentType ctr = new CTComponentType();
        ctr.setOperations(new CTOperations());
        for (CTOperation op : ct.getAllOperations()) {
            CTOperation rop = reverse(op);
            if (rop != null) {
                ctr.getOperations().getAllOperations().add(rop);
            }
        }
        return new ComponentType(ct.getName() + "_reversed", ctr);
    }

    static public CTOperation reverse(CTOperation op) {
        CTOperation rop = null;
        if (op instanceof CTReceivedEvent) {
            rop = new CTSentEvent();
            ((CTEvent) rop).getParameter().addAll(((CTReceivedEvent) op).getParameter());
        } else if (op instanceof CTSentEvent) {
            rop = new CTReceivedEvent();
            ((CTEvent) rop).getParameter().addAll(((CTSentEvent) op).getParameter());
        } else if (op instanceof CTReadData) {
            rop = new CTWrittenData();
            ((CTWrittenData) rop).setType(((CTReadData) op).getType());
            ((CTWrittenData) rop).setWriteOnly(true);
        } else if (op instanceof CTWrittenData) {
            rop = new CTReadData();
            ((CTReadData) rop).setType(((CTWrittenData) op).getType());
        } else if (op instanceof CTRequestSent) {
            rop = new CTRequestReceived();
            ((CTRequestReceived) rop).getParameter().addAll(((CTRequestSent) op).getParameter());
            ((CTRequestReceived) rop).getOut().addAll(((CTRequestSent) op).getOut());
        } else if (op instanceof CTRequestReceived) {
            rop = new CTRequestSent();
            ((CTRequestSent) rop).getParameter().addAll(((CTRequestReceived) op).getParameter());
            ((CTRequestSent) rop).getOut().addAll(((CTRequestReceived) op).getOut());
        }
        if (rop != null) {
            rop.setName(op.getName());
        }
        return rop;
    }

    public Instance getInstance(String name) {
        return instances.get(name);
    }

    /**
     * @param instance_name
     *            can be a real instance or "extern"
     * @return never null
     * @throws Exception
     *             if instance not found
     */
    public ComponentType getComponentType(String instance_name) throws Exception {
        ComponentType ct = components.get(instance_name);
        if (ct == null) {
            if (instance_name.equals(Instance.SPECIAL_INSTANCE_NAME_EXTERN)) {
                throw new Exception(
                        "Attribute assembly/@componenttype shall be defined because top-level assembly uses pseudo-instance 'extern'");
            } else {
                throw new Exception("Instance not found: " + instance_name);
            }
        }
        return ct;
    }

    public ComponentType getToplevelComponentType() {
        return toplevelComponentType;
    }

    // ! ComponentType per model name
    public final Map<String, ComponentType> compTypes = new LinkedHashMap<String, ComponentType>();

    // ! ComponentType per instance name
    private final Map<String, ComponentType> components = new LinkedHashMap<String, ComponentType>();

    // ! Instance per instance name
    public final Map<String, Instance> instances = new LinkedHashMap<String, Instance>();

    public final Set<ASEventLink> eventLinks = new LinkedHashSet<ASEventLink>();

    public final Set<ASDataLink> dataLinks = new LinkedHashSet<ASDataLink>();

    public final Set<ASRequestResponseLink> serviceLinks = new LinkedHashSet<ASRequestResponseLink>();

    public final Set<String> compTypeNames = new LinkedHashSet<String>();

    public final ASAssembly assembly;
}
