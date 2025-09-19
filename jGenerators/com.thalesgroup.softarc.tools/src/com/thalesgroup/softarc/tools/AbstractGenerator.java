/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.misc.STMessage;

/**
 * Base class for a code generator.
 *
 * <p>Manages logging and errors, command line arguments, file generation templates (optionally
 * crypted).
 */
public abstract class AbstractGenerator extends AbstractLogger {

    private static final String CHARSET_NAME = "UTF-8";
    private static final Charset CHARSET = Charset.forName(CHARSET_NAME);

    private final class ErrorListerner implements STErrorListener {
        @Override
        public void runTimeError(STMessage msg) {
            _generationErrors.add(msg);
        }

        @Override
        public void internalError(STMessage msg) {
            _generationErrors.add(msg);
        }

        @Override
        public void compileTimeError(STMessage msg) {
            _generationErrors.add(msg);
        }

        @Override
        // CHECKSTYLE:OFF
        public void IOError(STMessage msg) {
            _generationErrors.add(msg);
        }
        // CHECKSTYLE:ON
    }

    /**
     * Map used as input of templates. Id : name of the attribute used in templates. Value : Value
     * of this attribute for his template instance.
     */
    private final Map<String, Object> _tmplAttrs = new HashMap<String, Object>();

    /** Used to know wether the generator is launched from a jar or not */
    private String tmpDir = null;

    /**
     * Debugging aid: set this to the name of a .stg file to be able to inspect it before execution
     * !
     */
    public String debugTemplate = null;

    /**
     * The main abstract method of this class. This method must be implemented to do the real work
     * of the specific generator.
     */
    public abstract void generate() throws Exception;

    /** Called just before generate(). */
    protected abstract void initialize() throws Exception;

    /**
     * @param toolName Name of this generator (used for logging)
     * @param templatesRoot Root relative resource path for all templates
     */
    public AbstractGenerator(String toolName, String templatesRoot) {
        super(toolName);

        _templatesRoot = templatesRoot;
        _strngTmplErrLstnr = new ErrorListerner();

        // options common to all generators (activated by default)
        addArgument(
                ARGUMENT_KEY_LOG_FILE,
                ARGUMENT_KEY_SHORT_LOG_FILE,
                File.class,
                0,
                10,
                "Sets up log file to write to.",
                null);
        addArgument(
                ARGUMENT_KEY_ERR_FILE,
                ARGUMENT_KEY_SHORT_ERR_FILE,
                File.class,
                0,
                10,
                "Sets up log file to write to (errors and warnings only).",
                null);
        addArgument(
                ARGUMENT_KEY_REPORT_FILE,
                ARGUMENT_KEY_SHORT_REPORT_FILE,
                File.class,
                0,
                10,
                "Sets up report file to write to.",
                null);
        addArgument(
                ARGUMENT_KEY_DEBUG,
                ARGUMENT_KEY_SHORT_DEBUG,
                Boolean.class,
                0,
                1,
                "Turns on debug mode.",
                true,
                Boolean.FALSE);
        addArgument(
                ARGUMENT_KEY_HELP,
                ARGUMENT_KEY_SHORT_HELP,
                Boolean.class,
                0,
                1,
                "Displays this help and exits.",
                true,
                Boolean.FALSE);

        _arguments.setOptionActivated(ARGUMENT_KEY_DEBUG, true);
        _arguments.setOptionActivated(ARGUMENT_KEY_HELP, true);
        _arguments.setOptionActivated(ARGUMENT_KEY_LOG_FILE, true);
        _arguments.setOptionActivated(ARGUMENT_KEY_ERR_FILE, true);
        _arguments.setOptionActivated(ARGUMENT_KEY_REPORT_FILE, true);

        InputStream stream = this.getClass().getResourceAsStream("/templateList");

        if (stream != null) {
            tmpDir = System.getProperty("java.io.tmpdir") + "/" + System.nanoTime() + "/";
            new File(tmpDir).mkdirs();

            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String strLine = null;

            try {
                while ((strLine = br.readLine()) != null) {
                    if (!new File(strLine).exists()) {
                        debug("decrypting template: %s", strLine);
                        decrypt(strLine, tmpDir);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                tmpDir = null;
            }
        } else {
            // if "templateList" cannot be loaded as resource, then tmpDir will not be used; the
            // template files are considered not
            // encrypted.
            tmpDir = null;
        }
    }

    /**
     * Add an argument to the command line parser.
     *
     * @param argName the command line long commutator, like 'assembly' (without --)
     * @param shortName the command line short commutator, like 'a' (without -)
     * @param argType the expected argument value type, like File.class
     * @param min the minimum cardinality, 0 means optional
     * @param max the maximum cardinality
     * @param description this description will be prompted when the end-user uses the '--help'
     *     argument
     * @param defaultValue default value
     */
    @Requirement(ids = {"GenFramework-SRS-REQ-073", "GenFramework-SRS-REQ-004"})
    public void addArgument(
            String argName,
            char shortName,
            Class<?> argType,
            int min,
            int max,
            String description,
            Object defaultValue) {
        addArgument(argName, shortName, argType, min, max, description, false, defaultValue);
    }

    protected void addArgument(
            String argName,
            char shortName,
            Class<?> argType,
            int min,
            int max,
            String description,
            boolean forInternalUseOnly,
            Object defaultValue) {
        _arguments.add(
                argName,
                shortName,
                argType,
                min,
                max,
                description,
                forInternalUseOnly,
                defaultValue);
    }

    /**
     * Returns the command line parsing utility object.
     *
     * @return the command line parsing utility object
     */
    public CommandLineArguments getArguments() {
        return _arguments;
    } // CommandLineArguments getArguments()

    /**
     * Entry method of all generators. Typically called as follow:
     *
     * <pre>
     * public static void main(String[] args) throws JAXBException {
     *     System.exit(new MyGenerator().execute(args));
     * }
     * </pre>
     *
     * @param args the command line options and arguments
     * @return 0 if success, any other value if failure
     */
    public final int execute(String[] args) {
        int retCode = 1;

        try {
            _arguments.clearValues();
            _arguments.parse(_specificGeneratorName, args);
            configureLogging();

            StringBuilder sb = new StringBuilder("with args:");
            for (String a : args) sb.append(" " + a);
            info(sb.toString());

            // Debug?
            Boolean debug = _arguments.getFirst(ARGUMENT_KEY_DEBUG);
            _debug = (debug != null) && debug.booleanValue();
            STGroup.verbose = _debug;

            initialize();
            generate();
            if (_nbGenerationErrors == 0) {
                info("successfully done.");
                retCode = 0;
            } else {
                info("generation failed : %d error(s) raised.", _nbGenerationErrors);
            }
        }

        // first, errors that are the user's responsibility
        catch (InconsistentModelError | IOException | CommandLineParsingError x) {
            error("%s: %s", x.getClass().getSimpleName(), x.getMessage());
        }
        // junit assertion : do not catch it (for junit tests)
        catch (AssertionError assertion) {
            throw assertion;
        }
        // all other errors are internal errors and should not happen
        catch (Throwable x) {
            StackTraceElement es[] = x.getStackTrace();
            for (StackTraceElement e : es) _log.severe(e.toString());
            error("%s: %s", x.getClass().getSimpleName(), x.getMessage());
            error("Internal error, please contact SOFTARC development team");
        }

        flushLogs();

        if (tmpDir != null) {
            deleteDirectory(new File(tmpDir));
        }
        return retCode;
    }

    public boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    /**
     * StringTemplate's user may choose other separator than the default '\lt;' and '\&gt;'.
     *
     * @param start left separator
     * @param stop right separator
     */
    public void setSeparators(char start, char stop) {
        _separatorStart = start;
        _separatorStop = stop;
    }

    private STGroupFile loadTemplateGroup(String templateGroupName) throws IOException {
        String filename = _templatesRoot + '/' + templateGroupName + ".stg";
        String key = filename + "#" + _separatorStart + "#" + _separatorStop;
        STGroupFile group = templatesCache.get(key);

        if (group == null) {
            URL url = getClass().getClassLoader().getResource(filename);
            if (url != null) {
                info("Loading uncrypted template: %s", filename);
                group = new STGroupFile(url, CHARSET_NAME, _separatorStart, _separatorStop);
            } else {
                info("Loading template: %s", filename);
                if (tmpDir != null) {
                    filename = tmpDir + filename;
                }
                group = new STGroupFile(filename, _separatorStart, _separatorStop);
            }

            group.setListener(_strngTmplErrLstnr);
            templatesCache.put(key, group);
        }

        return group;
    }

    private static final String PASSPHRASE = "correct horse battery staple";

    private String decrypt(String in, String basedir)
            throws NoSuchAlgorithmException, IOException, NoSuchPaddingException,
                    InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        MessageDigest digest = MessageDigest.getInstance("SHA");
        digest.update(PASSPHRASE.getBytes());
        SecretKeySpec key = new SecretKeySpec(digest.digest(), 0, 16, "AES");

        InputStream istream = this.getClass().getResourceAsStream("/crypted/" + in);

        byte[] input = null;
        {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            byte chunk[] = new byte[1024];
            int read = 0;

            while (true) {
                read = istream.read(chunk);
                if (read == -1) {
                    break;
                } else if (read > 0) {
                    ostream.write(chunk, 0, read);
                }
            }

            input = ostream.toByteArray();
        }

        // byte[] input = IOUtils.readFully(istream, -1, true);

        Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aes.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedOutput = aes.doFinal(input);
        File temp = new File(basedir + in);
        temp.getParentFile().mkdirs();
        FileOutputStream stream = new FileOutputStream(temp);
        stream.write(decryptedOutput, 0, decryptedOutput.length);
        stream.close();

        return temp.getPath();
    }

    /**
     * Render a template with data.
     *
     * @param templateGroupName the name of the template group, i.e. its filename without '.stg'.
     * @param templateName the name of the template in the specified template group
     * @param attributes all the association {key, value} used by the template.
     * @return the rendered template as a buffer.
     * @throws IOException when the file doesn't exists or can't be created.
     */
    protected String renderTemplate(
            String templateGroupName, String templateName, Map<String, Object> attributes)
            throws IOException {
        STGroup group = loadTemplateGroup(templateGroupName);
        registerRenderers(group);

        // Clear any pending generation fault
        _generationErrors.clear();

        // compile the template (syntax errors will be raised here)
        ST template = group.getInstanceOf(templateName);

        for (Object err : _generationErrors) {
            error((STMessage) err);
        }

        if (template == null) {
            throw new IllegalArgumentException("No such template name=" + templateName + ",group=" + templateGroupName);
        }

        for (String attributeName : attributes.keySet()) {
            template.add(attributeName, attributes.get(attributeName));
        }

        Set<String> expectedKeys = template.getAttributes().keySet();
        Set<String> providedKeys = attributes.keySet();
        if (!providedKeys.equals(expectedKeys)) {
            boolean throwError = false;
            if (providedKeys.containsAll(expectedKeys)) {
                providedKeys.removeAll(expectedKeys);
                for (String key : providedKeys) {
                    throwError = true;
                    error(
                            "%s:%s:attribute '%s' not declared",
                            templateGroupName, templateName, key);
                }
            } else {
                expectedKeys.removeAll(providedKeys);
                for (String key : expectedKeys) {
                    if (template.impl.formalArguments.get(key).defaultValue == null) {
                        throwError = true;
                        error(
                                "%s:%s:attribute '%s' not valued",
                                templateGroupName, templateName, key);
                    }
                }
            }
            if (throwError) {
                throw new IllegalArgumentException(
                        "Template / values mismatch, see the log file for more.");
            }
        }

        if (debugTemplate != null && templateName.startsWith(debugTemplate)) template.inspect();

        // Generate content
        return template.render();
    }

    protected void registerRenderers(STGroup group) {
        group.registerRenderer(
                String.class,
                new StringRenderer() {
                    @Override
                    public String toString(Object o, String formatString, Locale locale) {
                        if (formatString != null && formatString.equals("dotToColons")) {
                            return ((String) o).replaceAll("\\.", "::");
                        } else {
                            return super.toString(o, formatString, locale);
                        }
                    }
                });
    }

    public void setSeparatorsAuto(String templateGroupName) {
        if (templateGroupName.endsWith(".dollar")) setSeparators('$', '$');
        else if (templateGroupName.endsWith(".hash")) setSeparators('#', '#');
        else setSeparators('<', '>');
    }

    /**
     * Creates a file from template and data.
     *
     * @param outputFile the target file to create.
     * @param templateGroupName the name of the template group, i.e. its filename without '.stg'.
     * @param templateName the name of the template in the specified template group
     * @param attributes all the association {key, value} used by the template.
     * @throws IOException when the file doesn't exists or can't be created.
     */
    @Requirement(ids = {"GenFramework-SRS-REQ-017", "GenFramework-SRS-REQ-020"})
    public void createFileFromTemplate(
            File outputFile,
            String templateGroupName,
            String templateName,
            Map<String, Object> attributes)
            throws IOException {

        String newContent = renderTemplate(templateGroupName, templateName, attributes);

        if (!_generationErrors.isEmpty()) {
            // Write errors list to log file
            for (Object err : _generationErrors) {
                if (err instanceof IOException) {
                    throw (IOException) err;
                } else if (err instanceof STMessage) {
                    STMessage m = (STMessage) err;
                    error("%s", m);
                } else {
                    error(
                            "Unexpected error encountered during %s generation",
                            outputFile.toURI().getPath());
                }
            }
            _nbGenerationErrors++;
            report(outputFile, ReportStatus.ERROR);
        } else {
            report(
                    outputFile,
                    Utilities.createFileFromStream(
                            outputFile, new ByteArrayInputStream(newContent.getBytes(CHARSET))));
        }
    }

    /**
     * Creates a file from template and data.
     *
     * @param target the target file to create.
     * @param templateGroupName the name of the template group, i.e. its filename without '.stg'.
     * @param templateName the name of the template in the specified template group
     * @param attributes all the association {key, value} used by the template.
     * @throws IOException when the file doesn't exists or can't be created.
     */
    @Requirement(ids = {"GenFramework-SRS-REQ-017", "GenFramework-SRS-REQ-020"})
    public void createFileFromTemplate(
            File target, String templateGroupName, String templateName, Object... attributes)
            throws IOException {
        assert (attributes.length % 2) == 0 : "Attributes must be defined as {key,value} pairs";
        _tmplAttrs.clear();
        for (int i = 0; i < attributes.length; i += 2) {
            _tmplAttrs.put((String) attributes[i], attributes[i + 1]);
        }
        createFileFromTemplate(target, templateGroupName, templateName, _tmplAttrs);
        _tmplAttrs.clear();
    }

    public void createFileFromStream(File target, InputStream stream) throws IOException {
        report(target, Utilities.createFileFromStream(target, stream));
    }

    /** Report an exception to the execution log. */
    @Requirement(ids = {"GenFramework-SRS-REQ-138", "GenFramework-SRS-REQ-139"})
    public void throwing(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            msg = t.toString();
        }
        _log.severe(msg);
    }

    private void error(STMessage msg) {
        String details = SINGLE_SPACE;
        if (msg.arg != null) details += msg.arg.toString() + SINGLE_SPACE;

        if (msg.arg2 != null) details += msg.arg2.toString() + SINGLE_SPACE;
        if (msg.arg3 != null) details += msg.arg3.toString() + SINGLE_SPACE;
        error("%s %s", msg.toString(), details.trim());
        if (msg.cause != null) {
            throwing(msg.cause);
        }
    }

    /** @return the generator specific Name. */
    public String getSpecificGeneratorName() {
        return _specificGeneratorName;
    }

    /** String Template Error Listener. */
    protected final STErrorListener _strngTmplErrLstnr;
    /** List of all the generation errors raised during this execution. */
    protected final List<Object> _generationErrors = new ArrayList<Object>();
    /** String Template begin token definition. Default : '<'. */
    protected char _separatorStart = '<';
    /** String template end token definition. Default : '>'. */
    protected char _separatorStop = '>';
    /** Debugging state. Default : false. */
    protected boolean _debug = false;
    /** template files root path. */
    protected String _templatesRoot;
    /** Number of errors raised during generation process */
    protected int _nbGenerationErrors = 0;

    /** Loaded templates cache */
    protected Map<String, STGroupFile> templatesCache = new HashMap<String, STGroupFile>();

    public static final String ARGUMENT_KEY_DEBUG = "debug";
    public static final char ARGUMENT_KEY_SHORT_DEBUG = 'g';
    public static final String ARGUMENT_KEY_HELP = "help";
    public static final char ARGUMENT_KEY_SHORT_HELP = 'h';
    protected static final String SINGLE_SPACE = " ";
}
