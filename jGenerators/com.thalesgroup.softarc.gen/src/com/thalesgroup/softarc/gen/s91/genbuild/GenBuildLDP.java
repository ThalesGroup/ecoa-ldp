/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s91.genbuild;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;
import com.thalesgroup.softarc.sf.Extra;

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

        // collect INCDIRS and SRCDIRS from all components
        {
            LinkedHashSet<String> flags = new LinkedHashSet<>();
            for (var c : context.system.getComponents()) {
                flags.add(c.getImplDir() + "/inc");
                flags.add(c.getImplDir() + "/inc-gen");
                for (Extra dir : c.getIncdir()) {
                    flags.add(dir.getValue());
                }
                for (Extra dir : c.getSrcdir()) {
                    flags.add(dir.getValue());
                }
                if (c.getIsLibrary()) {
                }
                else {
                }
            }
            attributes.put("INCDIRS", flags);
        }
        {
            LinkedHashSet<String> flags = new LinkedHashSet<>();
            for (var c : context.system.getComponents()) {
                flags.add(c.getImplDir() + "/src");
                flags.add(c.getImplDir() + "/src-gen");
                for (Extra dir : c.getSrcdir()) {
                    flags.add(dir.getValue());
                }
                if (c.getIsLibrary()) {
                }
                else {
                }
            }
            attributes.put("SRCDIRS", flags);
        }
        // collect compilationFlags and linkFlags from all components
        {
            LinkedHashSet<String> flags = new LinkedHashSet<>();
            for (var c : context.system.getComponents()) {
                for (var extra : c.getCompilationFlags()) {
                    if (extra.getProduction().isEmpty() || extra.getProduction().equals("linux-glaive2-x64"))
                        flags.add(Utilities.expandPath(extra.getValue(), context.workspace.getProjectRoot(), true));
                }
            }
            attributes.put("CFLAGS", flags);
        }
        {
            LinkedHashSet<String> flags = new LinkedHashSet<>();
            for (var c : context.system.getComponents()) {
                for (var extra : c.getLinkFlags()) {
                    if (extra.getProduction() == null || extra.getProduction().isEmpty() || extra.getProduction().equals("linux-glaive2-x64"))
                        flags.add(Utilities.expandPath(extra.getValue(), context.workspace.getProjectRoot(), true));
                }
            }
            attributes.put("LDFLAGS", flags);
        }

        createFileFromTemplate(new File(gendir, "makefile_dirs"), "core/makefile_dirs", "makefile_dirs", attributes);
    }

    private void generateFile(File file, String resourcePath) throws IOException {
        if (file.exists())
            report(file, ReportStatus.PRESERVED);
        else
            report(file, Utilities.createFileFromStream(file, getClass().getClassLoader().getResourceAsStream(resourcePath)));
    }
}
