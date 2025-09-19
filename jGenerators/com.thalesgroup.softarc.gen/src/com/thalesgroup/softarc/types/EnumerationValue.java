/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class EnumerationValue extends Value {
    com.thalesgroup.softarc.sf.EnumValue v;
    EnumWrapper enumWrapper;

    public EnumerationValue(EnumWrapper enumWrapper, TypeDefinition type, com.thalesgroup.softarc.sf.EnumValue v) {
        assert enumWrapper != null;
        assert type != null;
        assert v != null;
        this.type = type;
        this.enumWrapper = enumWrapper;
        this.v = v;
    }

    @Override
    public String toString() {
        if (v == null)
            return "";
        return v.getName();
    }

    @Override
    public long toIntegerKey() {
        return v.getValnum();
    }
}