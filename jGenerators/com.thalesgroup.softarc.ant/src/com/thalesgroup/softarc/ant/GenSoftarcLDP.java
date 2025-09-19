/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.ant;

import com.thalesgroup.softarc.tools.AbstractAntGeneratorTask;
import com.thalesgroup.softarc.tools.AbstractGenerator;

public class GenSoftarcLDP extends AbstractAntGeneratorTask {

    protected AbstractGenerator newGenerator() throws Throwable {
        com.thalesgroup.softarc.gen.GenSoftarcLDP genSoftarc = new com.thalesgroup.softarc.gen.GenSoftarcLDP();
        genSoftarc.setFeatureToggles(getProject().getProperty("gensoftarc.features"));
        return genSoftarc;
    }

    public void setDeployment(String i) {
        _antArgs.add("--deployment");
        _antArgs.add(i);
    }

    public void setVerbose(boolean v) {
        if (v)
            _antArgs.add("--verbose");
    }

    public void setCreatenew(boolean b) {
        if (b) {
            _antArgs.add("--createnew");
        }
    }
    
}
