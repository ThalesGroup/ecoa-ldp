/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s30.gentype;

public enum KindOfDirectory {

    INC("inc"), // not used in Java
    SRC("src"), //
    INC_GEN("inc-gen"), // not used in Java
    SRC_GEN("src-gen"), // used for Java only
    ROOT(""); // used for Rust only

    private KindOfDirectory(String propertyName) {
        dirName = propertyName;
    }

    public String getDirName() {
        return dirName;
    }

    private final String dirName;

}
