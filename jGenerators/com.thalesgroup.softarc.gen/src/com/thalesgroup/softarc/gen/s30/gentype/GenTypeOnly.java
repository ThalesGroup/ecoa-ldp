/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

import java.io.IOException;
import com.thalesgroup.softarc.sf.Component;

public final class GenTypeOnly extends GenType {

    // =========================================================================
    // Implementation of AbstractSoftarcGenerator generate() method
    // =========================================================================

    @Override
    public void execute() throws IOException {
        this.generateJavaInterfaces = true;

        for (Component model : context.system.getComponents()) {
            // In case the component is a PERIODIC_TRIGGER_MANAGER, we have nothing to do!
            if (!model.getIsTimer()) {
                generateTypes(model);
            }
        }
        generateTypes(context.system.getInterface());
    }
}
