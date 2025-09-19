/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import technology.ecoa.model.workspace.CTWorkspace;
import technology.ecoa.model.componenttype.CTComponentType;
import technology.ecoa.model.datatype.CTLibrary;
import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.implementation.CTImplementation;
import technology.ecoa.model.assembly.ASAssembly;
import com.thalesgroup.softarc.tools.XmlPersistence;

import java.net.URL;

/**
 * This class contains instances of XmlPersistence needed to load/save ECOA models.
 *
 * They can be shared between several generators that execute in the same JVM. Each class is instanciated only once.
 */
public class Models {

    /**
     * XMLPersistence instance for componentType files.
     */
    public static final XmlPersistence<CTComponentType> _componenttype = new XmlPersistence<CTComponentType>(
            CTComponentType.class, getSchema("ComponentType.xsd"), null, "componenttype ");
    /**
     * XMLPersistence instance for library files.
     */
    public static final XmlPersistence<CTLibrary> _library = new XmlPersistence<CTLibrary>(CTLibrary.class,
            getSchema("SOFTARC/DataTypes.xsd"), null, "library       ");

    /**
     * XMLPersistence instance for implementation files.
     */
    public static final XmlPersistence<CTImplementation> _implementation = new XmlPersistence<CTImplementation>(
            CTImplementation.class, getSchema("SOFTARC/Implementation.xsd"), null, "implementation");

    /**
     * XMLPersistence instance for assembly and technicalassembly files.
     */
    public static final XmlPersistence<ASAssembly> _assembly = new XmlPersistence<ASAssembly>(ASAssembly.class,
            getSchema("SOFTARC/Assembly.xsd"), null, "assembly      ");
    /**
     * XMLPersistence instance for deployment files.
     */
    public static final XmlPersistence<DEApplication> _deployment = new XmlPersistence<DEApplication>(DEApplication.class,
            getSchema("SOFTARC/Deployment.xsd"), null, "deployment    ");

    /**
     * XMLPersistence instance for workspace configuration files.
     */
    public static final XmlPersistence<CTWorkspace> _workspace = new XmlPersistence<CTWorkspace>(CTWorkspace.class,
            getSchema("Workspace.xsd"), null, "workspace     ");

    /* Private part */

    private static URL getSchema(String name) {
        return Models.class.getResource("/technology/ecoa/model/xsd/" + name);
    }
}
