/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.util.LinkedHashMap;
import java.util.Map;

import technology.ecoa.model.deployment.DEDeployedInstance;
import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.deployment.DEExecutable;
import technology.ecoa.model.deployment.DETask;

import com.thalesgroup.softarc.tools.Requirement;

@Requirement(ids = { "GenFramework-SRS-REQ-128", "GenFramework-SRS-REQ-129" })
public final class Deployment {
    public Deployment(DEApplication deployment) {

        _deployment = deployment;

		_executables.put(deployment.getName(), deployment);
		for (DEExecutable executable : deployment.getExecutable()) {
			_executables.put(executable.getName(), executable);
		}
		
		for (DEExecutable executable : _executables.values()) {
			for (DETask task : executable.getTask()) {
				for (DEDeployedInstance instance : task.getDeployedInstance()) {
					_instances.put(instance.getRef(), instance);
				}
			}
		}
    }

    public DEApplication getDeployment() {
        return _deployment;
    }

    public DEExecutable getExecutable(String execName) {
        return _executables.get(execName);
    }

    public DEDeployedInstance getInstance(String instName) {
        return _instances.get(instName);
    }

    public Map<String, DEDeployedInstance> getInstances() {
        return _instances;
    }

    private final Map<String, DEExecutable> _executables = new LinkedHashMap<String, DEExecutable>();

    private final Map<String, DEDeployedInstance> _instances = new LinkedHashMap<String, DEDeployedInstance>();

    private final DEApplication _deployment;
}
