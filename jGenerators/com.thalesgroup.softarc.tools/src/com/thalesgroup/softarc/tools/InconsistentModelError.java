/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

public class InconsistentModelError extends Error {

    public InconsistentModelError(String message) {
        super(message);
    }

    public InconsistentModelError(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 434371099433831329L;

}
