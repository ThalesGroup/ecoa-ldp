/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.math.BigInteger;
import com.thalesgroup.softarc.sf.TypeDefinition;

class UnsignedInt64Value extends Value {
    BigInteger v;

    UnsignedInt64Value(TypeDefinition type, BigInteger value) {
        this.type = type;
        this.v = value;
    }

    @Override
    public String toString() {
        return v.toString();
    }
}