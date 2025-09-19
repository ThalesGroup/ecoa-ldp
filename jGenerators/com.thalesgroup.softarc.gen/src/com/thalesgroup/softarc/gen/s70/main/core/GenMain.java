/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s70.main.core;

import com.thalesgroup.softarc.gen.common.AbstractGenerationPass;
import com.thalesgroup.softarc.sf.*;
import com.thalesgroup.softarc.sf.Thread;
import com.thalesgroup.softarc.tools.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenMain extends AbstractGenerationPass {
	
	private static final String SRC_GEN_DIR = "src-gen/";
	private static final String INC_GEN_DIR = "inc-gen/";
	private static final String TEMPLATE_CORE_DIR = "templates/core/";

    final GenerationOptions options = new GenerationOptions();
    FilepathResolver fpr;
    Mapping mapping;

    @Override
    public void execute() throws IOException {

        fpr = new FilepathResolver(context.workspace);
        File gendir = context.workspace.getGenDir();

        // Generator-independent c file generation
        for (String file : new String[] { // @formatter:off
                "softarc.h",
                "ecoa.h",
                "sarc_ldp_internal.h",
                "sarc_error.c",
                "sarc_life_cycle.c",
                "sarc_error.c",
                "sarc_life_cycle.c",
                "sarc_serial.c",
                "sarc_timed_message.c",
                "sarc_launcher.c",
                "sarc_task.c",
                "sarc_pinfo.c",
                "sarc_map.c" })
            //@formatter:on
            generateFile(new File(gendir, (file.endsWith(".h") ? INC_GEN_DIR : SRC_GEN_DIR) + file), TEMPLATE_CORE_DIR + file);

        mapping = context.system.getMapping();

        options.hasSafeReaders = mapping.getSafeReaders();
        options.autoStart = mapping.getAutoStart();
        options.fastStart = mapping.getFastStart();
        options.initializeParameters = mapping.getInitializeOutput();

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("system", context.system);

        generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.EXEC_MAIN_SOURCE_FILE), "core", null,
                "execMainBody");

        generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.LDP_HEADER_FILE), "core", null, "ldpHeader");

        generateExecutableGlobal(mapping.getGlobalExecutable());
        generateAllForComponents(mapping.getGlobalExecutable());
    }

    // =========================================================================
    // Executable-level files generation
    // =========================================================================

    private void generateExecutableGlobal(Executable execWrap) throws IOException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("exec", execWrap);

        // Component threads routines
        for (Thread thread : execWrap.getThreads()) {

            attributes.put("thread", thread);
            generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.EXEC_THREAD_SOURCE_FILE, thread), "core",
            		null , "execThreadBody");
        }

    }

    // =========================================================================
    // Component-level files generation
    // =========================================================================

    private void generateAllForComponents(Executable execWrap) throws IOException {
        Map<String, Object> attributes = new HashMap<String, Object>();

        for (Container container : execWrap.getContainers()) {

            String apiVariant = container.getComponent().getApiVariant();
            
            attributes.put("container", container);
            attributes.put("options", this.options);

            // Component instance
            generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.COMPONENT_FACADE_FILE, container),
                    "core", null, "facade");
            
            generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.COMPONENT_INSTANCE_SOURCE_FILE, container),
                    "core", apiVariant, "instanceBody");

            // Component container
            // Necessary attribute for Triggers
            List<List<Long>> liTrigId = getPendingRequestIds(container);
            attributes.put("triggerPendingRequestIds", liTrigId);
            attributes.put("mapping", mapping);
            generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.COMPONENT_CONTAINER_SOURCE_FILE, container),
                    "core", apiVariant, "container");
            attributes.remove("mapping");
            attributes.remove("triggerPendingRequestIds");
            attributes.remove("componentInstanceList");
            
            attributes.remove("options");
            attributes.remove("operationsMap");

            // Timer specific case
            if (container.getComponent().getIsTimer()) {
                // Timer body
                generateFileFromTemplate(attributes, fpr.getFilePath(KindOfFile.COMPONENT_TIMER_SOURCE_FILE, container),
                        "core", apiVariant, "timerBody");
            }
            // Supervisor specific case
            if (container.getComponent().getIsSupervisor()) {
                if (container.getComponent().getIsEcoa()) {
                    errorModel("Language C_ECOA is not allowed for SUPERVISOR component %s", container.getComponent()
                            .getTypeName());
                }
                attributes.clear();
                attributes.put("componentType", container.getComponent());

                attributes.put("mapping", mapping);

                    generateFileFromTemplate(attributes,
                            fpr.getFilePath(KindOfFile.COMPONENT_SUPERVISOR_CONSTANTS_HEADER_FILE, container.getComponent()),
                            "core", apiVariant, "supervisorConstantsHeader");
                attributes.remove("mapping");

                // TODO déplacer vers GenType
                attributes.put("exec", execWrap);

                generateExampleFileFromTemplate(attributes,
                        fpr.getFilePath(KindOfFile.COMPONENT_SUPERVISOR_CHANGE_SOURCE_FILE, container.getComponent()),
                        "supervisor", apiVariant, "onStateChangeBody");
                attributes.remove("exec");
            } // End of Supervisor specific case

            attributes.clear();
        }
    }

    private List<List<Long>> getPendingRequestIds(Container container) {
        /*
         * 1ere dimension: trigger dans le component type 2eme dimension: instance de composant Contenu du tableau: id de
         * l'opération (evènement) émis par le trigger
         */
        List<List<Long>> finalList = new ArrayList<List<Long>>();

        // Pour chaque trigger du composant
        for (Trigger trigger : container.getComponent().getTriggers()) {
            List<Long> bi = new ArrayList<Long>();
            // alors pour chaque instance de composants deployee dans l'executable
            for (Instance inst : container.getInstances()) {
                // pour chaque trigger associe a l'instance du composant
                for (TriggerInstance triggerInstance : inst.getTriggers()) {
                    // si le trigger evalue dans l'instance correspond au trigger que l'on etudie
                    // depuis le CT
                    if (triggerInstance.getName().equals(trigger.getName())) {
                        // alors on recupere l'ID qui nous interesse
                        bi.add(triggerInstance.getRequestId());
                    }
                }
            }
            // On stocke la table des id trouves dans la liste
            finalList.add(bi);
        }
        return finalList;
    }

    private void generateFileFromTemplate(Map<String, Object> attributes, String filepath, String templateDirName, String apiVariant, String templateName) throws IOException {
        createFileFromTemplate(new File(filepath), templateDirName + "/" + (apiVariant != null ? apiVariant + "/" : "") + templateName,
                templateName, attributes);
    }

    private void generateExampleFileFromTemplate(Map<String, Object> attributes, String filepath, String templateDirName,
            String lang, String templateName) throws IOException {
        File outfile = new File(filepath);
        if (!outfile.exists()) {
            createFileFromTemplate(outfile, templateDirName + "/" + (lang != null ? lang + "/" : "") + templateName,
                    templateName, attributes);
        }
    }
    
    private void generateFile(File file, String resourcePath) throws IOException {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null)
            throw new Error("Cannot load resource: " + resourcePath);
        report(file, Utilities.createFileFromStream(file, stream));
    }
}
