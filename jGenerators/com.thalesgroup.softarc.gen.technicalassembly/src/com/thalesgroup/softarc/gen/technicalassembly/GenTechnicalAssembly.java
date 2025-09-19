/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.io.File;
import java.io.IOException;

import com.thalesgroup.ecoa.model.Assembly;
import com.thalesgroup.ecoa.model.Deployment;
import com.thalesgroup.ecoa.model.ModelLoader;
import com.thalesgroup.ecoa.model.Workspace;

import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.assembly.ASAssembly;

import com.thalesgroup.softarc.tools.AbstractGenerator;
import com.thalesgroup.softarc.tools.ReportStatus;

/**
 * This generators transforms an Assembly to a Technical Assembly, i.e an assembly: - without the concept of composite. - where
 * the operation links (datalink, eventlink and service) have a unique numeric identifier
 */
public final class GenTechnicalAssembly extends AbstractGenerator {

    Workspace workspace = new Workspace(this);
    public ModelLoader modelLoader = new ModelLoader(workspace);

    RootInstance _rootInstance;

    boolean DO_NOT_USE_DEPLOYMENT = false; // used for testing GTA only

    /**
     * Create the technical assembly from an original assembly.
     * 
     * @param model The original assembly.
     * @param assemblyFile 
     * @return The technical assembly.
     * @throws Exception
     * @throws IOException
     */
    private ASAssembly createTechnicalAssembly(Assembly model, DEApplication deployment, File assemblyFile) throws Exception {

        // build internal model of assembly (wires)
        _rootInstance = new RootInstance(model, this, assemblyFile);
        _rootInstance.resolveAttributes();

        _rootInstance.resolveVariableAliases();

        // _rootInstance.dump();
        _rootInstance.flattenInstances();
        _rootInstance.removeIdenticalWires();

        if (!DO_NOT_USE_DEPLOYMENT) {
            _rootInstance.removeUnusedExternalOperations(deployment);
            _rootInstance.checkExternalRequestResponseOperations(deployment);
            _rootInstance.duplicateExternalRequestResponseLinks(deployment);
        }

        _rootInstance.dump();

        return new OutputWriter().createTechnicalAssembly(_rootInstance);
    }

    public GenTechnicalAssembly() throws Exception {
        super("GenTechnicalAssembly", null);
        addArgument(ARGUMENT_KEY_DEPLOYMENT, ARGUMENT_KEY_SHORT_DEPLOYMENT, File.class, 1, 1, "Specifies deployment model file.",
                null);
        getArguments().setOptionActivated(ARGUMENT_KEY_DEPLOYMENT, true);
    }

    @Override
    protected void initialize() {
    }

    @Override
    public void generate() throws Exception {
        // Load deployment (this will automatically load the assembly)
        File deploymentFile = getArguments().getFirst(ARGUMENT_KEY_DEPLOYMENT);
        Deployment deploymentModel = modelLoader.loadDeployment(deploymentFile);
        File assemblyFile = workspace.getAssemblyFile(deploymentModel.getDeployment().getAssembly());
        Assembly assembly = modelLoader.loadAssemblyDeeply(assemblyFile);

        // Generate technical assembly
        ASAssembly technicalAssembly = createTechnicalAssembly(assembly, deploymentModel.getDeployment(), assemblyFile);

        // Save technical assembly
        // REQ-003
        File outputAssemblyFile = workspace.getTechnicalAssembly();
        modelLoader.saveAssembly(technicalAssembly, outputAssemblyFile);
        report(outputAssemblyFile, ReportStatus.CREATED);
    }

    public static void main(String[] args) throws Exception {
        AbstractGenerator generator = new GenTechnicalAssembly();
        int rc = generator.execute(args);
        System.exit(rc);
    }

    public static final String ARGUMENT_KEY_DEPLOYMENT = "deployment";
    public static final char ARGUMENT_KEY_SHORT_DEPLOYMENT = 'd';
}
