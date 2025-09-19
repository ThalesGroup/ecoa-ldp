/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

class ReportHandler extends StreamHandler {

    ReportHandler(OutputStream s) {
        super(s, new reportFormatter());
        setLevel(Level.ALL);
    }

    static class reportFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%s %s\n", record.getParameters()[0], record.getMessage());
        }
    }

}
