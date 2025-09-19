/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.thalesgroup.softarc.tools.AbstractLogger;
import com.thalesgroup.softarc.tools.InconsistentModelError;
import com.thalesgroup.softarc.tools.Utilities;

/**
 * This class models the standard file hierarchy of an ECOA workspace, as defined by the Standard (AS7).
 * <p>
 * Is is associated to a given root directory.
 * <p>
 * From logical names, it returns access to {@link java.io.File} objects corresponding to each kind of file or directory defined
 * in a SOFTARC workspace.
 * <p>
 * The standard file hierarchy can be customized with an optional configuration file "workspace.xml".
 */
public class WorkspaceStandard {

    public static final String EXTENSION_IMPL = ".impl.xml";
    public static final String EXTENSION_COMPONENT = ".comp.xml";
    public static final String EXTENSION_LIBRARY = ".types.xml";
    public static final String EXTENSION_ASSEMBLY = ".assembly.xml";
    public static final String EXTENSION_DEPLOYMENT = ".deployment.xml";
    public static final String WORKSPACE_FILENAME = "workspace.xml";
    public AbstractLogger logger;

    protected File root;
    private File dir00;
    private File dir01;
    private File dir02;
    private File dir03;
    protected File dir04;
    protected NameMaps nameMaps = new NameMaps();
    private boolean ignoreRelocations;

    protected class NameMaps {
        HashMap<String, File> libraries = new HashMap<>();
        HashMap<String, File> componenttypes = new HashMap<>();
    };

    public static boolean isPossibleRoot(File root) {
        if ((new File(root, WORKSPACE_FILENAME)).exists())
            return true;
        // Note:
        // 00- may not exist if there are no user-defined types
        // 01- may not exist because of user-level workspace configuration (workspace.xml)
        // 02- and 03- are supposed to exist in any case
        // It would be nice to a have a mandatory file that marks the root of a workspace (e.g. .ECOA_workspace).
        return (new File(root, "02-Assemblies")).exists() && (new File(root, "03-Deployments")).exists();
    }

    public WorkspaceStandard() {
    }

    public WorkspaceStandard(File root) throws IOException {
        setRoot(root);
    }

    /**
     * @param root
     *            the root directory of the workspace
     * @throws IOException 
     *             when the workspace.xml file is incorrect (including undefined env variables)
     */
    public void setRoot(File root) throws IOException {
        if (logger != null)
            logger.debug("Workspace root is '%s'", root.getPath());
        this.root = root.getAbsoluteFile();
        dir00 = new File(root, "00-Types");
        dir01 = new File(root, "01-Components");
        dir02 = new File(root, "02-Assemblies");
        dir03 = new File(root, "03-Deployments");
        dir04 = new File(root, "04-Integration");
        readWorkspaceConfigurationFile();
    }

    public File getRootDir() {
        return root;
    }

    public String getProjectRoot() {
        return root.getPath();
    }

    public File getAssemblyFile(String logicalName) {
        /*
         * If the name is qualified (C.I), the assembly is the internal assembly of implementation I of component C 
         * (which must be a composite).
         */
        String[] a = logicalName.split("\\.");
        if (a.length == 2) {
            return getInternalAssembly(a[0], a[1]);
        }
        return new File(getAssembliesDir(), logicalName + EXTENSION_ASSEMBLY);
    }

    public File getDeploymentFile(String deploymentName) {
        return new File(getDeploymentsDir(), deploymentName + EXTENSION_DEPLOYMENT);
    }

    public File getTypesDir() {
        return dir00;
    }

    public File getComponentsDir() {
        return dir01;
    }

    public File getAssembliesDir() {
        return dir02;
    }

    public File getDeploymentsDir() {
        return dir03;
    }

    public File getComponentType(String typeName, boolean isLibrary) {
        if (isLibrary)
            return getLibrary(typeName);
        else
            return new File(getComponentTypeDir(typeName), typeName + EXTENSION_COMPONENT);
    }

    public File getComponentTypeDir(String typeName) {
        File dir = nameMaps.componenttypes.get(typeName);
        if (dir == null) {
            dir = new File(getComponentsDir(), typeName);
        }
        return dir;
    }

    public File getLibrary(String name) {
        File dir = nameMaps.libraries.get(name);
        if (dir == null) {
            dir = new File(getTypesDir(), name + EXTENSION_LIBRARY);
        }
        return dir;
    }

    public File getComponentImpl(String typeName, String implName) {
        return new File(getComponentImplDir(typeName, implName), typeName + "." + implName + EXTENSION_IMPL);
    }

    public File getComponentImplDir(String typeName, String implName) {
        return new File(getComponentTypeDir(typeName), implName);
    }

    public File getLibraryDir(String typeName, String language) {
        return new File(new File(getLibrary(typeName).getParentFile(), typeName), language);
    }

    public File getInternalAssembly(String typeName, String implName) {
        return new File(getComponentImplDir(typeName, implName), typeName + "." + implName + EXTENSION_ASSEMBLY);
    }

    /**
     * Warning: returns only components, not type libraries!
     */
    public Collection<String> findAllComponentTypes() {
        HashMap<String, File> map = new HashMap<>();
        try {
            for (File d : dir01.listFiles())
                if (d.isDirectory()) {
                    map.put(d.getName(), d);
                }
        } catch (Exception e) {
        }
        map.putAll(nameMaps.componenttypes);
        return map.keySet();
    }

    public Collection<String> findAllLibraries() {
        HashMap<String, File> map = new HashMap<>();
        try {
            for (File d : dir00.listFiles()) {
                String name = d.getName();
                if (d.isFile() && name.endsWith(EXTENSION_LIBRARY)) {
                    map.put(name.substring(0, name.length() - EXTENSION_LIBRARY.length()), d);
                }
            }
        }
        catch (Exception e) {
        }
        map.putAll(nameMaps.libraries);
        return new ArrayList<>(map.keySet());
    }

    public Collection<String> findAllComponentTypesAndLibraries() {
        Collection<String> result = findAllLibraries();
        result.addAll(findAllComponentTypes());
        return result;
    }

    public Collection<String> listImplementations(String typeName) {
        ArrayList<String> result = new ArrayList<>();
        File[] listFiles = getComponentTypeDir(typeName).listFiles();
        if (listFiles != null) {
            for (File dir : listFiles) {
                if (dir.isDirectory()) {
                    String implName = dir.getName();
                    result.add(implName);
                }
            }
        }
        return result;
    }

    private void readWorkspaceConfigurationFile() throws IOException {
        nameMaps.componenttypes.clear();
        nameMaps.libraries.clear();
        File file = new File(root, WORKSPACE_FILENAME);
        try {
            // this file is optional
            if (file.exists()) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                saxParser.parse(file, new WorkspaceFileHandler(file));
            }
        } catch (Exception e) {
            throw new IOException(String.format("In %s: %s", file, e.getMessage()));
        }
    }

    class WorkspaceFileHandler extends DefaultHandler {
        File wsfile;

        WorkspaceFileHandler(File wsfile) {
            this.wsfile = wsfile;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            try {
                String name = attributes.getValue("name");
                if (qName.equals("library")) {
                    processWorkspaceDefinition(nameMaps.libraries, getLibrary(name), name,
                            attributes.getValue("file"));
                } else if (qName.equals("componentType") || qName.equals("componenttype")) {
                    processWorkspaceDefinition(nameMaps.componenttypes, getComponentType(name, false), name,
                            attributes.getValue("dir"));
                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }

        private void processWorkspaceDefinition(HashMap<String, File> resultMap, File defaultFile, String name,
                String redefinedPathName) throws IOException, InconsistentModelError {

            if (name == null)
                return;
            if (redefinedPathName == null)
                throw new InconsistentModelError(String.format("in '%s': Invalid redefinition for '%s'", wsfile.getPath(), name));

            File redefinedFile = expandAndFormatPath(redefinedPathName);
            if (ignoreRelocations)
                redefinedFile = defaultFile;
            File oldFile = resultMap.put(name, redefinedFile);
            if (oldFile != null) {
                throw new InconsistentModelError(
                        String.format("in '%s': Duplicate definition for '%s'", wsfile.getPath(), name));
            }

            if (logger != null && defaultFile.exists() && !defaultFile.equals(redefinedFile)) {
                logger.warning("in '%s': file exists at default location '%s', but is redefined as '%s'", wsfile.getPath(), defaultFile,
                        redefinedFile);
            }
        }
    }

    private File expandAndFormatPath(String path) throws IOException {
        path = Utilities.formatPath(Utilities.expandPath(path, getProjectRoot(), true));
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(root, path);
        }
        file = file.getCanonicalFile();
        return file;
    }

    public Collection<String> findAllDeployments() {
        ArrayList<String> result = new ArrayList<>();
        try {
            for (File d : dir03.listFiles(deploymentFileFilter))
                if (d.isFile()) {
                    result.add(d.getName().replace(EXTENSION_DEPLOYMENT, ""));
                }
        }
        catch (Exception e) {
        }
        return result;
    }

    public Collection<String> findAllAssemblies() {
        ArrayList<String> result = new ArrayList<>();
        try {
            for (File d : dir02.listFiles(assemblyFileFilter))
                if (d.isFile()) {
                    result.add(d.getName().replace(EXTENSION_ASSEMBLY, ""));
                }
        }
        catch (Exception e) {
        }
        return result;
    }

    public Collection<String> findAllComponentImplementations(String typeName) {
        return listImplementations(typeName);
    }

    public void createWorkspaceStructure() {
        root.mkdir();
        dir00.mkdir();
        dir01.mkdir();
        dir02.mkdir();
        dir03.mkdir();
        dir04.mkdir();
    }

    static final FilenameFilter assemblyFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File string, String name) {
            return name.endsWith(EXTENSION_ASSEMBLY);
        }
    };

    static final FilenameFilter deploymentFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File string, String name) {
            return name.endsWith(EXTENSION_DEPLOYMENT);
        }
    };
    
    public void setIgnoreRelocations(boolean value) {
        ignoreRelocations = value;
    }
    
}