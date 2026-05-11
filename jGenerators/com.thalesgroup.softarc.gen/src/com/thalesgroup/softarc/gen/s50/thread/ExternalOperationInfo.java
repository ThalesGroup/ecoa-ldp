package com.thalesgroup.softarc.gen.s50.thread;

import com.thalesgroup.softarc.tools.InconsistentModelError;

import technology.ecoa.model.deployment.IOInterface;
import technology.ecoa.model.deployment.IOOperation;

public class ExternalOperationInfo {

    public IOInterface channel;
    public IOOperation operation;

    /**
     * 'id' for external operations in ECOA standard is a typed as xsd:string, but for SOFTARC only int32 values are accepted
     */
    long getId() {
        try {
            long result = Long.parseLong(operation.getId());
            if (result < 0 || result > 2147483647)
                throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new InconsistentModelError(
                    String.format("Id for external operation '%s' ('%s') is not a numeric value inside int32 limits",
                            operation.getName(), operation.getId()));
        }
    }
}