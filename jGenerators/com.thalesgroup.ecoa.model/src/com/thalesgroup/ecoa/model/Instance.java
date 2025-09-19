/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import technology.ecoa.model.assembly.ASInstance;

public class Instance {

    Instance(ASInstance instance, ComponentType component, Implementation implementation) {
        _instance = instance;
        _componentType = component;
        _implementation = implementation;
    }

    public ASInstance getInstance() {
        return _instance;
    }

    /**
     * @return the name of the instance
     */

    public String getName() {
        return _instance.getName();
    }

    /**
     * @return the componentType
     */

    public ComponentType getComponentType() {
        return _componentType;
    }

    /**
     * @return the componentImplementation
     */

    public Implementation getImplementation() {
        return _implementation;
    }

    private final ASInstance _instance;

    private final ComponentType _componentType;

    private final Implementation _implementation;

    public static final String SPECIAL_INSTANCE_NAME_EXTERN = "extern";

    public boolean isExtern() {
        return getName().equals(SPECIAL_INSTANCE_NAME_EXTERN);
    }

}
