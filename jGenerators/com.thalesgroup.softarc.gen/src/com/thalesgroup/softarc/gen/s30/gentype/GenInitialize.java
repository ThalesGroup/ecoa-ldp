/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

import java.io.File;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;

import java.io.IOException;

import com.thalesgroup.softarc.sf.Component;

public class GenInitialize extends AbstractGenerationPass {

    FilePathResolver fpr = new FilePathResolver();

    // =========================================================================
    // Implementation of AbstractSoftarcGenerator generate() method
    // =========================================================================

    @Override
    public void execute() throws IOException {
        for (Component model : context.system.getComponents()) {
            if (model.getIsLibrary()) {
                generateInitialize(model);
            }
        }
    }

    private void generateInitialize(Component component) throws IOException {

        if (component.getIsJavaComponent()
            || component.getIsPythonComponent()
            || component.getIsRustComponent()) {
            // rien à faire

        } else {
            generateFileFromTemplate(component, KindOfFile.COMPONENT_INITIALIZE_HEADER_FILE, "initializeHeader");

            if (!component.getTypes().isEmpty()) {
                if (component.getIsCComponent()) { // y compris C_ECOA
                    generateFileFromTemplate(component, KindOfFile.COMPONENT_INITIALIZE_SOURCE_FILE, "initializeSource");
                }
            }
        }
    }

    private void generateFileFromTemplate(Component model, KindOfFile fileid, String templateName) throws IOException {

        File outfile = fpr.getFilePath(fileid, model);
        Language lang = Language.valueOf(model.getLanguage());
        
        createFileFromTemplate(outfile, "initialize/" + (model.getIsEcoa() ? model.getApiVariant() : lang.name()) + "/" + templateName, templateName, "model", model);
    }

}
