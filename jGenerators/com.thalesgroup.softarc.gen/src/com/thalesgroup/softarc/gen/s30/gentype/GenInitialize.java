/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

import java.io.File;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;
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

        } else if (!(context.isLDP && component.getIsEcoa())) {
            // Note: initialize functions are not supported in ECOA by the LDP, because of multiple _initialize.h files with the same name
            generateFileFromTemplate(component, KindOfFile.COMPONENT_INITIALIZE_HEADER_FILE, "initializeHeader");

            if (!component.getTypes().isEmpty()) {
                if (component.getIsCComponent()) {
                    generateFileFromTemplate(component, KindOfFile.COMPONENT_INITIALIZE_SOURCE_FILE, "initializeSource");
                }
            }
        }
    }

    private void generateFileFromTemplate(Component model, KindOfFile fileid, String templateName) throws IOException {

        File outfile = fpr.getFilePath(fileid, model);
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("model", model);

        String dirName = model.getIsEcoa() ? model.getApiVariant() : model.getLanguage();
        // Il y a un paramètre supplémentaire "prefix" pour les templates C uniquement (pour GenLib)
        if (dirName.equals("C")) {
            attributes.put("prefix", "SARC");
        }
        
        createFileFromTemplate(outfile, "initialize/" + dirName + "/" + templateName, templateName, attributes);
    }

}
