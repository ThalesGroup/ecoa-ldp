/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.util.Collection;
import java.util.Iterator;

import com.thalesgroup.softarc.sf.Parameter;

/**
 * Write a value to a string using the same syntax as the one in class {@link ValueReader}.
 * 
 * This class is also the base class for writers with other syntaxes (ex: C syntax, Ada syntax, etc.)
 */

public class ValueWriter {

    protected String recordStop = "}";
    protected String recordStart = "{";
    protected String arrayStop = "}";
    protected String arrayStart = "{";

    public String write(Value v) throws Exception {
        clear();
        append(v);
        return sb.toString();
    }

    public void clear() {
        sb.setLength(0);
    }

    protected StringBuilder sb = new StringBuilder();

    public String toString() {
        return sb.toString();
    }

    protected void append(Value v) throws Exception {

        if (v.type.getIsPredef() || v.type.getIsSimple()) {
            sb.append(v.toString());
        }

        else if (v.type.getIsArray() || v.type.getIsFixedArray()) {
            ListWriter l = new ListWriter(arrayStart, arrayStop);
            for (Value element : ((ArrayValue) v).v) {
                l.next();
                append(element);
            }
            l.end();
        }

        else if (v.type.getIsRecord()) {
            ListWriter l = new ListWriter(recordStart, recordStop);
            appendFields(v.type.getFields(), ((RecordValue) v).fields, l);
            l.end();
        }

        else if (v.type.getIsVariantRecord()) {
            VariantValue vv = (VariantValue) v;
            ListWriter l = new ListWriter(recordStart, recordStop);
            l.next();
            append(v.type.getSelectName(), vv.selector);
            appendFields(v.type.getFields(), vv.fields, l);
            if (vv.union != null) {
                l.next();
                append(vv.unionName, vv.union);
            }
            l.end();
        }

        else if (v.type.getIsEnum()) {
            com.thalesgroup.softarc.sf.EnumValue ev = ((EnumerationValue) v).v;
            if (ev != null)
                sb.append(ev.getName());
        }

        else if (v.type.getIsString()) {
            StringValue value = (StringValue) v;
            // buf.append("{");
            //
            // /* max_length */
            // buf.append(v.type.getLength() + "+1");
            // buf.append(", ");
            //
            // /* current_length */
            // buf.append(value.v.length());
            // buf.append(", ");
            //
            // /* data */
            sb.append("\'" + value.v + "\'");

            // buf.append("}");
        }

        else  {
            // list, map, opaque ==> an empty array
            sb.append(arrayStart);
            sb.append(arrayStop);
        }
    }

    protected void appendFields(Collection<Parameter> types, Value[] values, ListWriter l) throws Exception {
        Iterator<Parameter> i = types.iterator();
        for (Value element : values) {
            l.next();
            append(i.next().getName(), element);
        }
    }

    protected void append(String name, Value v) throws Exception {
        appendFieldName(name);
        append(v);
    }

    protected void appendFieldName(String name) {
    }

    /**
     * Utility class to simplify the writing of lists in Writers. Just call {@link #next()} before each element (to print the
     * separator if needed), and {@link end()} at the end of the list.
     * 
     * The separator, opening and closing strings can be customised if needed.
     * 
     */
    protected class ListWriter {
        char separator = ',';
        String closing;
        int i;

        ListWriter(String opening, String closing) {
            sb.append(opening);
            this.closing = closing;
            i = 0;
        }

        void end() {
            sb.append(closing);
        }

        void next() {
            if (i++ != 0)
                sb.append(separator);
        }
    }
}
