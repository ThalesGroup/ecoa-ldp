/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import com.thalesgroup.softarc.sf.impl.JsonWriterFormalism;
import com.thalesgroup.softarc.sf.impl.TextWriter;
import com.thalesgroup.softarc.sf.impl.XmlWriter;
import com.thalesgroup.softarc.tools.AbstractGenerator;

/**
 * This class executes a sequence of passes, defined by a collection of classes (stepClasses).
 * 
 * In verbose mode, a file containing a formalism dump is created after each pass, except generation passes which cannot modify
 * the formalism.
 *
 * In verbose mode, the time taken by each pass is printed out.
 */
public abstract class AbstractGenSoftarc extends AbstractGenerator {

    /**
     * DEBUG OPTIONS
     * 
     * <li>To disable some passes for a one-shot test => just comment them out in the init of stepClasses list
     * <li>To disable some passes permanently => create a new generator class
     * <li>To skip all generation passes => use debugOption_skipGeneration
     * <li>To restart generation from a file => use debugOption_restartFromPass
     */
    /**
     * Skip all generation passes (to debug transformations more easily)
     */
    public boolean debugOption_skipGeneration = false;

    /** END OF DEBUG OPTIONS */

    protected final HashSet<String> featureToggles = new HashSet<String>();

    public void setFeatureToggles(String values) {
        if (values != null) {
            for (String s : values.split(",")) {
                featureToggles.add(s);
            }
        }
    }

    public static final String ARGUMENT_KEY_VERBOSE = "verbose";
    public static final String ARGUMENT_KEY_CREATE_NEW = "createnew";

    public void generate(PassContext context, Collection<Class<? extends AbstractPass>> stepClasses) throws Exception {

        boolean verboseMode = _arguments.getFirst(ARGUMENT_KEY_VERBOSE);
        if (verboseMode)
            featureToggles.add("verbose");
        boolean createNew = _arguments.getFirst(ARGUMENT_KEY_CREATE_NEW);
        if (createNew)
            featureToggles.add("createnew");
        context.featureToggles = featureToggles;

        File dumpFile = null;
        try {
            for (Class<? extends AbstractPass> c : stepClasses) {

                AbstractPass step = c.newInstance();

                if (debugOption_skipGeneration && step.isReadOnly())
                    continue;

                step.init(context, this);

                if (verboseMode) {
                    info("Pass: %s", step.passName);
                }
                long tPass = System.nanoTime();
                step.execute();

                if (verboseMode) {
                    step.info("Pass %s executed in %d ms\n", step.passName, (System.nanoTime() - tPass) / 1000000);

                    if (!step.isReadOnly() && context.workspace.currentDeploymentName != null) {
                    }

                    if (!step.isReadOnly()) {
                        dumpFile = save(context, "formalism_after_" + step.passName);
                    }
                }
            }
            if (dumpFile != null) {
                dumpFile.renameTo(
                        new File(context.workspace.getGenDir(), "formalism" + dumpFile.getName().replaceFirst("\\w*\\.\\w*", "")));
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    // =========================================================================
    // Save formalism (in verbose mode)
    // =========================================================================

    public File save(PassContext context, String name) throws Exception {
        File file = null;
        if (context.featureToggles.contains("dump_xml")) {
            file = new File(context.workspace.getGenDir(), name + ".xml");
            XmlWriter w = new XmlWriter(file);
            w.write(context.system, "system", false);
            w.close();
        } else if (context.featureToggles.contains("dump_text")) {
            file = new File(context.workspace.getGenDir(), name + ".txt");
            TextWriter w = new TextWriter(file);
            w.write(context.system, "system", false);
            w.close();
        } else {
            file = new File(context.workspace.getGenDir(), name + ".json");
            JsonWriterFormalism w = new JsonWriterFormalism(file);
            w.write(context.system, false);
            w.close();
        }
        return file;
    }

    // =========================================================================
    // Construction
    // =========================================================================

    public AbstractGenSoftarc(String toolName, String templatesRoot) {
        super(toolName, templatesRoot);

        addArgument(ARGUMENT_KEY_VERBOSE, 'v', Boolean.class, 0, 1,
                "Verbose mode: dump formalism in XML after each generation step.", Boolean.FALSE);
        getArguments().setOptionActivated(ARGUMENT_KEY_VERBOSE, true);

        addArgument(ARGUMENT_KEY_CREATE_NEW, 'n', Boolean.class, 0, 1,
                "Create a file with extension .new for each preserved file", Boolean.FALSE);
        getArguments().setOptionActivated(ARGUMENT_KEY_CREATE_NEW, true);

    }

    @Override
    protected void initialize() throws Exception {
        String ft = System.getenv("SOFTARC_FEATURES");
        if (ft != null) {
            warning("SOFTARC_FEATURES=%s", ft);
            setFeatureToggles(ft);
        }
    }
}
