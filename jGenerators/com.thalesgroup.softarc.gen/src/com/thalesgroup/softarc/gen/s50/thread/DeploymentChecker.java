package com.thalesgroup.softarc.gen.s50.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thalesgroup.softarc.tools.InconsistentModelError;

import technology.ecoa.model.deployment.DEApplication;
import technology.ecoa.model.deployment.IOInterface;

public class DeploymentChecker {

    private DEApplication deployment;

    public DeploymentChecker(DEApplication deployment) {
        this.deployment = deployment;
    }

    public void checkAll() {
        checkUnicity();
    }

    public void checkUnicity() {

        // Noms des exécutables par plateforme dans le DE
        var allExes = new ArrayList<>(deployment.getExecutable());
        allExes.add(deployment);
        checkUnicity(allExes, "deployment/application/executable", "getName");
        
        var allTasks = new ArrayList<>(deployment.getTask());
        for (var e : deployment.getExecutable()) {
            allTasks.addAll(e.getTask());
        }
        checkUnicity(allTasks, "deployment/**/task", "getName");

        if (deployment.getExternalIo() != null) {
            List<IOInterface> allPorts = new ArrayList<>();
            allPorts.addAll(deployment.getExternalIo().getInPort());
            allPorts.addAll(deployment.getExternalIo().getOutPort());
            allPorts.addAll(deployment.getExternalIo().getInOutPort());

            checkUnicity(allPorts, "deployment/application/external_io/port*", "getName");

            for (IOInterface port : allPorts) {
                checkUnicity(port.getOperation(), "deployment/application/external_io/port/" + port.getName(), "getId");
                checkUnicity(port.getOperation(), "deployment/application/external_io/port/" + port.getName(), "getName");
            }
        }
    }

    private static void checkUnicity(Collection<? extends Object> input, String parentName, String methodName) {

        Set<String> identifiers = new HashSet<String>();

        for (Object obj : input) {
            try {
                String id = obj.getClass().getMethod(methodName).invoke(obj).toString();
                if (!identifiers.add(id)) {
                    throw new InconsistentModelError(
                            String.format("Value '%s' for attribute '%s' is not unique in its scope '%s'", id, methodName.replaceAll("get", "").toLowerCase() 
                                    ,parentName));
                }
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

}
