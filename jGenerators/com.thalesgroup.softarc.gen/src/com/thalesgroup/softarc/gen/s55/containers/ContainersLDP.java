/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s55.containers;

import java.io.IOException;

import com.thalesgroup.softarc.sf.*;
import com.thalesgroup.softarc.sf.impl.*;

/**
 * Création des objets de type Container (intersection d'un exécutable et d'une implémentation de composant). Un même conteneur
 * peut servir à plusieurs instances.
 */
public class ContainersLDP extends ContainersCore {

    private static final int MIN_SIZE_BUFFER_OUT = 50000;

    @Override
    public void execute() throws IOException {

        computeIds();

        // Create a "global"  executable containing all the threads and all the instances
        Executable exec = new QExecutable();
        for (Platform platform : context.system.getMapping().getPlatforms()) {
            for (Executable e : platform.getExecutables()) {
                exec.getThreads().addAll(e.getThreads());
                exec.getInstances().addAll(e.getInstances());
                exec.setParent(platform);
                exec.setMaxBufferOut(exec.getMaxBufferOut() + e.getMaxBufferOut());
            }
        }
        
        // Add minimum bufferSizeOut to manage autostart for instances
        exec.setMaxBufferOut(exec.getMaxBufferOut() + MIN_SIZE_BUFFER_OUT);
        
        context.system.getMapping().setGlobalExecutable(exec);

        createSortedOperationsForInstances(exec.getInstances());

        for (Platform platform : context.system.getMapping().getPlatforms()) {
            for (Executable e : platform.getExecutables()) {
                listComponentTypes(e);
            }
        }
        listComponentTypes(exec);

        createContainers(exec);

        createOperationMaps(exec);

        computeNotificationInfo(exec);

        computeObjectCounts();
    }
}