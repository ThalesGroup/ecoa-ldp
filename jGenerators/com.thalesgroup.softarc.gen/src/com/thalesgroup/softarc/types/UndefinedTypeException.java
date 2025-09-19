/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

public class UndefinedTypeException extends Exception {

    private static final long serialVersionUID = 3499504485677763573L;
    public String name;

    public UndefinedTypeException(String name) {
        this.name = name;
    }

}
