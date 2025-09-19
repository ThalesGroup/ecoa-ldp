/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.io.File;

import com.thalesgroup.softarc.tools.AbstractGenerator;
import com.thalesgroup.softarc.tools.ReportStatus;

public abstract class AbstractPass {

    public PassContext context;

    abstract public void execute() throws Exception;

    public void report(File file, ReportStatus status) {
        gen.report(file, status);
    }

    public void info(String format, Object... arguments) {
        gen.info(format, arguments);
    }

    public void warning(String format, Object... arguments) {
        gen.warning(format, arguments);
    }

    public void errorModel(String format, Object... arguments) {
        gen.errorModel(format, arguments);
    }

    public void errorInternal(String format, Object... arguments) {
        throw new Error(String.format(format, arguments));
    }

    public AbstractGenerator gen;
    public String passName = getClass().getPackage().getName().replace("com.thalesgroup.softarc.gen.", "");

    public boolean isReadOnly() {
        return false;
    }

    public void init(PassContext context, AbstractGenerator gen) {
        this.gen = gen;
        this.context = context;
    }
}
