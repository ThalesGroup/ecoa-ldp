/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s20.xref;

import java.util.LinkedHashSet;
import java.util.Set;

import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.TypeDefinition;

/**
 * Class which gathers a type and the list of locally-defined types it depends on
 */
class TypeAndDependencies {

    final TypeDefinition definition;
    final Set<TypeDefinition> dependencies = new LinkedHashSet<TypeDefinition>();
    final boolean locally;

    public TypeAndDependencies(TypeDefinition td, boolean locally) {
        definition = td;
        this.locally = locally;
        // examine all possible references to dependent types:
        addDependency(td.getBaseType());
        addDependency(td.getKeyType());
        for (Parameter f : td.getFields()) {
            addDependency(f.getType());
        }
        for (Parameter f : td.getUnionFields()) {
            addDependency(f.getType());
        }
        if (td.getDefaultUnionField() != null)
            addDependency(td.getDefaultUnionField().getType());
    }

    final public void addDependency(TypeDefinition otherType) {
        if (otherType != null && (!locally || otherType.getParent() == definition.getParent()))
            dependencies.add(otherType);
    }
}