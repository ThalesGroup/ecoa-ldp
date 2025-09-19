/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a cache of models, each model being associated to a file; to avoid reloading several times the same file.
 *
 * One model = one file.
 * 
 * @param <E> class corresponding to the information parsed from the file (e.g. JAXB-generated class,...) 
 */
public class LoadedFileList<E extends Object> {

    /**
     * Represents a model already loaded in memory
     */
    class LoadedModel {
        E model;
        long timestamp;
        long length;
        byte[] bytes;
        boolean hasBeenLoaded;

        LoadedModel(File file) {
            timestamp = file.lastModified();
            length = file.length();
        }

        private boolean isUpToDate(File file, byte[] data) {
            return model != null && bytes != null && timestamp == file.lastModified() && Arrays.equals(bytes, data);
        }
    }

    /**
     * Returns a LoadedModel containing the bytes of the file (always) and the parsed model, if available
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public LoadedModel get(File file) throws IOException, FileNotFoundException {
        final String path = file.getCanonicalPath();
        LoadedModel m = _loadedFiles.get(path);

        // load bytes
        byte[] data = new byte[(int) file.length()];
        FileInputStream s = new FileInputStream(file);
        s.read(data);
        s.close();

        if (m == null) {
            m = new LoadedModel(file);
            m.bytes = data;
            m.hasBeenLoaded = true;
            _loadedFiles.put(path, m);
        } else
        // if model in cache is not up-to-date, forget it, and keep the new bytes in memory
        if (!m.isUpToDate(file, data)) {
            m.model = null;
            m.bytes = data;
        }
        return m;
    }

    public void create(File file, byte[] data, E model) throws IOException {
        final String path = file.getCanonicalPath();
        LoadedModel m = _loadedFiles.get(path);

        if (m != null) {
            if (m.hasBeenLoaded)
                throw new IOException("You cannot overwrite input model file: '" + file.toURI().getPath() + "'.\n"
                        + "Please specify another output.");
        }

        FileOutputStream fs = new FileOutputStream(file);
        fs.write(data);
        fs.close();

        m = new LoadedModel(file);
        m.bytes = data;
        m.model = model;
        _loadedFiles.put(path, m);
    }

    /**
     * Set of canonical file paths
     */
    private final Map<String, LoadedModel> _loadedFiles = new HashMap<String, LoadedModel>();

    private static ArrayList<LoadedFileList<?>> instances = new ArrayList<LoadedFileList<?>>();

    public LoadedFileList() {
        instances.add(this);
    }

    public static void clear() {
        for (LoadedFileList<?> l : instances) {
            l._loadedFiles.clear();
        }
    }

}
