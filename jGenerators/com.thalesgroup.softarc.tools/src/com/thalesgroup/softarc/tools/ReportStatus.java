/* Copyright (c) 2025 THALES -- All rights reserved */

/*
 * Copyright (c) 2011 THALES.
 * All rights reserved.
 */
package com.thalesgroup.softarc.tools;

/**
 * This class defines the different status types. UPDATED, IDENTICAL, CREATED, ERROR, SKIPPED.
 * 
 */
public enum ReportStatus {
    /**
     * The generated file has been changed since beginning of generator execution.
     */
    UPDATED("[ UPDATED ]"),
    /**
     * The generated file has not been changed since beginning of generator execution.
     */
    IDENTICAL("[IDENTICAL]"),
    /**
     * The generated file has been created. It was not existing before generator execution.
     */
    CREATED("[ CREATED ]"),
    /**
     * An error occurs during file generation.
     */
    ERROR("[  ERROR  ]"),
    /**
     * This file has been skipped, because there is no need to generate it.
     */
    SKIPPED("[ SKIPPED ]"),
    /**
     * This file has been skipped, because it is a user-modifiable file and it already exists.
     */
    PRESERVED("[PRESERVED]");

    /**
     * Private constructor.
     * 
     * @param symbol : instanciation value.
     */
    private ReportStatus(String symbol) {
        _symbol = symbol;
    }

    @Override
    public String toString() {
        return _symbol;
    }

    /**
     * Report status enum string value.
     */
    private final String _symbol;

}
