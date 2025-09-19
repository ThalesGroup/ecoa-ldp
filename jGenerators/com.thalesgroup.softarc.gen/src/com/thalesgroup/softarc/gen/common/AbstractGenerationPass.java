/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.thalesgroup.softarc.tools.AbstractGenerator;
import com.thalesgroup.softarc.tools.ReportStatus;

public abstract class AbstractGenerationPass extends AbstractPass {

    public AbstractGenerationPass() {
        super();
    }

    public void createFileFromTemplate(File target, String templateGroupName, String templateName, Object... attributes)
            throws IOException {
        gen.setSeparatorsAuto(templateGroupName);
        gen.createFileFromTemplate(target, templateGroupName, templateName, attributes);
    }

    public void createFileFromTemplate(File target, String templateGroupName, String templateName, Map<String, Object> attributes)
            throws IOException {
        gen.setSeparatorsAuto(templateGroupName);
        gen.createFileFromTemplate(target, templateGroupName, templateName, attributes);
    }

    public void createFileFromStream(File target, InputStream stream)
            throws IOException {
        gen.createFileFromStream(target, stream);
    }

    /**
     * Si le fichier target n'existe pas, renvoie ce fichier. Sinon, renvoie un fichier avec l'extension
     * .new si l'option createnewfile est positionnée, ou 'null' sinon.
     */
    public File possiblyNewFile(File target) {
        if (!target.exists()) {
            return target;
        } else {
            // Tell user that file generation has been skipped
            report(target, ReportStatus.PRESERVED);
            /* If the createNew option is activated, create a file anyway */
            if (context.featureToggles.contains("createnew")) {
                return new File(target.getAbsolutePath() + ".new");
            }
            return null;
        }
    }

    public void createNewFileFromTemplate(File target, String templateGroupName, String templateName,
            Map<String, Object> attributes) throws IOException {
        target = possiblyNewFile(target);
        if (target != null) {
            createFileFromTemplate(target, templateGroupName, templateName, attributes);
        }
    }

    public void createNewFileFromTemplate(File target, String templateGroupName, String templateName, Object... attributes)
            throws IOException {
        target = possiblyNewFile(target);
        if (target != null) {
            createFileFromTemplate(target, templateGroupName, templateName, attributes);
        }
    }

    @Override
    public void init(PassContext context, AbstractGenerator gen) {
        super.init(context, gen);

    }

    public final boolean isReadOnly() {
        return true;
    }
}
