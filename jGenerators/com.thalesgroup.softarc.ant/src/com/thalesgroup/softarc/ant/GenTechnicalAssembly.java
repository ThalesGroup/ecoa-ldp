/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.ant;

import com.thalesgroup.softarc.tools.AbstractAntGeneratorTask;
import com.thalesgroup.softarc.tools.AbstractGenerator;

public class GenTechnicalAssembly extends AbstractAntGeneratorTask {
	
	protected AbstractGenerator newGenerator() throws Throwable {
		return new com.thalesgroup.softarc.gen.technicalassembly.GenTechnicalAssembly();
	}

    public void setDeployment(String i) {
        _antArgs.add("-d");
        _antArgs.add(i);
    }
}
