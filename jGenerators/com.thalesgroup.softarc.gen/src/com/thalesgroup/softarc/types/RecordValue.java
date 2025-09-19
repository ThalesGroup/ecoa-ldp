/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

class RecordValue extends Value {
    RecordValue(TypeDefinition t) {
        type = t;
        this.fields = new Value[t.getFields().size()];
    }

    Value[] fields;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('{');
        int i = 0;
        for (Value element : fields) {
            if (i++ != 0)
                buf.append(',');
            buf.append(element.toString());
        }
        buf.append('}');
        return buf.toString();
    }
}