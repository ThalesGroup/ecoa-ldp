/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractLogger {

    /**
     * Logging channel definition. 1/ Additional handlers may be created before calling {@link #execute(String[])}, using
     * {@link Logger#addHandler(java.util.logging.Handler)}. 2/ When {@link #execute(String[])} is called, a handler is created
     * for each -o option in the command line arguments. 3/ If no handler exists after 1/ and 2/, then a handler on stderr is
     * created.
     * 
     */
    public Logger _log;

    protected void flushLogs() throws SecurityException {
        for (Handler h : _log.getHandlers())
            h.flush();
        for (Handler h : _report.getHandlers())
            h.flush();
    }

    protected void configureLogging() throws IOException {

        // Set log files
        ArrayList<File> files = new ArrayList<File>();
        _arguments.getAll(ARGUMENT_KEY_LOG_FILE, files);
        for (File log : files)
            try {
                _log.addHandler(new LogHandler(outputStream(log)));
            } catch (Exception e) {
                throw new IOException("Cannot open log file '" + log.toURI().getPath() + "'.");
            }

        _arguments.getAll(ARGUMENT_KEY_ERR_FILE, files);
        for (File log : files)
            try {
                LogHandler handler = new LogHandler(outputStream(log));
                handler.setLevel(Level.WARNING);
                _log.addHandler(handler);
            } catch (Exception e) {
                throw new IOException("Cannot open err file '" + log.toURI().getPath() + "'.");
            }

        if (_log.getHandlers().length > 1)
            _log.removeHandler(defaultHandler);

        info("Starting %s ...", _specificGeneratorName);

        // Set report file
        _arguments.getAll(ARGUMENT_KEY_REPORT_FILE, files);
        for (File log : files)
            try {
                _report.addHandler(new ReportHandler(outputStream(log)));
            } catch (Exception e) {
                throw new IOException("Cannot open report file '" + log.toURI().getPath() + "'.");
            }
        if (_report.getHandlers().length == 0)
            _report.addHandler(new ReportHandler(System.out));
    }

    private OutputStream outputStream(File f) throws FileNotFoundException {
        if (f.getPath().equals("stdout"))
            return System.out;
        if (f.getPath().equals("stderr"))
            return System.err;
        return new FileOutputStream(f);
    }

    /**
     * Report a status of a generated file.
     * 
     * @param file
     *            the file concerned by the status.
     * @param status
     *            the status of the generated file.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-020", "GenFramework-SRS-REQ-136", "GenFramework-SRS-REQ-137" })
    public void report(File file, ReportStatus status) {
        _report.log(Level.INFO, file.toURI().getPath(), status);
    }

    /**
     * Report an information to the execution log.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-138", "GenFramework-SRS-REQ-139" })
    public void info(String format, Object... arguments) {
        _log.info(String.format(format, arguments));
    }

    /**
     * Report debug message to the execution log.
     */
    public void debug(String format, Object... arguments) {
        _log.finest(String.format(format, arguments));
    }

    /**
     * Report a warning to the execution log.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-138", "GenFramework-SRS-REQ-139" })
    public void warning(String format, Object... arguments) {
        _log.warning(String.format(format, arguments));
    }

    /**
     * Convenience method to raise an InconsistentModelError.
     * 
     * @throws InconsistentModelError
     */
    public void errorModel(String format, Object... arguments) throws InconsistentModelError {
        throw new InconsistentModelError(String.format(format, arguments));
    }

    /**
     * Report an error to the execution log.
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-138", "GenFramework-SRS-REQ-139" })
    public void error(String format, Object... arguments) {
        _log.severe(String.format(format, arguments));
    }

    /**
     * Reporting channel definition. Default : System.out.
     */
    public Logger _report;
    protected LogHandler defaultHandler;
    /**
     * Specific generator name.
     */
    protected final String _specificGeneratorName;
    /**
     * Generator command line arguments (names and values).
     */
    protected final CommandLineArguments _arguments = new CommandLineArguments();
    public static final String ARGUMENT_KEY_LOG_FILE = "logfile";
    public static final char ARGUMENT_KEY_SHORT_LOG_FILE = 'o';
    public static final String ARGUMENT_KEY_ERR_FILE = "error";
    public static final char ARGUMENT_KEY_SHORT_ERR_FILE = 'e';
    public static final String ARGUMENT_KEY_REPORT_FILE = "report";
    public static final char ARGUMENT_KEY_SHORT_REPORT_FILE = 'r';

    public AbstractLogger(String toolName) {
        super();
        _specificGeneratorName = toolName;

        _log = Logger.getLogger(toolName);
        _log.setUseParentHandlers(false);
        _report = Logger.getLogger(toolName + ".report");
        _report.setUseParentHandlers(false);

        for (Handler handler : _log.getHandlers()) {
            _log.removeHandler(handler);
        }
        // set temporary handler (in case of error befor log configuration is finished)
        defaultHandler = new LogHandler(System.err);
        _log.addHandler(defaultHandler);

    }

}