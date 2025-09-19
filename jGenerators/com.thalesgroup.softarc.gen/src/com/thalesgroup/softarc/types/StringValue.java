/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class StringValue extends Value {
    String v;

    public StringValue(TypeDefinition type, String value) {
        this.type = type;
        this.v = value;
    }

    @Override
    public String toString() {
        return '\'' + v + '\'';
    }
}