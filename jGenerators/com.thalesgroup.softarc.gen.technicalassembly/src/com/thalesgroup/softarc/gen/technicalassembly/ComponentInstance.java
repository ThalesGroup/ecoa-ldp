/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.LinkedHashMap;
import java.util.Map;

import com.thalesgroup.ecoa.model.ComponentType;
import com.thalesgroup.ecoa.model.Instance;

import technology.ecoa.model.componenttype.CTOperation;
import technology.ecoa.model.componenttype.CTWrittenData;
import technology.ecoa.model.assembly.ASIdentifiedMemberValue;
import technology.ecoa.model.assembly.ASMemberValue;

public class ComponentInstance implements Comparable<ComponentInstance> {

    /** The adapted instance found in the input assembly model (null for root instance) */
    public final Instance model;

    public ComponentType componentType;

    /** The full instance name */
    public String fullName;

    /** The attributes of the instance (attribute --> value) */
    public final Map<String, String> attributes;

    /** The persistent information of the instance (key --> value) */
    public final Map<String, String> pinfos;

    /** The variables of the instance that are locally defined (variable --> initial value) */
    public final Map<String, String> variables;

    public final Map<String, Operation> operations;

    public ComponentInstance(Instance instance) {
        model = instance;
        attributes = new LinkedHashMap<String, String>();
        pinfos = new LinkedHashMap<String, String>();
        variables = new LinkedHashMap<String, String>();
        operations = new LinkedHashMap<String, Operation>();
        if (model != null) {
            fullName = model.getName();
            setComponentType(model.getComponentType());
            for (ASMemberValue attribute : instance.getInstance().getPropertyValue()) {
                attributes.put(attribute.getName(), attribute.getValue());
            }
            for (ASMemberValue pinfo : instance.getInstance().getPinfoValue()) {
                pinfos.put(pinfo.getName(), pinfo.getValue());
            }
            for (ASIdentifiedMemberValue variable : instance.getInstance().getVariableInit()) {
                variables.put(variable.getName(), variable.getValue());
            }
        }
    }

    protected void setComponentType(ComponentType ct) {

        if (ct != null) {
            componentType = ct;
            // WARNING: use the original list of CTOperations from JAXB model (not the wrapped ones!)
            for (CTOperation ctop : componentType.getComponentType().getOperations().getAllOperations()) {
                Operation op = new Operation(this, ctop.getName(), ctop);
                if (ctop instanceof CTWrittenData) {
                    op.writeOnly = ((CTWrittenData) ctop).isWriteOnly();
                }
                operations.put(op.name, op);
                // System.out.println(ct.getName() + "." + ctop.getName());
            }
        }
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return model.getName();
    }

    public String getRelativeName(ComponentInstance fromComposite) {
        if (fromComposite == this)
            return Instance.SPECIAL_INSTANCE_NAME_EXTERN;
        else
            // root instance can only be referenced from itself => model != null
            return model.getName();
    }

    /**
     * @return true if the instance shall be generated in the technical assembly
     */
    public boolean isComposite() {
        return false;
    }

    public int compareTo(ComponentInstance o) {
        return fullName.compareTo(o.fullName);
    }

}