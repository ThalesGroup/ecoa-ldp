/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

import java.io.File;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.TypeDefinition;

public class FilePathResolver {

    /** @return Given file absolute file path */
    public File getFilePath(KindOfFile kind, Component component) {

        Language lang = Language.valueOf(component.getLanguage());
        String filePrefix = component.getFileprefix();
        String outfilename = null;
        KindOfDirectory dir = null;

        switch (kind) {
           case COMPONENT_TYPES_CARGO_FILE :
        	   switch (lang) {
                   case RUST:
            	      dir = KindOfDirectory.ROOT;
                      outfilename = "Cargo.toml";
                      break;
                   case CPP:
                   case ADA:
                   case C:
                   case JAVA:
                   case PYTHON:
                   break;
               }
               break;
            case COMPONENT_HEADER_FILE:
                outfilename = filePrefix + "." + component.getHeaderextension();
                switch (lang) {
                    case RUST:
             	       break;
                    case CPP:
                        dir = KindOfDirectory.INC;
                        break;
                    case ADA:
                    case C:
                    case JAVA:
                    case PYTHON:
                        dir = KindOfDirectory.INC_GEN;
                        break;
                }
                break;
            case COMPONENT_SOURCE_FILE:
                switch (lang) {
                   case RUST:
                	   dir = KindOfDirectory.SRC;
                       outfilename = "lib.rs";
                       break;
                    case ADA:
                    case CPP:
                    case C:
                    case PYTHON:
                        outfilename = filePrefix + "." + component.getSourceextension().iterator().next();
                        break;
                    case JAVA:
                        outfilename = filePrefix + "/Component.java";
                        break;
                }
                dir = KindOfDirectory.SRC;
                break;
            case COMPONENT_TYPES_HEADER_FILE:
            	dir = KindOfDirectory.INC_GEN;
                switch (lang) {
                    case RUST:
                    	dir = KindOfDirectory.SRC;
                        outfilename = "lib.rs";
                       break;
                    case ADA:
                        outfilename = filePrefix + "_types.ads";
                        break;
                    case CPP:
                        outfilename = filePrefix + "_types.hpp";
                        break;
                    case PYTHON:
                    case C:
                        if (component.getIsEcoa() && component.getIsLibrary()) 
                            outfilename = filePrefix + ".h";
                        else
                            outfilename = filePrefix + "_types.h";
                        break;
                    case JAVA:
                        break;
                }
                break;
            case COMPONENT_DATA_HEADER_FILE:
                switch (lang) {
                    case ADA:
                        outfilename = filePrefix + "_data.ads";
                        break;
                    case CPP:
                        break;
                    case C:
                    case PYTHON:
                        if (component.getIsEcoa())
                            outfilename = filePrefix + "_container_types.h";
                        else
                            outfilename = filePrefix + "_data.h";
                        break;
                    case JAVA:
                        break;
                }
                dir = KindOfDirectory.INC_GEN;
                break;
            case COMPONENT_USER_CONTEXT_HEADER_FILE:
            	dir = KindOfDirectory.INC;
                switch (lang) {
                    case ADA:
                        outfilename = filePrefix + "_user_context.ads";
                        break;
                    case CPP:
                        break;
                    case C:
                        outfilename = filePrefix + "_user_context.h";
                        break;
                    case JAVA:
                    case RUST:
                    case PYTHON:
                        break;
                }
                break;
            case COMPONENT_GENERATED_USER_CONTEXT_HEADER_FILE:
                dir = KindOfDirectory.INC_GEN;
                switch (lang) {
                case C:
                    outfilename = filePrefix + "_user_context.h";
                    break;
                }
                break;
            case COMPONENT_CONTAINER_HEADER_FILE:
                switch (lang) {
                   case ADA:
                        outfilename = filePrefix + "_container.ads";
                        break;
                    case CPP:
                        break;
                    case C:
                    case PYTHON:
                        outfilename = filePrefix + "_container.h";
                        break;
                    case JAVA:
                        break;
                }
                dir = KindOfDirectory.INC_GEN;
                break;
                // Supervisor specific case
            case COMPONENT_SUPERVISOR_CONSTANTS_HEADER_FILE:
                switch (lang) {
                    case ADA:
                        outfilename = filePrefix + "_constants.ads";
                        dir = KindOfDirectory.INC_GEN;
                        break;
                    case C:
                    case PYTHON:
                        outfilename = filePrefix + "_constants.h";
                        dir = KindOfDirectory.INC_GEN;
                        break;
                    case CPP:
                        outfilename = filePrefix + "_constants.hpp";
                        dir = KindOfDirectory.INC_GEN;
                        break;
                    case JAVA:
                        outfilename = filePrefix + "/Constants.java";
                        dir = KindOfDirectory.SRC_GEN;
                        break;
                }
                break;
            case COMPONENT_ICONTAINER_FILE:
                  switch (lang) {
                    case RUST:
              	       dir = KindOfDirectory.SRC;
                       outfilename = "gen/mod.rs";
                       break;
                    case ADA:
                        break;
                    case CPP:
                        outfilename = "I" + filePrefix + "_container.hpp";
                        dir = KindOfDirectory.INC_GEN;
                        break;
                    case C:
                    case PYTHON:
                        break;
                    case JAVA:
                        outfilename = filePrefix + "/IContainer.java";
                        dir = KindOfDirectory.SRC_GEN;
                        break;
                }
                break;
            case COMPONENT_ICOMPONENT_FILE:
                switch (lang) {
                    case ADA:
                        break;
                    case CPP:
                        outfilename = "I" + filePrefix + ".hpp";
                        dir = KindOfDirectory.INC_GEN;
                        break;
                    case C:
                        break;
                    case JAVA:
                        outfilename = filePrefix + "/IComponent.java";
                        dir = KindOfDirectory.SRC_GEN;
                        break;
                    case PYTHON:
                        outfilename = "I" + filePrefix + ".py";
                        dir = KindOfDirectory.SRC_GEN;
                        break;
                }
                break;

            case COMPONENT_PACKAGE_LIST_FILE:
                if (lang == Language.JAVA) {
                    String pkgdir = "";
                    outfilename = "package-info.java";
                    for (String pkgToken : component.getSplittedPackage()) {
                        pkgdir = pkgdir + "/" + pkgToken;
                    }
                    outfilename = pkgdir + "/" + outfilename;
                    dir = KindOfDirectory.SRC_GEN;
                    break;
                }
                break;
            case COMPONENT_INITIALIZE_HEADER_FILE:
                dir = KindOfDirectory.INC_GEN;
                switch (lang) {
                    case ADA:
                        outfilename = filePrefix + "_initialize.ads";
                        break;
                    case C:
                        outfilename = filePrefix + "_initialize.h";
                        break;
                    case CPP:
                        outfilename = filePrefix + "_initialize.hpp";
                        break;
                    case JAVA:
                    case PYTHON:
                        break;
                }
                break;

            case COMPONENT_TYPES_SOURCE_FILE:
                dir = KindOfDirectory.SRC_GEN;
                switch (lang) {
                    case PYTHON:
                        if (component.getIsLibrary()) {
                            outfilename = filePrefix + ".py";
                        } else {
                            outfilename = filePrefix + "_types.py";
                        }
                        break;
                    case C:
                    case ADA:
                    case CPP:
                    case JAVA:
                        break;
                }
                break;

            case COMPONENT_INITIALIZE_SOURCE_FILE:
                dir = KindOfDirectory.SRC_GEN;
                switch (lang) {
                    case C:
                        outfilename = filePrefix + "_initialize.c";
                        break;
                    case ADA:
                        outfilename = filePrefix + "_initialize.adb";
                        break;
                    case CPP:
                    case JAVA:
                    case PYTHON:
                        break;
                }
                break;
        }
        assert dir != null;
        assert outfilename != null;
        return new File(new File(component.getImplDir(), dir.getDirName()), outfilename);
    }

    public File getJavaConstantsFilePath(Component component) {
        File dir = new File(component.getImplDir(), KindOfDirectory.SRC_GEN.getDirName());

        for (String pkgToken : component.getSplittedPackage()) {
            dir = new File(dir, pkgToken);
        }

        return new File(dir, "Constants.java");
    }

    public File getJavaTypeFilePath(Component component, TypeDefinition type) {
        File dir = new File(component.getImplDir());

        dir = new File(dir, KindOfDirectory.SRC_GEN.getDirName());

        for (String pkgToken : component.getSplittedPackage()) {
            dir = new File(dir, pkgToken);
        }

        return new File(dir, type.getName() + ".java");
    }

    public File getJavaRequestResponseHolderFilePath(Component component, OperationRequestResponse svc) {
        File dir = new File(component.getImplDir(), KindOfDirectory.SRC_GEN.getDirName());

        for (String pkgToken : component.getSplittedPackage()) {
            dir = new File(dir, pkgToken);
        }
        return new File(dir, svc.getName() + "Holder.java");
    }

    /**
     * @param component Component/Library which declares the given type.
     * @param type TypeDefinition object
     * @return True if the type implementation file is a manual source file.
     */
    public boolean isManualTypeFile(Component component, TypeDefinition type) {
        return false;
    }

    /**
     * @param kind TypeDefinition of file to check
     * @return True if the file is a manual or a generated file
     */
    boolean isManualFile(KindOfFile kind, Language lang) {
        switch (kind) {
            case COMPONENT_USER_CONTEXT_HEADER_FILE:
            case COMPONENT_SOURCE_FILE:
                return true;
            case COMPONENT_HEADER_FILE:
                return (lang == Language.CPP);
            default:
                return false;
        }
    }
}
