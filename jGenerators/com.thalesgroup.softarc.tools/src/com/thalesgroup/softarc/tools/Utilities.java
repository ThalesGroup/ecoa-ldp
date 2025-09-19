/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various utility functions, mainly dealing with files.
 */
public final class Utilities {

    /**
     * Does a recursive search starting in the root directory, for all files which match the given regular expression.
     * 
     * @param regex Regular expression to match
     * @param rootNode Root directory to search in
     * @param files Set which receives the files found
     */
    public static void findFiles(String regex, File rootNode, Collection<File> files) throws IOException {
        if (rootNode.exists()) {
            for (File file : rootNode.listFiles()) {
                if (file.isHidden()) {
                    continue;
                } else {
                    String filename = file.getName();
                    if (filename.matches(regex)) {
                        files.add(file);
                    }
                    if (file.isDirectory()) {
                        findFiles(regex, file, files);
                    }
                }
            }
        }
    }

    /**
     * Writes the content of the stream to the target file. Input stream position if left at the very end of the stream once
     * method returns but stream is not closed.
     * 
     * <p>
     * If the file already exists, the new contents are compared against the existing contents, and if identical, the file is not
     * touched and IDENTICAL is returned.
     * 
     * @param target
     *            The file to write to.
     * @param stream
     *            The stream to read from.
     * @return
     * @throws IOException
     */
    public static ReportStatus createFileFromStream(File target, InputStream stream) throws IOException {

        ReportStatus rc = ReportStatus.CREATED;
        boolean generate = true;
        InputStream inputstream = stream;

        if (target.exists()) {

            rc = ReportStatus.UPDATED;

            // Create a seekable stream in this the given one doesn't support reset()
            if (!stream.markSupported()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int cin;
                while ((cin = stream.read()) != -1) {
                    baos.write(cin);
                }
                baos.close();
                inputstream = new ByteArrayInputStream(baos.toByteArray());
            }
            inputstream.mark(Integer.MAX_VALUE);

            // Compare the existing file with the content of the stream to create file from
            FileInputStream in = new FileInputStream(target);
            BufferedInputStream bout = new BufferedInputStream(in);
            boolean keepGoing = true;

            while (keepGoing) {
                int cin = inputstream.read();
                int cout = bout.read();
                // A character differs => update forced!
                if (cin != cout) {
                    keepGoing = false;
                }
                // All characters are equal so far, and we reached the end
                // of both streams => no need to overwrite
                else if (cin == -1) {
                    keepGoing = false;
                    generate = false;
                }
            }
            in.close();
            bout.close();
            inputstream.reset();
        }

        if (generate) {

            if (target.getParentFile() != null)
                target.getParentFile().mkdirs();

            FileOutputStream out = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            // copy the whole file
            while (true) {
                int i = inputstream.read(buffer);
                if (i == -1)
                    break;
                out.write(buffer, 0, i);
            }
            out.close();

        } else {
            rc = ReportStatus.IDENTICAL;
        }

        // Close input stream in case it is an instance that we've created locally
        if (stream != inputstream) {
            inputstream.close();
        }

        return rc;
    }

    public static String readStreamAsString(InputStream f) throws IOException {
        int c;
        InputStreamReader reader = new InputStreamReader(f);
        StringBuilder sb = new StringBuilder(4096);
        while ((c = reader.read()) != -1)
            sb.append((char) c);
        reader.close();
        return sb.toString();
    }

    public static String readFileAsString(File f) throws IOException {
        FileInputStream stream = new FileInputStream(f);
        String result = readStreamAsString(stream);
        stream.close();
        return result;
    }

    public static void writeFileAsString(String s, File f) throws IOException {
        FileWriter w = new FileWriter(f);
        w.write(s);
        w.close();
    }

    /**
     * Call expandPath(path, projectRoot, false).
     */
    public static String expandPath(String path, String projectRoot) {
        try {
            return expandPath(path, projectRoot, false);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reformat all path names with '/' instead of '\', for use by 'make' and 'gprbuild'.
     */
    public static String formatPath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Replaces environment variables with their value in a path. The syntax for a variable is either $(VARIABLE) or ${VARIABLE}.
     * The special variable 'PROJECT_ROOT' is replaced with the value of 'projectRoot', regardless of the environment.
     * 
     * @param path
     *            string in which variables will be replaced
     * @param projectRoot
     *            value of special variable 'PROJECT_ROOT'
     * @param failIfUndefined
     *            if true, throws a IOException when a variable is not defined; if false, no replacement is done
     * @return expanded path
     * @throws IOException
     *             when failIfUndefined=true and a variable is not defined
     */
    public static String expandPath(String path, String projectRoot, boolean failIfUndefined) throws IOException {
        if (compiled_VARIABLE_PATTERN == null) {
            compiled_VARIABLE_PATTERN = Pattern.compile(VARIABLE_PATTERN);
        }
        Matcher matcher = compiled_VARIABLE_PATTERN.matcher(path);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String env = System.getenv().get(variableName);

            /* There is no actual "PROJECT_ROOT" environment variable, as it is defined in the makefiles. */
            if (variableName.equals("PROJECT_ROOT")) {
                env = projectRoot;
            }

            if (env != null) {
                env = env.replace("\\", "\\\\");
                Pattern subexpr = Pattern.compile(Pattern.quote(matcher.group(0)));
                path = subexpr.matcher(path).replaceAll(env);
            } else {
                if (failIfUndefined) {
                    throw new IOException("Environment variable not defined: " + variableName);
                }
            }
        }
        return path;
    }

    static private final String VARIABLE_PATTERN = "\\$[\\(|{]([A-Za-z0-9_]+)[\\)|}]";
    static private Pattern compiled_VARIABLE_PATTERN;
}
