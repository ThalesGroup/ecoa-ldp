/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.thalesgroup.softarc.tools.AbstractLogger;

/**
 * This class models the standard file hierarchy of a SOFTARC workspace, defined as an extension of the ECOA Standard workspace.
 * <p>
 * Is is associated to a given root directory.
 * <p>
 * From logical names, it returns access to {@link java.io.File} objects corresponding to each kind of file or directory defined
 * in a SOFTARC workspace.
 * <p>
 * The standard file hierarchy can be customized with an optional configuration file "workspace.xml".
 */
public class Workspace extends WorkspaceStandard {

    protected static final String EXTENSION_PROPERTIES = ".properties.xml";

    public String currentDeploymentName;

    /**
     * Create a workspace, with no defined root directory.
     * Before the workspace can be used, the root directory must be initialized 
     * by calling setRoot() or any init*() method.
     */
    public Workspace() {
        super();
    }

    public Workspace(File root) throws IOException {
        super(root);
    }

    public Workspace(AbstractLogger logger) {
        this.logger = logger;
    }


    public File getProductionPropertiesFile(String deploymentName) {
        return new File(getDeploymentsDir(), deploymentName + EXTENSION_PROPERTIES);
    }

    public File getIntegrationDir() {
        return dir04;
    }

    /**
     * The generation directory corresponding to the current deployment, i.e. 04-Integration/[deploymentname]
     */
    public File getGenDir() {
        if (currentDeploymentName == null)
            throw new Error("No current deployment is defined");
        return new File(getIntegrationDir(), currentDeploymentName);
    }

    public File getExeDir(String exeName) {
        return new File(getGenDir(), exeName);
    }

    public File getRuntimeDir() {
        return new File(getGenDir(), "_run");
    }

    public File getTechnicalAssembly() {
        return new File(getGenDir(), "TechnicalAssembly.xml");
    }

    public File getMapping() {
        return new File(getGenDir(), "Mapping.xml");
    }

    public File getConfigFile() {
        return new File(getGenDir(), "Config.xml");
    }

    /**
     * @param file
     *            implementation.xml file Try to find the root of the workspace by searching parent directories of the given file.
     * @throws IOException
     *            when a workspace root cannot be found, or if workspace.xml is incorrect
     */
    public void initFromAnyIncludedFile(File file) throws IOException {
        currentDeploymentName = null;
        File f = file.getAbsoluteFile();
        while (!isPossibleRoot(f)) {
            if (f == null) {
                throw new FileNotFoundException("This file is not in an ECOA workspace: " + file);
            }
            f = f.getParentFile();
        }
        setRoot(f);
    }

    public String initFromAssemblyFile(File file) throws IOException {
        if (!file.getName().endsWith(EXTENSION_ASSEMBLY))
            logger.error("Name of assembly file '%s' shall end with '%s'", file.getName(), EXTENSION_ASSEMBLY);
        currentDeploymentName = null;
        if (!file.exists())
            throw new FileNotFoundException(file.getPath());
        setRoot(file.getAbsoluteFile().getParentFile().getParentFile());
        return file.getName().replace(EXTENSION_ASSEMBLY, "");
    }

    public void initFromDeploymentFile(File file) throws IOException {
        if (!file.getName().endsWith(EXTENSION_DEPLOYMENT))
            logger.error("Name of deployment file '%s' shall end with '%s'", file.getName(), EXTENSION_DEPLOYMENT);
        if (!file.exists())
            throw new FileNotFoundException(file.getPath());
        setRoot(file.getAbsoluteFile().getParentFile().getParentFile());
        currentDeploymentName = (file.getName().replace(EXTENSION_DEPLOYMENT, ""));
    }

    public void initFromMappingFile(File file) throws IOException {
        File gendir = file.getParentFile();
        if (!file.exists())
            throw new FileNotFoundException(file.getPath());
        setRoot(gendir.getAbsoluteFile().getParentFile().getParentFile());
        currentDeploymentName = (gendir.getName());
    }

}
