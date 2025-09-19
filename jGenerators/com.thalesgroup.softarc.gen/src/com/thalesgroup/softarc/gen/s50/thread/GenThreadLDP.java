/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread;

import java.io.IOException;
import java.util.Map;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.Operation;

// Calcule les dimensionnements techniques associés aux instances et aux threads.

public class GenThreadLDP extends AbstractPass {

    static final int SARC_LDP_MAX_OPERATION_SIZE = 256*1024;

    @Override
    public void execute() throws IOException {

        // Calcul du contexte opérationnel
        OperationManager operationManager = new OperationManager(false, false);
        Map<Long, OperationContext> operationsContexts = operationManager.generateOperationContext(context.system.getAssembly());

        //
        ThreadManagerLDP threadManager = new ThreadManagerLDP(this, operationsContexts, operationManager);
        threadManager.initializeMapping();
        threadManager.finalizeMapping();

        // in case of sizing overflow, then generate the mapping file, and stop generation
        if (threadManager.checkSizingOverflow()) {
            errorModel("Memory usage overflow; generation stopped");
        }
        
        checkOperationSize();
    }

    /**
     * Check max size for an operation
     */
    private void checkOperationSize() {
        for (Component cmp : context.system.getComponents()) {
            for (Operation op : cmp.getOperations())
                if (op.getSize() > SARC_LDP_MAX_OPERATION_SIZE)
                    errorModel("Size of parameters of operation %s (%d kb) exceed LDP limitation (%d kb)", op.toString(),
                            op.getSize() / 1024, SARC_LDP_MAX_OPERATION_SIZE / 1024);
        }
    }

}
