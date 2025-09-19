/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class FloatValue extends Value {
    float v;

    FloatValue(TypeDefinition type, float value) {
        this.type = type;
        this.v = value;
    }

    @Override
    public String toString() {
        return Float.toString(v);
    }
}