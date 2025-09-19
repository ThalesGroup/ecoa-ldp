/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

import java.io.File;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.TypeDefinition;

public class GenType extends AbstractGenerationPass {

    protected boolean generateJavaInterfaces = false;
    FilePathResolver fpr = new FilePathResolver();

    // =========================================================================
    // Implementation of AbstractSoftarcGenerator generate() method
    // =========================================================================

    @Override
    public void execute() throws IOException {
        for (Component model : context.system.getComponents()) {
            // In case the component is a PERIODIC_TRIGGER_MANAGER, we have nothing to do! (except for LDP)
            if (model.getIsTimer()) {
                if (context.isLDP) {
                    generateForTimerLDP(model);
                }
            } else {
                if (model.getIsLibrary())
                    generateTypes(model);
                else {
                    generateComponentInterfaces(model);
                }
            }
        }
    }

    // =========================================================================
    // Component types generation
    // =========================================================================

    protected void generateTypes(Component component) throws IOException {

        // Java types generation ------------------------------
        if (component.getIsJavaComponent()) {
            if (!component.getConstants().isEmpty()) {
                generateJavaConstantsFile(component);
            }
            for (TypeDefinition type : component.getTypes()) {
                // Generate only complex types!!
                generateJavaTypeFile(component, type);
            }
            generateFileFromTemplate(component, KindOfFile.COMPONENT_PACKAGE_LIST_FILE, "package_info");
        }
        // Python types generation ------------------------------
        else if (component.getIsPythonComponent()) {
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_SOURCE_FILE, "lib_types");
        }
        // RUST generation -----------------------
        else if (component.getIsRustComponent()) {
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_HEADER_FILE, "component_types");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_CARGO_FILE, "cargo_types", "SOFTARC_HOME",
                    System.getenv("SOFTARC_HOME"));
        }
        // ADA, C, C++ types generation -----------------------
        else {
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_HEADER_FILE, "component_types");
        }
    }

    // =========================================================================
    // Component interface generation
    // =========================================================================

    private void generateComponentInterfaces(Component component) throws IOException {

        switch (Language.valueOf(component.getLanguage())) {
        case ADA:
        case C:
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_HEADER_FILE, "component_types");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_HEADER_FILE, "component_header");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_SOURCE_FILE, "component_source");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_DATA_HEADER_FILE, "component_data");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_USER_CONTEXT_HEADER_FILE, "component_user_context");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_CONTAINER_HEADER_FILE, "component_container");
            break;

        case CPP:
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_HEADER_FILE, "component_types");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICOMPONENT_FILE, "component_interface");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_HEADER_FILE, "component_header");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_SOURCE_FILE, "component_source");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICONTAINER_FILE, "component_container_interface");
            break;

        case JAVA:
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICOMPONENT_FILE, "component_interface");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_SOURCE_FILE, "component_source");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICONTAINER_FILE, "component_container_interface");
            for (OperationRequestResponse svc : component.getRequiredRequestResponses()) {
                if (svc.getHasOutParameters()) {
                    generateRequestResponseHolder(component, svc);
                }
            }
            for (OperationRequestResponse svc : component.getProvidedRequestResponses()) {
                if (svc.getHasOutParameters()) {
                    generateRequestResponseHolder(component, svc);
                }
            }
            break;

        case PYTHON:
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICOMPONENT_FILE, "component_interface");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_SOURCE_FILE, "component_source");
            // generateFileFromTemplate(component, KindOfFile.COMPONENT_CONTAINER_HEADER_FILE, "component_container");
            break;

        case RUST:
            generateFileFromTemplate(component, KindOfFile.COMPONENT_ICONTAINER_FILE, "component_interface");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_SOURCE_FILE, "component_source");
            generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_CARGO_FILE, "cargo_types", "SOFTARC_HOME",
                    System.getenv("SOFTARC_HOME"));

            break;

        default:
            errorModel("unsupported language: %s", component.getLanguage());
        }
    }

    private void generateForTimerLDP(Component component) throws IOException {
        generateFileFromTemplate(component, KindOfFile.COMPONENT_TYPES_HEADER_FILE, "component_types");
        generateFileFromTemplate(component, KindOfFile.COMPONENT_DATA_HEADER_FILE, "component_data");
        generateFileFromTemplate(component, KindOfFile.COMPONENT_HEADER_FILE, "component_header");
        generateFileFromTemplate(component, KindOfFile.COMPONENT_GENERATED_USER_CONTEXT_HEADER_FILE, "component_user_context");
        generateFileFromTemplate(component, KindOfFile.COMPONENT_CONTAINER_HEADER_FILE, "component_container");
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private void generateFileFromTemplate(Component model, KindOfFile fileid, String templateName, Object... attributes)
            throws IOException {

        List<Object> args = new ArrayList<>();
        args.add("model");
        args.add(model);
        args.addAll(Arrays.asList(attributes));

        Language lang = Language.valueOf(model.getLanguage());
        File outfile = fpr.getFilePath(fileid, model);

        if (fpr.isManualFile(fileid, lang)) {
            createNewFileFromTemplate(outfile,
                    "gentype/" + (model.getIsEcoa() ? model.getApiVariant() : lang.name()) + "/" + templateName, templateName,
                    args.toArray());
        } else {
            createFileFromTemplate(outfile,
                    "gentype/" + (model.getIsEcoa() ? model.getApiVariant() : lang.name()) + "/" + templateName, templateName,
                    args.toArray());
        }
    }

    private void generateJavaConstantsFile(Component component) throws IOException {
        File outfile = fpr.getJavaConstantsFilePath(component);
        createFileFromTemplate(outfile, "gentype/JAVA/component_type", "component_constants", "model", component);
    }

    private void generateJavaTypeFile(Component component, TypeDefinition type) throws IOException {
        // No type class generation for string types
        // Only for non-scalar types and enumerates.
        if (!type.getIsString() && (type.getIsEnum() || !type.getIsScalar())) {
            File outfile = fpr.getJavaTypeFilePath(component, type);
            if (fpr.isManualTypeFile(component, type)) {
                createNewFileFromTemplate(outfile, "gentype/JAVA/component_type", "component_type", "model", component, "type",
                        type);
            } else {
                if (this.generateJavaInterfaces) {
                    createFileFromTemplate(outfile, "gentype/JAVA/component_type", "component_type_base", "model", component,
                            "type", type);
                    if (type.getHasJavaInterface()) {
                        createFileFromTemplate(new File(outfile.getParent(), "impl/" + type.getImplName() + ".java"),
                                "gentype/JAVA/component_type", "component_type_impl", "model", component, "type", type);
                        createFileFromTemplate(new File(outfile.getParent(), type.getRwName() + ".java"),
                                "gentype/JAVA/component_type", "component_type_rwInterface", "model", component, "type", type);
                    }
                } else {
                    createFileFromTemplate(outfile, "gentype/JAVA/component_type", "component_type", "model", component, "type",
                            type);
                }
            }
        }
    }

    private void generateRequestResponseHolder(Component component, OperationRequestResponse svc) throws IOException {
        File outfile = fpr.getJavaRequestResponseHolderFilePath(component, svc);
        createFileFromTemplate(outfile, "gentype/JAVA/component_service_holder", "serviceHolder", "model", component, "svc", svc);
    }

}
