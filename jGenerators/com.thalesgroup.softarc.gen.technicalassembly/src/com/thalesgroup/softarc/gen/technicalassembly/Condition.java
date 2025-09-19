/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import technology.ecoa.model.assembly.ASWhenCondition;

public class Condition implements java.lang.Comparable<Condition> {

    public ASWhenCondition model;
    public VariableReference variable;
    String text;

    public Condition(ASWhenCondition model, ComponentInstance instance) {
        this.model = model;
        variable = new VariableReference(instance, model.getVariable());
        text = String.format("%s.%s=%s", instance.getFullName(), model.getVariable(), model.getValue());
    }

    public int compareTo(Condition w) {
        return text.compareTo(w.text);
    }
}