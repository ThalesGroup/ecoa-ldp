/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.sf.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

public class TextWriter {

    protected PrintWriter w;
    int depth = 0;
    String specialIdAttribute = "xmlID";

    public TextWriter(File file) throws FileNotFoundException {
        FileOutputStream s = new FileOutputStream(file);
        w = new PrintWriter(s);
    }

    protected void writeValue(Object obj) {
        w.append(obj.toString());
    }

    protected void beforeField(String name) {
        w.append('\n');
        for (int i = depth; i > 0; i--)
            w.append(' ');
        w.append(name);
        w.append(':');
    }

    void beginObject(String id) {
        if (id != null)
            w.append(id);
    }

    void endObject() {
    }

    public void close() throws Exception {
        // if (!duplicateXmlIds.isEmpty()) {
        // error("Duplicate xmlIDs in %s: %s", file.getName(), writer.duplicateXmlIds.toString());
        // }
        w.close();
    }

    public void write(Object obj, String name, boolean asRef) throws Exception {
        if (obj != null) {
            if (obj instanceof AbstractFormalismObject) {
                beforeField(name);
                writeObject((AbstractFormalismObject) obj, asRef);
            } else {
                if (!AbstractFormalismObject.isDefaultValue(obj)) {
                    beforeField(name);
                    writeValue(obj);
                }
            }
        }
    }

    public void writeObject(AbstractFormalismObject obj, boolean asRef) throws Exception {
        if (obj != null) {
            if (asRef) {
                writeValue(obj);
            } else {
                String id = null;
                try {
                    if (specialIdAttribute != null && obj.getClass().getField(specialIdAttribute) != null)
                        id = obj.toString();
                } catch (NoSuchFieldException | SecurityException e) {
                }
                beginObject(id);
                depth++;
                // if (allObjects.add(obj) == true) {
                // // TODO: error
                // }
                for (Field f : obj.getObjectFields()) {

                    if (!f.getName().equals(specialIdAttribute)) {
                        Object value = f.get(obj);
                        final boolean asRefField = AbstractFormalismObject.isReference(f);

                        if (value instanceof List) {
                            final List<?> list = (List<?>) value;
                            for (Object v : list) {
                                write(v, f.getName(), asRefField);
                            }
                        } else {
                            write(value, f.getName(), asRefField);
                        }
                    }
                }
                depth--;
                endObject();
            }
        }
    }

}
