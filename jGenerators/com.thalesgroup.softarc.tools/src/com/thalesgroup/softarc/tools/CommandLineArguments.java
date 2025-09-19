/* Copyright (c) 2025 THALES -- All rights reserved */

/*
 * Copyright (c) 2011 THALES.
 * All rights reserved.
 */
package com.thalesgroup.softarc.tools;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Manages a set of command line arguments
 */
@Requirement(ids = "GenFramework-SRS-REQ-004")
public final class CommandLineArguments {
    /**
     * Argument of command line
     */
    public static final class Argument {
        /**
         * Ctor.
         *
         * @param argName argument
         * @param shortName argument
         * @param argType argument
         * @param min argument
         * @param max argument
         * @param description argument
         * @param forInternalUseOnly argument
         * @param defaultValue argument
         */
        Argument(String argName, char shortName, Class<?> argType, int min, int max, String description,
                boolean forInternalUseOnly, Object defaultValue) {
            _name = argName;
            _shortName = shortName;
            _type = argType;
            _min = min;
            _max = max;
            _description = description;
            _forInternalUseOnly = forInternalUseOnly;
            _defaultValue = defaultValue;
        }

        /**
         * @return the argument name.
         */
        public String getName() {
            return _name;
        }

        /**
         * @return the argument shortName.
         */
        public char getShortName() {
            return _shortName;
        }

        /**
         * @return the argument Type.
         */
        public Class<?> getType() {
            return _type;
        }

        /**
         * @return the argument min value.
         */
        public int getMin() {
            return _min;
        }

        /**
         * @return the argument max value.
         */
        public int getMax() {
            return _max;
        }

        /**
         * @return the argument values.
         */
        public List<Object> getValues() {
            return _values;
        }

        /**
         * @return the argument defaultValue.
         */
        public Object getDefaultValue() {
            return _defaultValue;
        }

        /**
         * @return the argument description.
         */
        public String getDescription() {
            return _description;
        }

        /**
         * Set the argument description.
         */
        public void setDescription(String newDesc) {
            _description = newDesc;
        }

        /**
         * @return the argument forInternalUseOnly status.
         */
        public boolean isForInternalUseOnly() {
            return _forInternalUseOnly;
        }

        /**
         * @return the argument isActivated status.
         */
        public boolean isActivated() {
            return _isActivated;
        }

        /**
         * name.
         */
        private final String _name;
        /**
         * shortname
         */
        private final char _shortName;
        /**
         * type
         */
        private final Class<?> _type;
        /**
         * min
         */
        private int _min;
        /**
         * max
         */
        private int _max;
        /**
         * values
         */
        private final List<Object> _values = new LinkedList<Object>();
        /**
         * default values
         */
        private final Object _defaultValue;
        /**
         * description
         */
        private String _description;
        /**
         * Don't touch!
         */
        private final boolean _forInternalUseOnly;
        /**
         * is enabled?
         */
        private boolean _isActivated;

    }

    /**
     * Add one parameter specification to the command line analyzer.
     *
     * @param argName the name of the argument as <code>--output-file</code>
     * @param shortName the one character option as <code>-o</code>
     * @param argType the Java class of the value to parse as <code>String.class</code>
     * @param min the minimum times the option must occurs
     *            <ul>
     *            <li>0: optional</li>
     *            <li>1: mandatory</li>
     *            <li>&gt; 1: more than one</li>
     *            </ul>
     * @param max the maximum times the option may occurs
     *            <ul>
     *            <li>0: throws {@link java.lang.IllegalArgumentException}</li>
     *            <li>if min == 1 and max == 1, the option is mandatory</li>
     *            <li>&gt; 0: the option may be added more than one</li>
     *            </ul>
     * @param description A sentence added to the usage, shown by <code>--help</code>
     * @param forInternalUseOnly No documentation for this parameter
     * @param defaultValue default value
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-073", "GenFramework-SRS-REQ-074", "GenFramework-SRS-REQ-053" })
    public void add(String argName, char shortName, Class<?> argType, int min, int max, String description,
            boolean forInternalUseOnly, Object defaultValue) {
        final Argument arg = new Argument(argName, shortName, argType, min, max, description, forInternalUseOnly, defaultValue);
        _arguments.put(argName, arg);
        if (shortName != '\0') {
            _arguments.put(Character.toString(shortName), arg);
        }
    }

    /**
     * setOptionActivated
     *
     * @param option
     * @param activated
     */
    public void setOptionActivated(String option, boolean activated) {
        _arguments.get(option)._isActivated = activated;
    }

    /**
     * Set the cardinality of the option.
     *
     * @param option
     * @param min
     * @param max
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-069" })
    public void setOptionCardinality(String option, int min, int max) {
        final Argument arg = _arguments.get(option);
        arg._min = min;
        arg._max = max;
    }

    /**
     * completeUsage
     *
     * @param arg argument
     * @param shortdesc argument
     * @param mandatory argument
     * @param optional argument
     */
    private void completeUsage(Argument arg, StringBuilder shortdescMandatory, StringBuilder shortdescOptional,
            StringBuilder mandatory, StringBuilder optional) {
        String line = LONG_OPTION_PREFIX + arg._name;
        if (Character.isLetterOrDigit(arg._shortName))
            line += "|-" + arg._shortName;
        if (arg._type != Boolean.class && !arg._type.isEnum()) {
            line += " <" + arg._type.getSimpleName() + '>';
        }
        if (arg._type.isEnum()) {
            String list = " ";
            for (Object cst : arg._type.getEnumConstants()) {
                list += cst + "|";
            }
            line += list.substring(0, list.length() - 1);
        }
        String text = '\t' + line + ": " + arg._description;
        text += '\n';
        if (arg._min == 0) {
            shortdescOptional.append(' ');
            shortdescOptional.append('[');
            shortdescOptional.append(line);
            shortdescOptional.append(']');
            optional.append(text);
        } else {
            shortdescMandatory.append(' ');
            shortdescMandatory.append(line);
            mandatory.append(text);
        }
    }

    /**
     * get usage
     *
     * @param toolName
     * @return usage string
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-006", "GenFramework-SRS-REQ-014" })
    public String getUsage(String toolName) {
        final StringBuilder shortdescMandatory = new StringBuilder();
        final StringBuilder shortdescOptional = new StringBuilder();
        final StringBuilder mandatory = new StringBuilder("\n== Mandatory arguments ==\n\n");
        final StringBuilder optional = new StringBuilder("\n== Optional arguments ==\n\n");
        for (Entry<String, Argument> e : _arguments.entrySet()) {
            final Argument arg = e.getValue();
            if (e.getKey().length() > 1 && !arg._forInternalUseOnly && arg._isActivated) {
                completeUsage(arg, shortdescMandatory, shortdescOptional, mandatory, optional);
            }
        }
        return String.format("\nusage: %s%s%s\n------\n%s%s", toolName, shortdescMandatory, shortdescOptional, mandatory,
                optional);
    }

    /**
     * check one arg
     *
     * @param args the args
     * @param i the index
     * @param usage the usage
     * @return the next index
     */
    private int check(String[] args, int i, String usage) {
        final boolean shortNameUsed = (args[i].charAt(1) != '-');
        final String argName = args[i].substring(shortNameUsed ? 1 : 2);
        final Argument arg = _arguments.get(argName);
        if (arg == null || !arg._isActivated) {
            throw new CommandLineParsingError(UNEXPECTED_TOKEN + args[i] + DOT_CR + usage);
        }
        return handleArgument(args, i, arg, usage);
    }

    /**
     * check the command line
     *
     * @param args les arguments
     * @param usage l'usage
     * @quand c'est n'imorte quoi
     */
    private void check(String[] args, String usage) {
        int i = 0;
        while (i < args.length) {
            if (args[i].length() > 1 && args[i].charAt(0) == '-') {
                i = check(args, i, usage);
            } else {
                throw new CommandLineParsingError(UNEXPECTED_TOKEN + args[i] + DOT_CR + usage);
            }
            ++i;
        }
    }

    /**
     * Parse ze commande ligne
     *
     * @param toolName arg
     * @param args arg
     * @exception
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-014", "GenFramework-SRS-REQ-140", "GenFramework-SRS-REQ-075" })
    public void parse(String toolName, String[] args) {
        final String usage = getUsage(toolName);
        check(args, usage);
        for (Argument arg : _arguments.values()) {
            final int size = arg._values.size();
            if (!arg._forInternalUseOnly && arg._isActivated && arg._min > size || (size > arg._max && arg._max >= 0)) {
                String errMsg = "";
                if (arg._max == 0) {
                    errMsg = String.format("Unexpected token '--%s'.", arg._name);
                } else if (arg._min > size) {
                    errMsg = String.format("Parsed only %d occurence(s) of option '--%s' instead of minimum %d expected.\n%s",
                            size, arg._name, arg._min, usage);
                } else if (arg._max < size && arg._max >= 0) {
                    errMsg = String.format("Parsed %d occurence(s) of option '--%s' " + "instead of maximum %d expected.\n%s",
                            size, arg._name, arg._max, usage);
                } else {
                    errMsg = String.format("Parsed %d occurence(s) of option '--%s' instead of %d%s expected.\n%s", size,
                            arg._name, arg._min, (arg._min < arg._max ? (" (and up to " + arg._max + ")") : ""), usage);
                }
                throw new CommandLineParsingError(errMsg);
            }
        }
    }

    /**
     * Clear the values set by {@link #parse(String, String[])}
     */
    public void clearValues() {
        for (Argument arg : _arguments.values()) {
            arg._values.clear();
        }
    }

    /**
     * test if args has Argument argument
     *
     * @param argument argument
     * @return true or false
     */
    @Requirement(ids = "GenFramework-SRS-REQ-081")
    public boolean hasArgument(String argument) {
        return _arguments.get(argument)._values.size() > 0;
    }

    /**
     *
     * @param <T>
     * @param argName
     * @param values
     * @return
     */
    @SuppressWarnings("unchecked")
    @Requirement(ids = "GenFramework-SRS-REQ-081")
    public <T> List<T> getAll(String argName, List<T> values) {
        values.clear();
        final Argument arg = _arguments.get(argName);
        if (!arg._values.isEmpty()) {
            values.addAll((Collection<T>) arg._values);
        } else if (arg._defaultValue != null) {
            values.add((T) arg._defaultValue);
        }
        return values;
    }

    /**
     *
     * @param <T>
     * @param argName
     * @return
     */
    @SuppressWarnings("unchecked")
    @Requirement(ids = "GenFramework-SRS-REQ-081")
    public <T> T getFirst(String argName) {
        final Argument arg = _arguments.get(argName);
        T res = null;
        if (arg._values.isEmpty()) {
            res = (T) arg._defaultValue;
        } else {
            res = (T) arg._values.get(0);
        }

        return res;
    }

    /**
     *
     * @param <T>
     * @param argShortName
     * @param values
     * @return
     */
    @Requirement(ids = "GenFramework-SRS-REQ-081")
    public <T> List<T> getAll(char argShortName, List<T> values) {
        return getAll(Character.toString(argShortName), values);
    }

    /**
     *
     * @param <T>
     * @param argShortName
     * @return
     */
    @Requirement(ids = "GenFramework-SRS-REQ-081")
    public <T> T getFirst(char argShortName) {
        return this.<T> getFirst(Character.toString(argShortName));
    }

    /**
     *
     * @param args
     * @param index
     * @param arg
     * @param usage
     * @return
     */
    @Requirement(ids = { "GenFramework-SRS-REQ-053", "GenFramework-SRS-REQ-070", "GenFramework-SRS-REQ-140" })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int handleArgument(String[] args, int index, Argument arg, String usage) {
        int i = index;
        if (arg._type == Boolean.class || arg._type == boolean.class) {
            if (arg._shortName == AbstractGenerator.ARGUMENT_KEY_SHORT_HELP || arg._name == AbstractGenerator.ARGUMENT_KEY_HELP) {
                throw new CommandLineParsingError(usage);
            }
            arg._values.add(Boolean.TRUE);
        } else {
            ++i;
            if (i == args.length || args[i].startsWith(LONG_OPTION_PREFIX)) {
                throw new CommandLineParsingError("Option '--" + arg._name + "' needs an argument.\n" + usage);
            }
            try {
                if (arg._type.isEnum()) {
                    arg._values.add(Enum.valueOf((Class<Enum>) arg._type, args[i]));
                } else {
                    if (arg._type.equals(File.class)) {
                        // for args of type File, allow a "path" (filenames separated by ";")
                        // to be provided as a single option.
                        String fileNames[] = args[i].split(System.getProperty("path.separator"));
                        for (String fn : fileNames) {
                            File f = new File(fn);
                            // unless the file name is absolute, consider it from 'basedir'
                            if (!f.isAbsolute())
                                f = new File(_basedir, fn);
                            arg._values.add(f);
                        }
                    } else {
                        final Constructor<?> ctor = arg._type.getConstructor(new Class<?>[] { String.class });
                        arg._values.add(ctor.newInstance(args[i]));
                    }
                }
            } catch (Throwable t) {
                // Build error message for the special case of enum : we have to
                // quote each possible value
                if (arg._type.isEnum()) {
                    String errorMessage = OPTION_VALUE + args[i] + "' specified for option '--" + arg._name
                            + "' shall be one of: ";
                    final Object enumvalues[] = arg._type.getEnumConstants();
                    final Object last = enumvalues[enumvalues.length - 1];
                    for (Object o : enumvalues) {
                        errorMessage += o.toString() + (o != last ? ", " : "");
                    }
                    throw new CommandLineParsingError(errorMessage);
                }
                throw new CommandLineParsingError(OPTION_VALUE + args[i] + "' specified after '--" + arg._name
                        + "' doesn't match expected " + arg._type.getSimpleName() + " type.\n" + usage);
            }
        }
        return i;
    }

    File _basedir;

    public void setBaseDir(java.io.File baseDir) {
        _basedir = baseDir;
    }

    /**
     * @return the command line arguments map.
     */
    public Map<String, Argument> getArguments() {
        return _arguments;
    }

    /**
     * Arguments
     */
    private final Map<String, Argument> _arguments = new LinkedHashMap<String, Argument>();

    /**
     * Long prefix
     */
    private static final String LONG_OPTION_PREFIX = "--";

    /**
     * A point then a dot. Yes, this is a well documented item, isn't?
     */
    private static final String DOT_CR = "'.\n";
    /**
     * unexpected token
     */
    private static final String UNEXPECTED_TOKEN = "Unexpected token '";
    /**
    *
    */
    private static final String OPTION_VALUE = "Option value '";

}
