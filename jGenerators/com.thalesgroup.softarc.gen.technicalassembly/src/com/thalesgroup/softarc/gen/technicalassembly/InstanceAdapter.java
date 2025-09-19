/* Copyright (c) 2025 THALES -- All rights reserved */

/**
 * Copyright (c) 2011 THALES.
 * All rights reserved.
 */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.Map.Entry;

import technology.ecoa.model.assembly.ASVariableAlias;

import com.thalesgroup.ecoa.model.Instance;
import com.thalesgroup.softarc.tools.InconsistentModelError;

/**
 * This is an instance in the scope of a complete system. (By contrast, instance in the model are only defined in the scope of the
 * parent assembly: toplevel or internal assembly of a composite definition)
 */
public class InstanceAdapter extends ComponentInstance {

    /** The instance that includes this one (null for root instance) */
    final CompositeInstance _parent;

    /** The separation string for the instancename prefix */
    private static final String SEPARATOR = "_";

    /**
     * Creation of a component instance.
     * 
     * @param instance The instance to adapt. Is null for the root instance (for which there is no ASInstance in the input models)
     */
    public InstanceAdapter(Instance instance, CompositeInstance parent) {
        super(instance);

        _parent = parent;

        if (parent == null)
            // REQ
            fullName = Instance.SPECIAL_INSTANCE_NAME_EXTERN;
        else if (parent._parent != null)
            fullName = parent.fullName + SEPARATOR + model.getName();

        if (instance != null) {
            for (ASVariableAlias variable : instance.getInstance().getVariableAlias()) {
                if (parent == null) {
                    throw new InconsistentModelError("<variablealias> elements are not allowed in toplevel assembly");
                }
                parent._variableAliases.put(variable.getAlias(), new VariableReference(this, variable.getName()));
            }
        }
    }

    public boolean isRoot() {
        return _parent == null;
    }

    /**
     * Replace, in the attributes, the values starting with '$' by their real value, accoring to the attribute values of the
     * parent instance.
     */
    public void resolveAttributes() {
        for (String name : attributes.keySet()) {
            String value = attributes.get(name);
            if (value.startsWith("$")) {
                String key = value.substring(1);

                // // The following code searches the value in all the ancestors and not only the
                // parent instance:
                // String valueFound = null;
                // for (InstanceAdapter ancestor = _parent;
                // ancestor != null;
                // ancestor = ancestor._parent)
                // {
                // valueFound = ancestor._attributes.get(key);
                // if (valueFound!=null) break;
                // }

                // The following code searches the value only in the parent instance:
                String valueFound = _parent.attributes.get(key);

                if (valueFound != null)
                    attributes.put(name, valueFound);
                else
                    throw new InconsistentModelError(
                            String.format("Attribute %s not found for instance %s", value, getFullName()));
            }
        }

        // Do the same thing with PINFO values
        for (String name : pinfos.keySet()) {
            String value = pinfos.get(name);
            if (value.startsWith("$")) {
                String key = value.substring(1);
                String valueFound = _parent.pinfos.get(key);

                if (valueFound != null)
                    pinfos.put(name, valueFound);
                else
                    throw new InconsistentModelError(
                            String.format("PINFO value %s not found for instance %s", value, getFullName()));
            }
        }
    }

    public void resolveVariableAliases() {
        for (Entry<String, VariableReference> alias : _parent._variableAliases.entrySet()) {
            if (alias.getValue().instance == this) {
                String virtualName = alias.getKey();
                String realName = alias.getValue().variableName;
                String initalValue = _parent.variables.get(virtualName);
                if (initalValue == null)
                    throw new InconsistentModelError(String.format(
                            "Initial value of variable '%s' (alias '%s') not found for instance '%s', should be defined in parent instance '%s'",
                            virtualName, realName, getFullName(), _parent.getFullName()));
                variables.put(realName, initalValue);
            }
        }
    }

}