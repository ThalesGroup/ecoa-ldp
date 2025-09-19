/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s91.genbuild;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;
import java.io.IOException;

import com.thalesgroup.softarc.tools.ReportStatus;
import com.thalesgroup.softarc.tools.Utilities;

public class GenBuildLDP extends AbstractGenerationPass {

    @Override
    public void execute() throws IOException {

        File gendir = context.workspace.getGenDir();

        // build tool: make
        generateFile(new File(gendir, "makefile"), "templates/core/makefile");

        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("LIB_IMPLEMENTATIONS", context.system.getComponents().stream().filter(c -> c.getIsLibrary())
                .map(c -> c.getTypeName() + "/" + c.getImplName()).collect(Collectors.toList()));
        attributes.put("CMP_IMPLEMENTATIONS", context.system.getComponents().stream().filter(c -> !c.getIsLibrary())
                .map(c -> c.getTypeName() + "/" + c.getImplName()).collect(Collectors.toList()));

        createFileFromTemplate(new File(gendir, "makefile_dirs"), "core/makefile_dirs", "makefile_dirs", attributes);
    }

    private void generateFile(File file, String resourcePath) throws IOException {
        if (file.exists())
            report(file, ReportStatus.PRESERVED);
        else
            report(file, Utilities.createFileFromStream(file, getClass().getClassLoader().getResourceAsStream(resourcePath)));
    }
}
