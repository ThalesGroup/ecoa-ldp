/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class IntegerValue extends Value {
    long v;

    IntegerValue(TypeDefinition type, long value) {
        this.type = type;
        this.v = value;
    }

    @Override
    public String toString() {
        if (type.getName().equals("char8")) {
            if (v < 32 || v > 125) {
                return Long.toString(v);
            } else if (v == '\'') {
                return "'\\''";
            } else {
                return "\'" + (char) v + '\'';
            }
        } else {
            return Long.toString(v);
        }
    }

    @Override
    public long toIntegerKey() {
        return v;
    }
}