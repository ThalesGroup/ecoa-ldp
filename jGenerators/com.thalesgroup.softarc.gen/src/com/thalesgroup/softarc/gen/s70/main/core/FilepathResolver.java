/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s70.main.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.ecoa.model.Workspace;
import com.thalesgroup.softarc.gen.s30.gentype.KindOfDirectory;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.Container;
import com.thalesgroup.softarc.sf.Thread;
import com.thalesgroup.softarc.tools.InconsistentModelError;

public class FilepathResolver {

    Workspace ws;

    public FilepathResolver(Workspace ws) {
        this.ws = ws;
    }

    /**
     * @return Given file absolute file path
     */
    public String getFilePath(KindOfFile kind, Component component) throws IOException {
        List<Object> path = new ArrayList<Object>();
        Language lang = Language.valueOf(component.getLanguage());
        String filePrefix = component.getFileprefix();
        path.add(component.getImplDir());

        String outfilename = null;
        switch (kind) {

        // Supervisor specific case
        case COMPONENT_SUPERVISOR_CONSTANTS_HEADER_FILE:
            switch (lang) {
            case C:
                outfilename = filePrefix + "_constants.h";
                break;
            case CPP:
                outfilename = filePrefix + "_constants.hpp";
                break;
            }
            path.add(INC_GEN);
            path.add(outfilename);
            break;

        case COMPONENT_SUPERVISOR_CHANGE_SOURCE_FILE:
            switch (lang) {
            case C:
                outfilename = filePrefix + "_on_state_change.c";
                break;
            case CPP:
                outfilename = filePrefix + "_on_state_change.cpp";
                break;
            }
            path.add(SRC);
            path.add(outfilename);
            break;

        // End of supervisor specific case

            // Serialisation code

            case COMPONENT_SERIALIZE_HEADER_FILE:
                path.add(INC_GEN);
                    path.add(filePrefix + "_serialize.h");
                break;

            case COMPONENT_SERIALIZE_SOURCE_FILE:
                path.add(SRC_GEN);
                    path.add(filePrefix + "_serialize.c");
                break;

            case COMPONENT_VALIDATE_HEADER_FILE:
                path.add(INC_GEN);
                    path.add(filePrefix + "_validate.h");
                break;

            case COMPONENT_VALIDATE_SOURCE_FILE:
                path.add(SRC_GEN);
                    path.add(filePrefix + "_validate.c");
                break;

            
        default:
            return null;
        }
        return computePath(path);
    }

    public String getFilePath(KindOfFile kind, Container container) throws IOException {
        Component model = container.getComponent();
        String cmpName = model.getTypeName() + "_" + model.getImplName();
        Language lang = Language.valueOf(model.getLanguage());
        List<Object> path = new ArrayList<Object>();
        String filePrefix = model.getFileprefix();

        path.add(ws.getGenDir());

        switch (kind) {
        case COMPONENT_CONTAINER_HEADER_FILE:
            switch (lang) {
            case CPP:
                path.add(INC_GEN);
                path.add(filePrefix + "_container.hpp");
                break;
            default:
                break;
            }
            break;

        case COMPONENT_TIMER_CONTAINER_HEADER_FILE:
            switch (lang) {
            case C:
                path.add(SRC_GEN);
                path.add(filePrefix + "_container.h");
                break;
            default:
                throw new InconsistentModelError(
                        "Component " + model.getTypeName() + " is a PERIODIC_TRIGGER_MANAGER so it cannot be in " + lang);
            }
            break;

        case COMPONENT_CONTAINER_SOURCE_FILE:
            path.add(SRC_GEN);
            switch (lang) {
            case CPP:
                path.add(filePrefix + "_container.cpp");
                break;
            case C:
                path.add(filePrefix + "_container.c");
                break;
            }
            break;

        case COMPONENT_FACADE_FILE:
            path.add(INC_GEN);
            path.add(cmpName + "_facade.h");
            break;

        case COMPONENT_INSTANCE_SOURCE_FILE:
            path.add(SRC_GEN);
            switch (lang) {
            case CPP:
                path.add(filePrefix + "_instance.cpp");
                break;
            case C:
                path.add(filePrefix + "_instance.c");
                break;
            }
            break;

        // Timer specific files

        case COMPONENT_TIMER_HEADER_FILE:
            path.add(SRC_GEN);
                path.add(filePrefix + ".h");
            break;
        case COMPONENT_TIMER_SOURCE_FILE:
            path.add(SRC_GEN);
                path.add(filePrefix + ".c");
            break;

        default:
            return null;
        }

        return computePath(path);
    }

    /**
     * @return Given file absolute file path
     */
    public String getFilePath(KindOfFile kind, Thread thread)  {
        List<Object> path = new ArrayList<Object>();

        path.add(ws.getGenDir());

        switch (kind) {
        case EXEC_THREAD_SOURCE_FILE:
            path.add(SRC_GEN);
            path.add(thread.getName() + "_routine.c");
            break;

        default:
            return null;
        }

        return computePath(path);
    }

    /**
     * @return Given file absolute file path
     */
    public String getFilePath(KindOfFile kind) throws IOException {

        List<Object> path = new ArrayList<Object>();

        path.add(ws.getGenDir());

        switch (kind) {
			case EXEC_MAIN_SOURCE_FILE:
			    path.add(SRC_GEN);
			    path.add("main.c");
			    break;
			
			case LDP_HEADER_FILE:
				path.add(INC_GEN);
				path.add("sarc_ldp.h"); 
				break;
		    	
	        default:
	            return null;
	        }
        return computePath(path);
    }

    protected String computePath(List<Object> pathElems) {
        String path = "";

        for (Object o : pathElems) {
            if (path.length() > 0) {
                path += "/";
            }
            if (o instanceof String) {
                path += (String) o;
            } else if (o instanceof File) {
                path += ((File) o).getPath();
            }
        }
        return path.replace('\\', '/');
    }

    static final String INC_GEN = KindOfDirectory.INC_GEN.getDirName();
    static final String SRC_GEN = KindOfDirectory.SRC_GEN.getDirName();
    static final String INC = KindOfDirectory.INC.getDirName();
    static final String SRC = KindOfDirectory.SRC.getDirName();
}
