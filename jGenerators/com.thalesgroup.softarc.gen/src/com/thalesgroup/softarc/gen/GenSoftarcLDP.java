/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.thalesgroup.ecoa.model.Workspace;
import com.thalesgroup.softarc.gen.common.AbstractGenSoftarc;
import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.PassContext;
import com.thalesgroup.softarc.tools.CommandLineParsingError;

public final class GenSoftarcLDP extends AbstractGenSoftarc {

    // Use this for debug:
    {
        // debugOption_skipGeneration = true;
        // debugOption_restartFromPass = com.thalesgroup.softarc.gen.s90.buildinfo.BuildInfo.class;
    }

    //@formatter:off
    private static final List<Class<? extends AbstractPass>> stepClasses = new ArrayList<Class<? extends AbstractPass>>(Arrays.asList(
            com.thalesgroup.softarc.gen.s10.importcomponents.ImportComponentsCore.class,
            com.thalesgroup.softarc.gen.s20.xref.XRefCore.class,
            com.thalesgroup.softarc.gen.s27.ctypes.CTypes.class,
            com.thalesgroup.softarc.gen.s30.gentype.GenType.class,
            com.thalesgroup.softarc.gen.s30.gentype.GenInitialize.class,
            com.thalesgroup.softarc.gen.s40.importassembly.ImportAssembly.class,
            com.thalesgroup.softarc.gen.s43.checksignatures.CheckSignatures.class,
            com.thalesgroup.softarc.gen.s50.thread.GenThreadLDP.class,
            com.thalesgroup.softarc.gen.s51.entrypoints.EntryPoints.class,
            com.thalesgroup.softarc.gen.s53.operationlinks.OperationLinks.class,
            com.thalesgroup.softarc.gen.s55.containers.ContainersLDP.class,
            com.thalesgroup.softarc.gen.s59.translatevalues.TranslateValues.class,
            com.thalesgroup.softarc.gen.s70.main.core.GenSerialize04.class,
            com.thalesgroup.softarc.gen.s70.main.core.GenMain.class,
            com.thalesgroup.softarc.gen.s91.genbuild.GenBuildLDP.class 
            ));
    //@formatter:on

    @Override
    public void generate() throws Exception {

        File deploymentFile = getArguments().getFirst(ARGUMENT_KEY_DEPLOYMENT);

        Workspace workspace = new Workspace(this);
        workspace.initFromDeploymentFile(deploymentFile);

        PassContext context = new PassContext(workspace);
        context.isLDP = true;
        
        context.loadFiles();

        super.generate(context, stepClasses);
    }

    public static final String ARGUMENT_KEY_DEPLOYMENT = "deployment";

    public GenSoftarcLDP() throws Exception {
        super("GenSoftarc", "templates");

        addArgument(ARGUMENT_KEY_DEPLOYMENT, 'd', File.class, 0, 1, "Specifies deployment model file.", null);
        getArguments().setOptionActivated(ARGUMENT_KEY_DEPLOYMENT, true);
    }

    @Override
    protected void initialize() throws Exception {
        super.initialize();

        File file = null;
        if (_arguments.hasArgument(ARGUMENT_KEY_DEPLOYMENT)) {
            file = _arguments.getFirst(ARGUMENT_KEY_DEPLOYMENT);
        }
        if (file == null) {
            throw new CommandLineParsingError(
                    "Generator needs at least one input model file in order to retrieve project's root directory.");
        }
        info("Using deployment %s", file.getPath());
    }

    // =========================================================================
    // Generator main entry point
    // =========================================================================

    public static void main(String args[]) throws Exception {
        GenSoftarcLDP generator = new GenSoftarcLDP();
        generator.execute(args);
    }

}
