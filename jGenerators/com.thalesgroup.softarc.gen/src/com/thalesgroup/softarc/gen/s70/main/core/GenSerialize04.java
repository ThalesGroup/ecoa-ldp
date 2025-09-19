/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s70.main.core;

import java.io.File;
import java.util.LinkedHashSet;

import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;

import java.io.IOException;
import com.thalesgroup.softarc.sf.Component;

/**
 * Generate all files *_serialize.c and *_validate.c, in 04-Integration/deployment/src-gen.
 * 
 * The code is always generated in C.
 */
public class GenSerialize04 extends AbstractGenerationPass {

    FilepathResolver fpr;

    @Override
    public void execute() throws IOException {
        fpr = new FilepathResolver(context.workspace);

        {
            {
                // All the libraries used by the local components
                LinkedHashSet<Component> allLibraries = new LinkedHashSet<>();
                for (Component ct : context.system.getComponents()) {
                    allLibraries.addAll(ct.getAllLibraries());
                }
                
                LinkedHashSet<Component> allCLibraries = new LinkedHashSet<>();
                for (Component ct : allLibraries) {
                    allCLibraries.add(ct.getCComponent());
                }

                // Serialisation interface
                // Generate headers for SOFTARC_C
                for (Component ct : allCLibraries) {

                    createFileFromTemplate(new File(fpr.getFilePath(KindOfFile.COMPONENT_SERIALIZE_HEADER_FILE, ct)),
                            "serialize/C/serializeHeader", "serializeHeader", "componentType", ct);
                }

                // Serialisation code in SOFTARC_C:
                for (Component ct : allCLibraries) {

                    createFileFromTemplate(new File(fpr.getFilePath(KindOfFile.COMPONENT_SERIALIZE_SOURCE_FILE, ct)),
                            "serialize/C/serializeSource", "serializeSource", "componentType", ct, "noByteSwap", true,
                            "osProperties", context.system.getMapping().getPlatforms().get(0).getOsProperties());
                }

                // Validate functions: first identify all the files we need (in C)
                LinkedHashSet<Component> validateFunctionsNeeded = new LinkedHashSet<>();
                for (Component ct : allCLibraries) {
                    validateFunctionsNeeded.add(ct);
                }
                for (Component ct : allLibraries) {
                    if (ct.getIsAdaComponent()) {
                        validateFunctionsNeeded.add(ct);
                    }
                }

                // Generate Validate functions
                for (Component ct : validateFunctionsNeeded) {
                    if (ct.getNeedsValidation()) {
                        createFileFromTemplate(new File(fpr.getFilePath(KindOfFile.COMPONENT_VALIDATE_HEADER_FILE, ct)),
                                "validate/" + ct.getLanguage() + "/validateHeader", "validateHeader", "componentType", ct);
                        createFileFromTemplate(new File(fpr.getFilePath(KindOfFile.COMPONENT_VALIDATE_SOURCE_FILE, ct)),
                                "validate/" + ct.getLanguage() + "/validateSource", "validateSource", "componentType", ct);
                    }
                }
            }
        }
    }
}
