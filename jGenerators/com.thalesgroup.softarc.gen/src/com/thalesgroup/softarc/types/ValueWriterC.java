/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.util.Iterator;

import com.thalesgroup.softarc.sf.Parameter;

public class ValueWriterC extends ValueWriter {

    ValueWriterC() {
    }

    protected void append(Value v) throws Exception {
    	
    	 if (v.type.getIsNumeric()) {
            if (v.type.getRealType().getName().equals("int64")) {
                IntegerValue vv = (IntegerValue) v;
                // Repeat after me: "C is an evil language."
                // https://stackoverflow.com/questions/60323203/c-warningclang-compiler-integer-literal-is-too-large-to-be-represented-in-a-s
                if (vv.v == -9223372036854775808L) {
                    sb.append("(-9223372036854775807LL - 1LL)");
                } else {
                    super.append(v);
                    sb.append("LL");
                }
            }
            else if (v.type.getRealType().getName().startsWith("uint")) {
                super.append(v);
                sb.append('U');
            }
            else if (v.type.getRealType().getName().equals("float32")) {
                super.append(v);
                sb.append('F');
            }
            else {
                super.append(v);
            }
        }

        else if (v.type.getIsArray()) {
            Value tab[];
            int size = ((ArrayValue) v).v.length;
            ListWriter l = new ListWriter(arrayStart, arrayStop);

            /* Add 0 at the end of char8 array */
            if ((((ArrayValue) v).v.length < v.type.getArraySize()) && v.type.getIsChar8Array()) {
                size += 1;
                tab = new Value[size];
                for (int i = 0; i < (size - 1); i++) {
                    tab[i] = ((ArrayValue) v).v[i];
                }
                tab[size - 1] = new IntegerValue(v.type.getBaseType(), 0);
            } else {
                tab = new Value[size];
                for (int i = 0; i < size; i++) {
                    tab[i] = ((ArrayValue) v).v[i];
                }
            }
            l.next();
            sb.append(tab.length);
            l.next();
            super.append(v);
            l.end();
        }

        else if (v.type.getIsFixedArray()) {
            sb.append(recordStart);
            super.append(v);
            sb.append(recordStop);
        }

        else if (v.type.getIsVariantRecord()) {
            VariantValue vv = (VariantValue) v;
            ListWriter l = new ListWriter("{", "}");
            l.next();
            append(v.type.getSelectName(), vv.selector);
            Iterator<Parameter> it = v.type.getFields().iterator();
            for (int i = 0; i < vv.fields.length; i++) {
                l.next();
                append(it.next().getName(), vv.fields[i]);
            }
            if (vv.union != null) {
                l.next();
                ListWriter l2 = new ListWriter("{", "}");
                append(vv.unionName, vv.union);
                l2.end();
            }
            l.end();
        }

        else if (v.type.getIsEnum()) {
            append (new IntegerValue(v.type.getRealType(), ((EnumerationValue) v).v.getValnum()));
        }

        else if (v.type.getIsString()) {
            StringValue value = (StringValue) v;
            sb.append("{");

            /* max_length */
            sb.append(value.type.getLength() + "+1");
            sb.append(", ");

            /* current_length */
            sb.append(value.v.length());
            sb.append(", ");

            /* data */
            sb.append("\"" + value.v + "\"");

            sb.append("}");
        }

        else {
            super.append(v);
        }
    }

    protected void append(String name, Value value) throws Exception {
        sb.append('.');
        sb.append(name);
        sb.append('=');
        append(value);
    }

}
