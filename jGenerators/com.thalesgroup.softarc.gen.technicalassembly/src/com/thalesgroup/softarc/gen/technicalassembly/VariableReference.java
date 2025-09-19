/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

public class VariableReference {

    public ComponentInstance instance;
    public String variableName;

    public VariableReference(ComponentInstance instance, String variableName) {
        this.instance = instance;
        this.variableName = variableName;
    }

    @Override
    public String toString() {
        return String.format("%s.%s", instance.fullName, variableName);
    }
}