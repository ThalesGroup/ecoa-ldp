/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class DoubleValue extends Value {
    double v;

    DoubleValue(TypeDefinition type, double value) {
        this.type = type;
        this.v = value;
    }

    @Override
    public String toString() {
        return Double.toString(v);
    }
}