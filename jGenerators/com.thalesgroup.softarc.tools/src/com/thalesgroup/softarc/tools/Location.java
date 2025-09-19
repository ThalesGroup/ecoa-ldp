/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.File;

public class Location {
    
    public File file;
    public int line; // starting from ** 1 **

    public Location(File file, int line) {
        this.file = file;
        this.line = line;
    }
}