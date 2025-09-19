/* Copyright (c) 2025 THALES -- All rights reserved */

/*
 * Copyright (c) 2011 THALES.
 * All rights reserved.
 */
package com.thalesgroup.softarc.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * Allows to load and save XML files corresponding to a given XSD schema.
 *
 * @param <E> JAXB-generated class corresponding to the parsed XML information
 */
public class XmlPersistence<E extends Object> implements ValidationEventHandler {

    public static boolean validateXsd = true;

    /**
     * @param contextPath fully qualified name of the package containing JAXB-generated classes
     * @param validationSchema URI of schema for automatic validation on load(), or null if no
     *     validation is wanted.
     * @param outputSchemaURI URI written in attribute <tt>noNamespaceSchemaLocation</tt> of the
     *     output, on save(). Or null if not wanted. Can also be set with {@link
     *     #setNoNamespaceSchemaLocation(String)}
     */
    public XmlPersistence(
            Class<E> c, URL validationSchema, String outputSchemaURI, String filetype) {
        _filetype = filetype;
        _desttype = c;

        String contextPath = c.getPackage().getName();
        try {
            JAXBContext ctx =
                    JAXBContext.newInstance(contextPath, XmlPersistence.class.getClassLoader());
            _loader = ctx.createUnmarshaller();
            _saver = ctx.createMarshaller();

            // uncomment the following lines to debug XSD schema loading:
            // if (validationSchema != null)
            // System.out.println("schema loaded for " + contextPath);
            // else
            // System.out.println("warning: no schema found for " +
            // contextPath);

            if (validationSchema != null && validateXsd) {
                Schema schema = SF.newSchema(validationSchema);
                _loader.setSchema(schema);
                _saver.setSchema(schema);
            }

            _loader.setEventHandler(this);
            _saver.setEventHandler(this);

            if (outputSchemaURI != null)
                _saver.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, outputSchemaURI);
        } catch (JAXBException e1) {
            e1.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience constructors, for compatibility with existing code. The first arg is a class
     * instead of the package name.
     *
     * @param c class corresponding to type E
     */
    public XmlPersistence(Class<E> c, URL validationSchema, String outputSchemaURI) {
        this(c, validationSchema, outputSchemaURI, null);
    }

    public XmlPersistence(Class<E> c, URL validationSchema) {
        this(c, validationSchema, null);
    }

    /** */
    @Override
    public boolean handleEvent(ValidationEvent event) {
        final ValidationEventLocator locator = event.getLocator();
        _errors.add(
                _file.toURI().getPath()
                        + ':'
                        + locator.getLineNumber()
                        + ':'
                        + locator.getColumnNumber()
                        + ':'
                        + event.getMessage());
        return event.getSeverity() == ValidationEvent.WARNING;
    }

    /** for compatibility with existing code - this method does not trace loaded files */
    @Deprecated
    public E load(File inputFile) throws IOException {
        return load(inputFile, null);
    }

    @Deprecated
    public E loadFromFile(File inputFile) throws IOException {
        return loadFromFile(inputFile, java.nio.file.Files.readAllBytes(inputFile.toPath()));
    }

    @SuppressWarnings({"unchecked"})
    public E loadFromFile(File inputFile, byte[] bytes) {
        E model = null;

        // In order to catch casting problems, one shall not use "(E) obj"
        // syntax in template code, but "Class<E>.cast(obj)" instead.

        try {
            _errors.clear();
            final Object res = _loader.unmarshal(new ByteArrayInputStream(bytes));
            if (res instanceof JAXBElement<?>) {
                Object obj = ((JAXBElement<Object>) res).getValue();
                model = _desttype.cast(obj);
            } else {
                model = _desttype.cast(res);
            }
        } catch (UnmarshalException j) {
            String message = "Syntax error in model file " + inputFile.toURI().getPath();
            for (String we : _errors) {
                message += '\n' + we;
            }
            throw new InconsistentModelError(message);
        } catch (JAXBException e) {
            throw new InconsistentModelError(e);
        } catch (ClassCastException e) {
            throw new InconsistentModelError(
                    "Unexpected content in file '" + inputFile.getPath() + "'.");
        }

        return model;
    }

    @Requirement(ids = {"GenFramework-SRS-REQ-143"})
    public E load(File inputFile, AbstractLogger generator) throws IOException {
        _file = inputFile;
        LoadedFileList<E>.LoadedModel cache = loadedFileList.get(inputFile);
        // if already loaded, use it without reloading
        if (cache.model == null) {

            if (generator != null)
                generator.info("Loading model %s : %s", _filetype, inputFile.getCanonicalPath());

            cache.model = this.loadFromFile(inputFile, cache.bytes);
        }
        // else
        // if (generator != null)
        // generator.info("Reusing (from cache) %s : %s", _filetype,
        // inputFile.getCanonicalPath());

        return cache.model;
    }

    @Requirement(ids = {"GenFramework-SRS-REQ-143"})
    public void save(JAXBElement<E> jaxbElement, OutputStream s) throws IOException {
        try {
            _errors.clear();
            _saver.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            _saver.marshal(jaxbElement, s);
        } catch (JAXBException j) {
            if (j.getCause() instanceof java.io.FileNotFoundException) {
                throw new InconsistentModelError(j.getCause());
            } else {
                StringBuffer message = new StringBuffer();
                for (String we : _errors) {
                    if (message.length() == 0)
                        message.append('\n');
                    message.append(we);
                }
                message.append(j.getMessage());
                throw new InconsistentModelError(message.toString());
            }
        }
    }

    public void save(JAXBElement<E> jaxbElement, File outputFile) throws IOException {
        if (outputFile.getParentFile() != null)
            outputFile.getParentFile().mkdirs();
        _file = outputFile;
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        save(jaxbElement, s);
        loadedFileList.create(outputFile, s.toByteArray(), jaxbElement.getValue());
    }

    /**
     * @param outputSchemaURI URI written in attribute <tt>noNamespaceSchemaLocation</tt> of the
     *     output, on save(). Or null if not wanted.
     */
    public void setNoNamespaceSchemaLocation(String outputSchemaURI) {
        try {
            _saver.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, outputSchemaURI);
        } catch (PropertyException e) {
        }
    }

    /** */
    private Unmarshaller _loader;

    private LoadedFileList<E> loadedFileList = new LoadedFileList<E>();

    /** */
    private Marshaller _saver = null;
    /** */
    private final List<String> _errors = new LinkedList<String>();
    /** */
    private File _file;

    /** Kind file that will be loaded by this object */
    private final String _filetype;

    private final Class<E> _desttype;

    private static final SchemaFactory SF =
            SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
} // class XmlPersistence
