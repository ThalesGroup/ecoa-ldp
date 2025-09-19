/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import technology.ecoa.model.componenttype.CTComponentType;
import technology.ecoa.model.datatype.CTLibrary;
import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.implementation.CTImplementation;
import technology.ecoa.model.assembly.ASAssembly;

import com.thalesgroup.softarc.tools.AbstractLogger;
import com.thalesgroup.softarc.tools.Requirement;

public class ModelLoader {

    public final Workspace workspace;
    public final AbstractLogger logger;

    public ModelLoader(Workspace workspace) {
        this.logger = workspace.logger;
        this.workspace = workspace;
    }

    private Map<String, Library> libraries = new HashMap<String, Library>();

    /**
     * Loads an assembly, indexes the named parts of the assembly model and returns a wrapper to the JAXB class {@link ASAssembly}
     * .
     * 
     * @param filename
     *            the name of the file without path.
     * @return a wrapper to the JAXB class {@link ASAssembly}.
     * @throws IOException
     *             when the file doesn't exists.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-135", "GenFramework-SRS-REQ-092" })
    public Assembly loadAssembly(String name) throws IOException {

        File file = workspace.getAssemblyFile(name);

        return new Assembly(Models._assembly.load(file, logger), file, null);
    }

    public Assembly loadAssemblyDeeply(File file) throws IOException {
        return new Assembly(Models._assembly.load(file, logger), file, this);
    }

    public Assembly loadAssemblyDeeply(String name) throws IOException {
        return loadAssemblyDeeply(workspace.getAssemblyFile(name));
    }

    /**
     * Saves a model into the specified output file.
     * 
     * @param assembly
     *            Input model to save
     * @param outputFile
     *            Output file
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-142" })
    public void saveAssembly(ASAssembly assembly, File outputFile) throws IOException {
        Models._assembly.save(new technology.ecoa.model.assembly.ObjectFactory().createAssembly(assembly),
                outputFile);
    }

    /**
     * Saves a model into the specified output file.
     * 
     * @param assembly
     *            Input model to save
     * @param outputFile
     *            Output file
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-142" })
    public void saveImplementation(CTImplementation impl, File outputFile) throws IOException {
        Models._implementation.save(new technology.ecoa.model.implementation.ObjectFactory().createImplementation(impl),
                outputFile);
    }

    /**
     * Saves a model into the specified output file.
     * 
     * @param deployment
     *            Input model to save
     * @param outputFile
     *            Output file
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-142" })
    public void saveDeployment(DEApplication deployment, File outputFile) throws IOException {
        Models._deployment.save(new technology.ecoa.model.deployment.ObjectFactory().createApplication(deployment),
                outputFile);
    }

    /**
     * Saves a model into the specified output file.
     * 
     * @param component
     *            Input model to save
     * @param outputFile
     *            Output file
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-142" })
    public void saveComponent(CTComponentType component, File outputFile) throws IOException {
        Models._componenttype.save(new technology.ecoa.model.componenttype.ObjectFactory().createComponentType(component),
                outputFile);
    }

    /**
     * Saves a model into the specified output file.
     * 
     * @param library
     *            Input model to save
     * @param outputFile
     *            Output file
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-142" })
    public void saveLibrary(CTLibrary library, File outputFile) throws IOException {
        Models._library.save(new technology.ecoa.model.datatype.ObjectFactory().createLibrary(library), outputFile);
    }

    /**
     * Loads a deployment, indexes the named parts of the deployment model and returns a wrapper to the JAXB class
     * {@link DEApplication}.
     * 
     * @param filename
     *            the name of the file without path.
     * @return a wrapper to the JAXB class {@link DEApplication}.
     * @throws IOException
     *             when the file doesn't exists.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-135", "GenFramework-SRS-REQ-095" })
    public Deployment loadDeployment(String name) throws IOException {
        workspace.currentDeploymentName = name;
        File file = workspace.getDeploymentFile(name);

        return new Deployment(Models._deployment.load(file, logger));
    }

    public Deployment loadDeployment(File file) throws IOException {
        workspace.initFromDeploymentFile(file);

        return new Deployment(Models._deployment.load(file, logger));
    }

    /**
     * Loads a component model from a given file path, indexes the named parts of the component model and returns a wrapper to the
     * JAXB class {@link CTComponentype}.
     * 
     * @param file
     *            Component file to load
     * @return a wrapper to the JAXB class {@link CTComponentType}.
     * @throws IOException
     *             when the file doesn't exists.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-135" })
    public ComponentType loadComponent(String name) throws IOException {
        File file = workspace.getComponentType(name, false);
        return new ComponentType(name, Models._componenttype.load(file, logger));
    }

    public ComponentType loadComponentTypeDeeply(String name) throws IOException {
        File file = workspace.getComponentType(name, false);
        ComponentType componentType = new ComponentType(name, Models._componenttype.load(file, logger));
        componentType.loadSubLibraries(this);
        return componentType;
    }

    /**
     * Loads a library model, indexes the named parts of the library model and returns a wrapper to the JAXB class
     * {@link CTLibrary}.
     * 
     * @param lp
     *            the logical path of the component type, must denote a library directory
     * @return a wrapper to the JAXB class {@link CTLibrary}.
     * @throws IOException
     *             when the file doesn't exists.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-135", "GenFramework-SRS-REQ-106" })
    public Library loadLibrary(String name) throws IOException {
        Library library = libraries.get(name);
        if (library == null) {
            library = new Library(name, Models._library.load(workspace.getComponentType(name, true), logger));
            libraries.put(name, library);
            library.loadSubLibraries(this);
        }
        return library;
    }

    /**
     * Loads an implementation model from componentTypeName and implementationName.
     * 
     * @param compName
     *            the name of the component type, must denote a component type directory
     * @param implementationName
     *            the name of the specific implementation, must denote an implementation directory
     * @return a wrapper to the JAXB class {@link CTImplementation}.
     * @throws IOException
     *             when the file doesn't exists.
     */
    public Implementation loadImplementation(String compName, String implementationName) throws IOException {
        File file = workspace.getComponentImpl(compName, implementationName);
        
        // composite implementation is optional: if no implementation file exists but an internal assembly file exists,
        // the component is assumed to be a composite
		if (!file.exists() && workspace.getInternalAssembly(compName, implementationName).exists())
			return new Implementation(null, compName, implementationName);
		else
			return new Implementation(Models._implementation.load(file, logger), compName, implementationName);
    }

    public Implementation loadImplementation(File file) throws IOException {
        return new Implementation(Models._implementation.load(file, logger), file.getParentFile().getParentFile().getName(),
                file.getParentFile().getName());
    }

}