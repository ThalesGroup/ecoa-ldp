/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

public class ValueWriterCPP extends ValueWriterC {

    protected void append(String name, Value value) throws Exception {
        // in C++, the syntax { .fieldname = value , ... }
        // for struct/union initialisation is not allowed.
        // using syntax: { fieldname: value , ... }
        sb.append(name);
        sb.append(':');
        append(value);
    }

    protected void append(Value v) throws Exception {
        if (v.type.getIsPredef()) {
            if (v.type.getName().equals("boolean8"))
                sb.append(((IntegerValue) v).v == 0 ? "false" : "true");
            else
                super.append(v);
        }

        else if (v.type.getIsSimple() || v.type.getIsEnum()) {
            sb.append("{");
            if (v.type.getName().equals("boolean8"))
                sb.append(((IntegerValue) v).v == 0 ? "false" : "true");
            else
                super.append(v);
            sb.append("}");
        }

        else {
            super.append(v);
        }
    }
}
