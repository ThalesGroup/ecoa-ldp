/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import technology.ecoa.model.deployment.DEApplication;

import technology.ecoa.model.assembly.ASAssembly;

import com.thalesgroup.ecoa.model.Models;
import com.thalesgroup.ecoa.model.Workspace;
import com.thalesgroup.softarc.sf.System;

public class PassContext {

    final public Workspace workspace;
    public System system;
    public ASAssembly ASFILE;
    public DEApplication DEFILE;
    public Map<String, Object> args = new HashMap<String, Object>();
    public HashSet<String> featureToggles = new HashSet<String>();
    public boolean isLDP;
    
    public PassContext(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Load all files that can be used by any pass. This is not done in the passes themselves, in order to allow restarting
     * generation from any pass based on a dump of the formalism.
     */
    public void loadFiles() throws Exception {
        // Deployment
        DEFILE = Models._deployment.load(workspace.getDeploymentFile(workspace.currentDeploymentName), workspace.logger);

        // Assembly
        File asFile = workspace.getTechnicalAssembly();
        // File asFile = workspace.getAssemblyFile(DEFILE.getAssembly());
        ASFILE = Models._assembly.load(asFile, workspace.logger);
    }

}
