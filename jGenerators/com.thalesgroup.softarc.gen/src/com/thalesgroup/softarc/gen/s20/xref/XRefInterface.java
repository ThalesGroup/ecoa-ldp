/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s20.xref;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.ExternInterface;
import com.thalesgroup.softarc.sf.Operation;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.TypeDefinition;

public class XRefInterface extends AbstractPass {

    @Override
    public void execute() {
        resolveUsedLibraries(context.system.getInterface());
    }

    private void resolveUsedLibraries(ExternInterface i) {
        for (Operation op : i.getOperations()) {
            for (Parameter p : op.getInParameters()) {
                resolveUsedLibrary(i, p.getType());
            }
            for (Parameter p : op.getOutParameters()) {
                resolveUsedLibrary(i, p.getType());
            }
            if (op instanceof OperationData) {
                resolveUsedLibrary(i, ((OperationData) op).getType());
            }
        }
    }

    private void resolveUsedLibrary(ExternInterface i, TypeDefinition td) {
        Component l = td.getParent();
        if (!l.getIsPredefLib() && !i.getUsedLibraries().contains(l)) {
            i.getUsedLibraries().add(l);
        }
    }
}
