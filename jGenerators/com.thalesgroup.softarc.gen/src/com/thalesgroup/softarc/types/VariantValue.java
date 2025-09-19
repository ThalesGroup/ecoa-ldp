/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class VariantValue extends RecordValue {
    VariantValue(TypeDefinition t) {
        super(t);
    }

    Value selector;
    Value union;
    String unionName;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('{');
        buf.append(selector.toString());
        for (Value element : fields) {
            buf.append(',');
            buf.append(element.toString());
        }
        if (union != null) {
            buf.append(',');
            buf.append(union.toString());
        }
        buf.append('}');
        return buf.toString();
    }
}