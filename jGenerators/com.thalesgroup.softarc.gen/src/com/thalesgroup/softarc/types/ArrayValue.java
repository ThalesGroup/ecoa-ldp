/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.util.Arrays;

import com.thalesgroup.softarc.sf.TypeDefinition;

class ArrayValue extends Value {
    ArrayValue(TypeDefinition t, int size) {
        type = t;
        this.v = new Value[size];
    }

    Value[] v;

    void resize(int size) {
        this.v = Arrays.copyOf(this.v, size);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('{');
        int i = 0;
        for (Value element : v) {
            if (i++ != 0)
                buf.append(',');
            buf.append(element.toString());
        }
        buf.append('}');
        return buf.toString();
    }
}