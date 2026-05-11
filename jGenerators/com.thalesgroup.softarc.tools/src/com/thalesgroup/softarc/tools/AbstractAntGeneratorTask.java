/* Copyright (c) 2025 THALES -- All rights reserved */

/*
 * Copyright (c) 2011 THALES.
 * All rights reserved.
 */
package com.thalesgroup.softarc.tools;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Base class for a generator packaged as an Ant task.
 * 
 */
public abstract class AbstractAntGeneratorTask extends Task {

    protected abstract AbstractGenerator newGenerator() throws Throwable;

    private int antLogLevel = Level.WARNING.intValue();

    public AbstractAntGeneratorTask() {
    }

    @Override
    public void execute() throws BuildException {
        int retcode;
        try {
            AbstractGenerator generator = newGenerator();

            generator.getArguments().setBaseDir(getProject().getBaseDir());
            
            final List<LogRecord> logRecords = new ArrayList<LogRecord>();
            final List<LogRecord> reportRecords = new ArrayList<LogRecord>();
            final List<LogRecord> allRecords = new ArrayList<LogRecord>();

            generator._log.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                	logRecords.add(record);
                	allRecords.add(record);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws SecurityException {
                }
            });
            
            generator._report.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                	reportRecords.add(record);
                	allRecords.add(record);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws SecurityException {
                }
            });
            
            retcode = generator.execute(_antArgs.toArray(new String[_antArgs.size()]));
            
            printReport(reportRecords);
            printLog(logRecords, antLogLevel);
            
            printSummary(allRecords, antLogLevel);
                        
        } catch (Throwable t) {
            throw new BuildException(t);
        }
        if (retcode != 0)
            throw new BuildException();
    }

    public void setLevel(String level) {
        if (level.equalsIgnoreCase("off"))
            antLogLevel = Level.OFF.intValue();
        else if (level.equalsIgnoreCase("error"))
            antLogLevel = Level.SEVERE.intValue();
        else if (level.equalsIgnoreCase("warning"))
            antLogLevel = Level.WARNING.intValue();
        else if (level.equalsIgnoreCase("info"))
            antLogLevel = Level.INFO.intValue();
        else if (level.equalsIgnoreCase("debug") || level.equalsIgnoreCase("all"))
            antLogLevel = Level.ALL.intValue();
        else
            throw new BuildException("'level' shall be in (off,error,warning,info,debug,all) and is: " + level);
    }
    
    private void print(String recordMessage, int recordLevel, int logLevel) {
    	if (recordLevel >= logLevel) {
            if (recordLevel < Level.INFO.intValue())
                log(recordMessage, Project.MSG_DEBUG);
            else if (recordLevel == Level.INFO.intValue())
                log(recordMessage, Project.MSG_INFO);
            else if (recordLevel == Level.WARNING.intValue())
                log(recordMessage, Project.MSG_WARN);
            else if (recordLevel == Level.SEVERE.intValue())
                log(recordMessage, Project.MSG_ERR);
    	}
    }
    
    private String levelName(Level l) {
        if (l == Level.FINEST)
            return "DEBUG";
        if (l == Level.SEVERE)
            return "ERROR";
        return l.getName();
    }
    
    private void printLog(List<LogRecord> logRecords, int logLevel) {
    	for (LogRecord record : logRecords) {
    		print(String.format("[%s] %s\n", levelName(record.getLevel()), record.getMessage()), record.getLevel().intValue(), logLevel);
    	}
    }
    
    private void printReport(List<LogRecord> logRecords) {
    	for (LogRecord record : logRecords) {
    		print(String.format("%s %s\n", record.getParameters()[0], record.getMessage()), record.getLevel().intValue(), Level.INFO.intValue());
    	}
    }
    
    private void printSummary(List<LogRecord> logRecords, int logLevel) {
    	long nbWarning = 0;
    	long nbErrors = 0;
    	for (LogRecord record : logRecords) {
	        int v = record.getLevel().intValue();
            if (v < Level.INFO.intValue())
                ;
            else if (record.getLevel() == Level.INFO)
                ;
            else if (record.getLevel() == Level.WARNING)
            	nbWarning++;
            else if (record.getLevel() == Level.SEVERE)
            	nbErrors++;
    	}
    	if (nbWarning > 0)
    		print("[STATUS] Task ended with " + nbWarning + " warning(s).", Level.WARNING.intValue(), logLevel) ;   
    	if (nbErrors > 0)
    		print("[STATUS] Task ended with " + nbErrors + " errors(s).", Level.SEVERE.intValue(), logLevel) ;   
    }

    /**
     * Set the log file path.
     * 
     * @param logPath
     *            : the log file path.
     */
    public void setLog(String logPath) {
        _antArgs.add(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_LOG_FILE);
        _antArgs.add(logPath);
    }

    /**
     * Set the report file path.
     * 
     * @param reportPath
     *            : the report file path.
     */
    public void setReport(String reportPath) {
        _antArgs.add(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_REPORT_FILE);
        _antArgs.add(reportPath);
    }

    /**
     * Set the debug state.
     * 
     * @param debug
     *            : the new debug state value.
     */
    public void setDebug(boolean debug) {
        if (debug) {
            _antArgs.add(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_DEBUG);
        } else {
            while (_antArgs.contains(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_DEBUG)) {
                _antArgs.remove(_antArgs.indexOf(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_DEBUG) + 1);
                _antArgs.remove(ANTLR_ARGUMENT_PREFIX + AbstractGenerator.ARGUMENT_KEY_DEBUG);

            }
        }
    }

    /**
     * The ANT arguments list.
     */
    protected final List<String> _antArgs = new LinkedList<String>();

    /**
     * ANTLR Argument prefix constant. Value: "--".
     */
    public static final String ANTLR_ARGUMENT_PREFIX = "--";

}
