/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.sf.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XmlWriter {

    public XmlWriter(File file) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError {
        FileOutputStream s = new FileOutputStream(file);
        w = XMLOutputFactory.newFactory().createXMLStreamWriter(s, "UTF-8");
    }

    public void close() throws XMLStreamException {
        w.close();
    }

    XMLStreamWriter w;
    int depth = 0;
    String specialIdAttribute = null;

    // private boolean writeAsAttributeIfPossible(Object obj, String name)
    // throws XMLStreamException, IllegalArgumentException, IllegalAccessException {
    // if (obj != null && !AbstractFormalismObject.isDefaultValue(obj)) {
    // // if (obj instanceof AbstractFormalismObject) {
    // // } else {
    // // w.writeAttribute(name, obj.toString());
    // // }
    // return false;
    // }
    // return true;
    // }

    protected void beforeField(String name) throws Exception {
        w.writeCharacters("\n");
        for (int i = depth; i > 0; i--)
            w.writeCharacters(" ");
        w.writeStartElement(name);
    }

    void afterField() throws Exception {
        w.writeEndElement();
    }

    void beginObject(String id) throws Exception {
        if (id != null)
            w.writeAttribute("xmlID", id);
    }

    void endObject() throws Exception {
    }

    void writeValue(Object obj) throws Exception {
        w.writeCharacters(obj.toString());
    }

    public void write(Object obj, String name, boolean asRef) throws Exception {
        if (obj != null) {
            if (obj instanceof AbstractFormalismObject) {
                beforeField(name);
                writeObject((AbstractFormalismObject) obj, asRef);
                afterField();
            } else {
                if (!AbstractFormalismObject.isDefaultValue(obj)) {
                    beforeField(name);
                    writeValue(obj);
                    afterField();
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
                    if (specialIdAttribute != null && obj.getClass().getDeclaredField(specialIdAttribute) != null)
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
                        final boolean fieldAsRef = AbstractFormalismObject.isReference(f);

                        String name = f.getName().replace("$", "");
                        if (value instanceof List) {
                            final List<?> list = (List<?>) value;
                            for (Object v : list) {
                                write(v, name, fieldAsRef);
                            }
                        } else {
                            write(value, name, fieldAsRef);
                        }
                    }
                }
                depth--;
                endObject();
            }
        }
    }
}
