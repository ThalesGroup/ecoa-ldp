/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.sf.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.thalesgroup.softarc.tools.json.JsonWriter;

public class JsonWriterFormalism implements AutoCloseable {

    private final JsonWriter w;
    private final BufferedWriter out;

    public JsonWriterFormalism(File file) throws IOException {
        out = new BufferedWriter(new FileWriter(file), 32 * 1024);
        w = new JsonWriter(out);
    }

    public void write(Object obj, boolean asRef) throws Exception {
        if (obj != null) {
            if (obj instanceof AbstractFormalismObject) {
                writeObject((AbstractFormalismObject) obj, asRef);
            } else {
                writeValue(obj);
            }
        }
    }

    private void writeObject(AbstractFormalismObject obj, boolean asRef) throws Exception {
        if (asRef) {
            writeValue(obj);
        } else {
            w.beginObject();
            for (Field f : obj.getObjectFields()) {

                Object value = f.get(obj);
                if (!AbstractFormalismObject.isDefaultValue(value)) {
                    w.beforeField(f.getName());
                    final boolean fieldAsRef = AbstractFormalismObject.isReference(f);

                    if (value instanceof List) {
                        final List<?> list = (List<?>) value;
                        w.beginList(list.size());
                        for (Object v : list) {
                            w.beforeListElement();
                            write(v, fieldAsRef);
                        }
                        w.endList();
                    } else {
                        write(value, fieldAsRef);
                    }
                }
            }
            w.endObject();
        }
    }

    private void writeValue(Object obj) throws IOException {
        if (obj instanceof Long) {
            out.append(obj.toString());
        } else if (obj instanceof Boolean) {
            out.append(obj.toString());
        } else {
            w.putString(obj.toString());
        }
    }

    @Override
    public void close() throws Exception {
        out.close();
    }

}
