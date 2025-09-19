/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.io.IOException;

public class ValueWriters {

    final public ValueWriter c = new ValueWriterC();
    final public ValueWriter cpp = new ValueWriterCPP();

    public ValueWriter getValueWriter(String language) throws IOException {
        switch (language) {
            case "C":
            case "C_ECOA":
                return c;
            case "CPP":
                return cpp;
        }
        throw new Error("cannot write values in language " + language);
    }
}
