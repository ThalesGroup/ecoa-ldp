/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class LogHandler extends StreamHandler {

    public LogHandler(OutputStream s) {
        setFormatter(new logFormatter());
        setLevel(Level.ALL);
        setOutputStream(s);
    }

    static class logFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%s: %s: %s: %s\n", levelName(record.getLevel()),
                    DATE_FRMTTR.format(new Date(record.getMillis())), record.getLoggerName(), record.getMessage());
        }

        private String levelName(Level l) {
            if (l == Level.FINEST)
                return "DEBUG";
            if (l == Level.SEVERE)
                return "ERROR";
            return l.getName();
        }
    }

    private static final DateFormat DATE_FRMTTR = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

}
