/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s10.importcomponents;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import com.thalesgroup.ecoa.model.ComponentType;
import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.ecoa.model.Library;
import com.thalesgroup.ecoa.model.Models;
import com.thalesgroup.ecoa.model.TypesContainer;
import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.StringJoiner;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguageHandler;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguagesHandler;

import technology.ecoa.model.datatype.CTArray;
import technology.ecoa.model.componenttype.CTComponentType;
import technology.ecoa.model.datatype.CTConstant;
import technology.ecoa.model.datatype.CTEnum;
import technology.ecoa.model.datatype.CTFixedArray;
import technology.ecoa.model.componenttype.CTPinfo;
import technology.ecoa.model.datatype.CTLibrary;
import technology.ecoa.model.datatype.CTList;
import technology.ecoa.model.datatype.CTMap;
import technology.ecoa.model.componenttype.CTOperation;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.QualifiedField;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.datatype.CTRecord;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.datatype.CTSimple;
import technology.ecoa.model.datatype.CTString;
import technology.ecoa.model.componenttype.CTTrigger;
import technology.ecoa.model.datatype.CTType;
import technology.ecoa.model.datatype.CTUnionField;
import technology.ecoa.model.datatype.CTValue;
import technology.ecoa.model.datatype.CTVariantRecord;
import technology.ecoa.model.datatype.MetaData;
import technology.ecoa.model.componenttype.CTWrittenData;
import technology.ecoa.model.componenttype.EComponentKind;
import technology.ecoa.model.implementation.CTExtra;
import technology.ecoa.model.implementation.CTImplementation;
import technology.ecoa.model.implementation.CTLanguage;
import technology.ecoa.model.implementation.CTLanguageada;
import technology.ecoa.model.implementation.CTLanguageada.GprPackage;
import technology.ecoa.model.implementation.CTLanguageada.WithGpr;
import technology.ecoa.model.implementation.CTLanguagerust;
import technology.ecoa.model.implementation.CTLanguagerust.Dependency;
import technology.ecoa.model.implementation.CTOption;
import technology.ecoa.model.implementation.CTTopic;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.Extra;
import com.thalesgroup.softarc.sf.PInfo;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.Topic;
import com.thalesgroup.softarc.sf.Trigger;
import com.thalesgroup.softarc.sf.impl.QAssembly;
import com.thalesgroup.softarc.sf.impl.QComponent;
import com.thalesgroup.softarc.sf.impl.QConstantDefinition;
import com.thalesgroup.softarc.sf.impl.QEnumValue;
import com.thalesgroup.softarc.sf.impl.QExtra;
import com.thalesgroup.softarc.sf.impl.QPInfo;
import com.thalesgroup.softarc.sf.impl.QOperationData;
import com.thalesgroup.softarc.sf.impl.QOperationEvent;
import com.thalesgroup.softarc.sf.impl.QOperationRequestResponse;
import com.thalesgroup.softarc.sf.impl.QParameter;
import com.thalesgroup.softarc.sf.impl.QRename;
import com.thalesgroup.softarc.sf.impl.QRustDependency;
import com.thalesgroup.softarc.sf.impl.QSystem;
import com.thalesgroup.softarc.sf.impl.QTopic;
import com.thalesgroup.softarc.sf.impl.QTrigger;
import com.thalesgroup.softarc.sf.impl.QTypeDefinition;
import com.thalesgroup.softarc.sf.impl.QVariantField;
import com.thalesgroup.softarc.tools.Utilities;

public class ImportComponent extends AbstractPass {

    public static final String PASS_ARG_KEY_COMPONENT_NAME_PREFIX = "componentNamePrefix";
    public static final String PASS_ARG_KEY_FORCE_IMPLEMENTATION_LANGUAGE = "forceImplementationLanguage";
    public static final String PASS_ARG_KEY_FORCE_PACKAGE = "forcePackage";

    private String ctName;
    private String implName;

    public void setComponentType(String name) {
        ctName = name;
    }

    public void setImplementation(String name) {
        implName = name;
    }

    @Override
    public void execute() throws IOException {

        QSystem sys = new QSystem();
        context.system = sys;

        if (context.workspace.getComponentType(ctName, false).exists()) {
            importComponent(ctName, implName);
        } else {
            importLibrary(ctName, implName);
        }

        Assembly assembly = new QAssembly();
        sys.setAssembly(assembly);

        addStubImplementations();
    }

    void importTypes(List<CTType> types, Component component) {
        for (CTType t : types) {
            if (t instanceof CTConstant) {
                QConstantDefinition td = new QConstantDefinition();
                td.setName(t.getName());
                td.setTypeName(((CTConstant) t).getType());
                td.setValueOrConstant(((CTConstant) t).getValue());
                importDoc(t.getDoc(), td.getDoc());
                component.getConstants().add(td);
            } else {
                QTypeDefinition td = new QTypeDefinition();
                td.setName(t.getName());
                // TR-SARC-GEN-REQ-138
                String qName = component.getTypeName() + "." + td.getName();
                td.setTypeName(qName);
                td.setXmlID(qName + "." + component.getImplName());

                importDoc(t.getDoc(), td.getDoc());
                if (t instanceof CTSimple) {
                    td.setIsSimple(true);
                    td.setUnit(((CTSimple) t).getUnit());
                    td.setBaseTypeName(((CTSimple) t).getType());
                    td.setMinRange(toStringOrEmpty(((CTSimple) t).getMinRange()));
                    td.setMaxRange(toStringOrEmpty(((CTSimple) t).getMaxRange()));
                } else if (t instanceof CTEnum) {
                    td.setIsEnum(true);
                    td.setBaseTypeName(((CTEnum) t).getType());
                    for (CTValue v : ((CTEnum) t).getValue()) {
                        QEnumValue z = new QEnumValue();
                        z.setName(v.getName());
                        if (v.getValNum() != null) {
                            z.setValnumOrConstant(v.getValNum());
                            z.setIsUserDefined(true);
                        }
                        importDoc(v.getDoc(), z.getDoc());
                        td.getEnumValues().add(z);
                    }
                } else if (t instanceof CTVariantRecord) {
                    td.setIsVariantRecord(true);
                    for (technology.ecoa.model.datatype.QualifiedField f : ((CTVariantRecord) t).getField()) {
                        QParameter p = new QParameter();
                        p.setName(f.getName());
                        p.setTypeName(f.getType());
                        importDoc(f.getDoc(), p.getDoc());
                        td.getFields().add(p);
                    }
                    td.setSelectName(((CTVariantRecord) t).getSelectName());
                    td.setBaseTypeName(((CTVariantRecord) t).getSelectType());
                    technology.ecoa.model.datatype.QualifiedField def = ((CTVariantRecord) t).getDefault();
                    if (def != null) {
                        QParameter p = new QParameter();
                        p.setName(def.getName());
                        p.setTypeName(def.getType());
                        importDoc(def.getDoc(), p.getDoc());
                        td.setDefaultUnionField(p);
                    }
                    for (CTUnionField f : ((CTVariantRecord) t).getUnion()) {
                        QVariantField p = new QVariantField();
                        p.setName(f.getName());
                        p.setTypeName(f.getType());
                        p.setWhen(f.getWhen());
                        importDoc(f.getDoc(), p.getDoc());
                        td.getUnionFields().add(p);
                    }
                } else if (t instanceof CTRecord) {
                    td.setIsRecord(true);
                    for (technology.ecoa.model.datatype.QualifiedField f : ((CTRecord) t).getField()) {
                        QParameter p = new QParameter();
                        p.setName(f.getName());
                        p.setTypeName(f.getType());
                        importDoc(f.getDoc(), p.getDoc());
                        td.getFields().add(p);
                    }
                } else if (t instanceof CTFixedArray) {
                    td.setIsFixedArray(true);
                    td.setBaseTypeName(((CTFixedArray) t).getType());
                    td.setArraySizeOrConstant((((CTFixedArray) t).getMaxNumber()));
                } else if (t instanceof CTArray) {
                    td.setIsArray(true);
                    td.setBaseTypeName(((CTArray) t).getType());
                    td.setArraySizeOrConstant((((CTArray) t).getMaxNumber()));
                } else if (t instanceof CTString) {
                    if (component.getIsEcoa()) {
                        errorModel(
                                "String type is not supported by ECOA; fix '%s' type for '%s'.",
                                t.getName(), component);
                    }
                    td.setIsString(true);
                    td.setLength(((CTString) t).getLength());
                    component.setHasStrings(true);
                } else if (t instanceof CTList) {
                    warning(
                            component.getTypeName()
                                    + "."
                                    + t.getName()
                                    + ": 'list' types are now obsolete, and support will be removed in a future version");
                    td.setIsList(true);
                    td.setBaseTypeName(((CTList) t).getType());
                    td.setArraySizeOrConstant((((CTList) t).getMaxnumber()));
                } else if (t instanceof CTMap) {
                    warning(
                            component.getTypeName()
                                    + "."
                                    + t.getName()
                                    + ": 'map' types are now obsolete, and supoprt will be removed in a future version");
                    td.setIsMap(true);
                    td.setBaseTypeName(((CTMap) t).getType());
                    td.setKeyTypeName(((CTMap) t).getKeytype());
                    td.setArraySizeOrConstant((((CTMap) t).getMaxnumber()));
                }
                td.setType(td);
                component.getTypes().add(td);
            }
        }
    }

    private String toStringOrEmpty(Object o) {
        if (o == null) return "";
        return o.toString();
    }

    protected void importUsedLibraries(Component component, TypesContainer type) throws IOException {
        for (String usedLib : type.getUsedLibraries()) {
            if (importedNow.add(usedLib) == false) {
                errorModel("Circular dependency detected between libraries %s and %s", component.getFullName(), usedLib);
            }
            Component lib = importLibrary(usedLib, component.getApiVariant());
            component.getUsedLibraries().add(lib);
            importedNow.remove(usedLib);
        }
    }

    public void convertGenLibPackage(String typeName, Component component, Language language) {
        String[] split = patternUnderscore.split(typeName);
        int iter;
        for (iter = 0; iter < split.length; iter++) {
            split[iter] = LanguagesHandler.getSoftarc(language.name()).avoidKeywords(split[iter].toLowerCase());
        }
        component.setPackage(StringJoiner.stringJoin(split, "."));
    }

    public void computeGenLibPackage(Component component) {
        String[] split = patternDot.split(component.getPackage());
        component.setJniPackage(StringJoiner.stringJoin(split, "/"));
        component.getSplittedPackage().clear();
        Collections.addAll(component.getSplittedPackage(), split);
    }

    private void checkComponentConstraints(Component component) throws IOException {
        // A library shall be in the same language that the component/library that uses it.
        for (Component lib : component.getUsedLibraries()) {
            if (!lib.getLanguage().equals(component.getLanguage()))
                errorModel("%s implementation %s/%s (in language %s) cannot use library implementation %s/%s in language %s",
                        component.getIsLibrary() ? "Library" : "Component", component.getTypeName(), component.getImplName(),
                        component.getLanguage(), lib.getTypeName(), lib.getImplName(), lib.getLanguage());
        }
    }

    private void importExtra(List<CTExtra> in, Collection<Extra> out) {
        for (CTExtra x : in) {
            QExtra y = new QExtra();
            y.setProduction(x.getProduction());
            y.setValue(x.getValue());
            out.add(y);
        }
    }

    private String findMetaData(String key, List<MetaData> meta, String defaultValue) {
        for (MetaData m : meta)
            if (key.equals(m.getName())) {
                info("Metadata %s='%s' overrides default value '%s'", key, m.getValue(), defaultValue);
                return m.getValue();
            }
        return defaultValue;
    }

    private void addExtra(Collection<Extra> collection, String string) {
        QExtra y = new QExtra();
        y.setValue(string);
        collection.add(y);
    }
    
    Component importLibrary(String typeName, String apiVariant) throws IOException {
        OldOrNewComponent on = addComponentIfAbsent("library", typeName, apiVariant);
        Component component = on.component;
        if (on.isNew) {
            CTLibrary ct = Models._library.load(context.workspace.getLibrary(typeName), gen);

            component.setIsLibrary(true);
            setApiVariant(apiVariant, component);

            String language = LanguagesHandler.get(apiVariant).language.name();
            String languagePrefix = language + "_";
            component.setLanguage(language);
        	
            if (context.args.containsKey(PASS_ARG_KEY_COMPONENT_NAME_PREFIX)) {
            	String fullname =
                        (String) context.args.get(PASS_ARG_KEY_COMPONENT_NAME_PREFIX)
                                + "_"
                                + typeName;
                component.setPackage(fullname);
                component.setFileprefix(fullname);
            }
            else {
                component.setPackage(findMetaData(apiVariant + "_package", ct.getMeta(), findMetaData(languagePrefix + "package", ct.getMeta(), typeName)));
                component.setFileprefix(findMetaData(apiVariant + "_fileprefix", ct.getMeta(), findMetaData(languagePrefix + "fileprefix", ct.getMeta(), component.getPackage())));
            }
            component.setImplDir(context.workspace.getLibraryDir(typeName, apiVariant).getPath());
            importDoc(ct.getDoc(), component.getDoc());
            importTypes(ct.getLibraryTypes(), component);
            
            computeNamesForComponentOrLibrary(component);
            importUsedLibraries(component, new Library(typeName, ct));
            for (MetaData m : ct.getMeta())
            {
                if (m.getName().startsWith(languagePrefix)) {
                    switch (m.getName().substring(languagePrefix.length())) {
                    case "srcDir":
                        addExtra(component.getSrcdir(), m.getValue());
                        break;
                    case "incDir":
                        addExtra(component.getIncdir(), m.getValue());
                        break;
                    case "compilationFlags":
                        addExtra(component.getCompilationFlags(), m.getValue());
                        break;
                    case "linkFlags":
                        addExtra(component.getCompilationFlags(), m.getValue());
                        break;
                    case "usedLibrary":
                        Component lib = importLibrary(m.getValue(), apiVariant);
                        component.getUsedLibraries().add(lib);
                        break;
                    }
                } else if (m.getName().equals("usedLibrary")) {
                    Component lib = importLibrary(m.getValue(), apiVariant);
                    component.getUsedLibraries().add(lib);
                }
            }
        }
        return component;
    }

    Component importComponent(String typeName, String implName) throws IOException {

        OldOrNewComponent on = addComponentIfAbsent("component", typeName, implName);
        Component component = on.component;
        if (on.isNew) {
            CTComponentType ct = Models._componenttype.load(context.workspace.getComponentType(typeName, false), gen);

            File implFile = context.workspace.getComponentImpl(typeName, implName);
            CTImplementation ci = Models._implementation.load(implFile, gen);
            CTLanguage genericLanguage = null;

            if (ci.getLanguageC() != null) {
                genericLanguage = ci.getLanguageC();

                component.setPackage(ci.getLanguageC().getFullName());
                component.setLanguage(Language.C.name());
                if (ci.getLanguageC().getFilePrefix() != null) {
                    component.setFileprefix(ci.getLanguageC().getFilePrefix());
                }
            }
            if (ci.getLanguageCpp() != null) {
                genericLanguage = ci.getLanguageCpp();
                component.setPackage(ci.getLanguageCpp().getNamespace());
                if (ci.getLanguageCpp().getFilePrefix() != null) {
                    component.setFileprefix(ci.getLanguageCpp().getFilePrefix());
                }
                component.setLanguage(Language.CPP.name());
            }
            if (ci.getLanguageAda() != null) {
                genericLanguage = ci.getLanguageAda();
                CTLanguageada ada = ci.getLanguageAda();
                component.setPackage(ada.getPackageName());
                component.setLanguage(Language.ADA.name());
                if (ada.getExtendsGpr() != null) {
                    component.getExtendsGpr().add(ada.getExtendsGpr().getName());
                }
                if (ada.getWithGpr() != null) {
                    for (WithGpr w : ada.getWithGpr()) component.getWithGpr().add(w.getName());
                }
                for (GprPackage p : ada.getGprPackage()) {
                    QRename out = new QRename();
                    out.setNewName(p.getName());
                    out.setOldName(p.getRenames());
                    component.getGprPackage().add(out);
                }
            }
            if (ci.getLanguageJava() != null) {
                genericLanguage = ci.getLanguageJava();
                component.setPackage(ci.getLanguageJava().getPackageName());
                component.setLanguage(Language.JAVA.name());
            }
            if (ci.getLanguagePython() != null) {
                genericLanguage = ci.getLanguagePython();
                component.setPackage(ci.getLanguagePython().getPackageName());
                component.setLanguage(Language.PYTHON.name());
            }
            if (ci.getLanguageRust() != null) {
                genericLanguage = ci.getLanguageRust();
                CTLanguagerust rust = ci.getLanguageRust();
                component.setPackage(rust.getPackageName());
                component.setLanguage(Language.RUST.name());

                for (Dependency d : rust.getDependency()) {
                	String path = Utilities.formatPath(Utilities.expandPath(d.getPath(), context.workspace.getProjectRoot(), true));
                	QRustDependency dep = new QRustDependency();
                	dep.setName(d.getName());
                	dep.setPath(path);
                	component.getRustDependencies().add(dep);
                }
            }
            if (genericLanguage != null) {
                setApiVariant(genericLanguage.getAPIType(), component);
                importExtra(genericLanguage.getAdditionalJar(), component.getAdditionalJar());	
                importExtra(genericLanguage.getCompilationFlags(), component.getCompilationFlags());
                importExtra(genericLanguage.getLinkFlags(), component.getLinkFlags());
                importExtra(genericLanguage.getIncDir(), component.getIncdir());
                importExtra(genericLanguage.getSrcDir(), component.getSrcdir());
                component.setStack(genericLanguage.getStack());
                component.setExternalStack(genericLanguage.getExternalStack());
            }

            if (context.args.containsKey(PASS_ARG_KEY_FORCE_IMPLEMENTATION_LANGUAGE)) {

                Language originalLanguage = Language.valueOf(component.getLanguage());
                Language forcedLanguage =
                        (Language) context.args.get(PASS_ARG_KEY_FORCE_IMPLEMENTATION_LANGUAGE);
                component.setLanguage(forcedLanguage.name());
                assert component.getIsEcoa() == false;

                if (forcedLanguage.equals(Language.C)) {
                    component.setIsCComponent(true);
                } else if (forcedLanguage.equals(Language.JAVA)) {
                    if (!context.args.containsKey(PASS_ARG_KEY_FORCE_PACKAGE)) {
                        throw new Error("Internal error");
                    }
                    component.setIsJavaComponent(true);
                    component.setImplName("JAVA_lib");
                    if (forcedLanguage != originalLanguage) {
                        convertGenLibPackage(typeName, component, forcedLanguage);
                    }
                    component.setPackage(
                            context.args.get(PASS_ARG_KEY_FORCE_PACKAGE)
                                    + "."
                                    + component.getPackage());
                    computeGenLibPackage(component);
                }

                if (context.args.containsKey(PASS_ARG_KEY_COMPONENT_NAME_PREFIX)
                        && forcedLanguage != Language.JAVA) {
                    String fullname =
                            (String) context.args.get(PASS_ARG_KEY_COMPONENT_NAME_PREFIX)
                                    + "_"
                                    + typeName;
                    component.setPackage(fullname);
                    component.setFileprefix(fullname);

                    component.setImplName(component.getTypeName());
                }

                component.setImplDir(
                        context.workspace
                                .getComponentImplDir(typeName, component.getImplName())
                                .getPath());
            }
            checkComponentConstraints(component);

            if (ci.getTopic() != null) {
                for (CTTopic t : ci.getTopic()) {
                    Topic topic = new QTopic();
                    topic.setName(t.getName());
                    component.getTopics().add(topic);
                }
            }
            for (CTOption o : ci.getOption()) {
            	if(o.isValue()) {
            		switch (o.getName()) {
                        case "autostartExternalThread":
                            component.setAutoStartExternalThread(true);
                            break;
                        case "hasReset":
                            component.setHasReset(true);
                            break;
                        case "hasWarmStartContext":
                            component.setHasWarmStartContext(true);
                            break;
                        case "needsUTCTime":
                            component.setNeedsUTCTime(true);
                            break;
                    }
            	}
            }

            for (technology.ecoa.model.implementation.MetaData m : ci.getMeta()) {
                if (m.getName().equals("usedLibrary")) {
                    Component lib = importLibrary(m.getValue(), component.getApiVariant());
                    component.getUsedLibraries().add(lib);
                }
            }
            LanguageHandler lh = LanguagesHandler.getSoftarc(component.getLanguage());
            component.setHeaderextension(lh.headerextension);
            component.getSourceextension().addAll(Arrays.asList(lh.sourceextension));
            // if (genericLanguage != null) {
            // // check if fileprefix, headerextension, sourceexetension attributes explicitely are defined by user
            // if (genericLanguage.getHeaderextension() != null) {
            // component.setHeaderextension(genericLanguage.getHeaderextension());
            // }
            // if (genericLanguage.getSourceextension() != null && !genericLanguage.getSourceextension().isEmpty()) {
            // component.getSourceextension().clear();
            // component.getSourceextension().addAll(Arrays.asList(genericLanguage.getSourceextension().split(",")));
            // }
            // }
            // fileprefix is set to package, unless language-specific behaviour
            component.setIsExternal(ct.getKind() == EComponentKind.EXTERNAL);
            component.setIsSupervisor(ct.getKind() == EComponentKind.SUPERVISOR);
            component.setIsTimer(ct.getKind() == EComponentKind.PERIODIC_TRIGGER_MANAGER);
            if (component.getIsTimer()) {
                if (!component.getLanguage().equals("C")
                        && !component.getLanguage().equals(Language.ADA.name())) {
                    warning(
                            "Converting implementation of timer %s from %s to C",
                            component.getXmlID(), component.getLanguage());
                    component.setLanguage("C");
                    component.setApiVariant(LanguagesHandler.defaultBinding);
                    component.setIsEcoa(false);
                }
            }
            importDoc(ct.getDoc(), component.getDoc());

            importOperations(ct, component);
            if (ct.getProperties() != null) {
                importParameters(ct.getProperties().getProperty(), component.getAttributes(), component.getXmlID());
            }
            if (ct.getPinfos() != null) {
                importPInfos(ct.getPinfos().getPinfo(), component.getPinfos());
            }
            if (ct.getVariables() != null) {
                if (!component.getIsSupervisor()) {
                    errorModel(
                            "variables not allowed in component type '%s'; only SUPERVISORs can contain variables",
                            typeName);
                }
                importParameters(
                        ct.getVariables().getVariable(),
                        component.getVariables(),
                        component.getXmlID());
            }
            if (ct.getTriggers() != null) {
                for (CTTrigger t : ct.getTriggers().getTrigger()) {
                    Trigger trig = new QTrigger();
                    trig.setName(t.getName());
                    for (OperationEvent event : component.getReceivedEvents()) {
                        if (event.getName().equals(t.getEvent())) {
                            trig.setEvent(event);
                            break;
                        }
                    }
                    if (trig.getEvent() == null) {
                        errorModel(
                                "cannot resolve event '%s' for trigger '%s' in component type '%s'",
                                t.getEvent(), t.getName(), typeName);
                    }
                    component.getTriggers().add(trig);
                }
            }
            computeNamesForComponentOrLibrary(component);
           
            importUsedLibraries(component, new ComponentType(typeName, ct));
        }
        assert component != null;
        return component;
    }

    private void setApiVariant(String apiType, Component component) {
        if (apiType == null || apiType.isBlank()) {
            component.setApiVariant(LanguagesHandler.getDefaultApiTypeForLanguage(component.getLanguage()));
        } else {
            component.setApiVariant(apiType);
            component.setIsEcoa(apiType.contains("ECOA"));
        }
    }

    private void computeNamesForComponentOrLibrary(Component component) {
        assert !component.getPackage().isEmpty();
        switch (Language.valueOf(component.getLanguage())) {
        case CPP:
            Collections.addAll(component.getSplittedPackage(), patternColonColon.split(component.getPackage()));
            if (component.getFileprefix().isEmpty()) {
                component.setFileprefix(patternColonColon.matcher(component.getPackage()).replaceAll("_"));
            }
            break;
        case JAVA:
            Collections.addAll(component.getSplittedPackage(), patternDot.split(component.getPackage()));
            component.setFileprefix(component.getPackage().replace(".", "/"));
            break;
        case ADA:
            component.setFileprefix(component.getPackage().toLowerCase());
            break;
        default:
            break;
        }
        if (component.getFileprefix().isEmpty()) {
            component.setFileprefix(component.getPackage());
        }
        assert !component.getFileprefix().isEmpty();
        assert !component.getPackage().isEmpty();
    }

    void importOperations(CTComponentType ct, Component component) throws IOException {
        if (ct.getOperations() != null) {
            for (CTOperation op : ct.getOperations().getAllOperations()) {

                if (op instanceof CTReadData) {
                    OperationData oper = new QOperationData();
                    oper.setIsRead(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    oper.setTypeName(((CTReadData) op).getType());
                    oper.setNotify(((CTReadData) op).isNotifying());
                    oper.setMaxversions(((CTReadData) op).getMaxVersions());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getReadData().add(oper);
                    component.getData().add(oper);
                    component.getOperations().add(oper);

                } else if (op instanceof CTWrittenData) {
                    OperationData oper = new QOperationData();
                    oper.setIsWritten(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    oper.setTypeName(((CTWrittenData) op).getType());
                    oper.setActivating(true); // TODO remove attribute
                    oper.setWriteonly(((CTWrittenData) op).isWriteOnly());
                    oper.setIsRead(!oper.getWriteonly());
                    oper.setNotify(((CTWrittenData) op).isNotifying());
                    oper.setMaxversions(((CTWrittenData) op).getMaxVersions());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getWrittenData().add(oper);
                    component.getData().add(oper);
                    component.getOperations().add(oper);

                    if (!oper.getWriteonly()) {
                        component.getReadData().add(oper);
                    }

                } else if (op instanceof CTReceivedEvent) {
                    OperationEvent oper = new QOperationEvent();
                    oper.setIsReceived(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    importParameters(
                            ((CTReceivedEvent) op).getParameter(),
                            oper.getInParameters(),
                            oper.getXmlID());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getReceivedEvents().add(oper);
                    component.getOperations().add(oper);

                } else if (op instanceof CTSentEvent) {
                    OperationEvent oper = new QOperationEvent();
                    oper.setIsSent(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    importParameters(
                            ((CTSentEvent) op).getParameter(),
                            oper.getInParameters(),
                            oper.getXmlID());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getSentEvents().add(oper);
                    component.getOperations().add(oper);

                    // import timer-specific attributes
                    if (component.getIsTimer()) {
                        BigDecimal period_ms = ((CTSentEvent) op).getPeriod();
                        String s = op.getName();
                        if (period_ms == null && s.startsWith("Every_")) {
                            s = s.replace("Every_", "");
                            try {
                                if (s.endsWith("ms")) {
                                    s = s.replace("ms", "");
                                    period_ms = new BigDecimal(s);
                                } else if (s.endsWith("us")) {
                                    s = s.replace("us", "");
                                    period_ms = new BigDecimal(s).scaleByPowerOfTen(-3);
                                } else {
									errorModel("Timer event name '%s' does not respect the following format: "
											+ "Every_<n>ms or Every_<n>us", oper.getXmlID());
                                }
                            } catch (NumberFormatException e) {
                                errorModel("Period value unreadable for event " + oper.getXmlID(), e);
                            }
                        }
                        if (period_ms != null) {
                            oper.setRepeatTime(convertDuration(period_ms));
                            BigDecimal delay = ((CTSentEvent) op).getDelay();
                            if (delay != null) {
                                if (delay.compareTo(period_ms) > 0)
									warning("Delay is not lower than or equal to the period for event %s", oper.getXmlID());
                                oper.setDelay(convertDuration(delay));
                            }
                        }
						else {
							errorModel("Missing attribute 'period' for event %s", oper.getXmlID());
						}
                    }
                } else if (op instanceof CTRequestReceived) {
                    OperationRequestResponse oper = new QOperationRequestResponse();
                    oper.setIsProvided(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    oper.setMaxDeferred(((CTRequestReceived) op).getMaxConcurrentRequests());
                    oper.setIsDeferred(!(((CTRequestReceived) op).isImmediate()));
                    importParameters(((CTRequestReceived) op).getParameter(), oper.getInParameters(), oper.getXmlID());
                    importParameters(((CTRequestReceived) op).getOut(), oper.getOutParameters(), oper.getXmlID());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getProvidedRequestResponses().add(oper);
                    component.getOperations().add(oper);

                } else if (op instanceof CTRequestSent) {
                    OperationRequestResponse oper = new QOperationRequestResponse();
                    oper.setIsRequired(true);
                    oper.setName(op.getName());
                    oper.setXmlID(operationXmlID(op, component));
                    oper.setIsAsynchronous(!((CTRequestSent) op).isIsSynchronous());
                    oper.setMaxRequests(((CTRequestSent) op).getMaxConcurrentRequests());
                    BigDecimal timeout = ((CTRequestSent) op).getTimeout();
                    if (timeout.compareTo(BigDecimal.ZERO) > 0) {
                        oper.setIsTimed(true);
                        timeout =
                                timeout.scaleByPowerOfTen(
                                        4); // from milliseconds to 100-nano time units
                        oper.setTimeout(timeout.longValue());
                    }
                    importParameters(((CTRequestSent) op).getParameter(), oper.getInParameters(), oper.getXmlID());
                    importParameters(((CTRequestSent) op).getOut(), oper.getOutParameters(), oper.getXmlID());
                    importDoc(op.getDoc(), oper.getDoc());
                    component.getRequiredRequestResponses().add(oper);
                    component.getOperations().add(oper);
                }
            }
        }
    }

    private long convertDuration(BigDecimal ms) {
        // input (models): milliseconds
        // output (formalism): microseconds
        return ms.scaleByPowerOfTen(3).longValue();
    }

    String operationXmlID(CTOperation op, Component component) {
        return "op:" + component.getFullName() + "/" + op.getName();
    }

    void importParameters(List<? extends QualifiedField> source, Collection<Parameter> dest, String xmlID) {
        for (QualifiedField p : source) {
            QParameter par = new QParameter();
            par.setName(p.getName());
            par.setTypeName(p.getType());
            importDoc(p.getDoc(), par.getDoc());
            dest.add(par);
        }
    }

    void importPInfos(List<CTPinfo> source, Collection<PInfo> dest) {
        for (CTPinfo p : source) {
            QPInfo q = new QPInfo();
            q.setName(p.getName());
            q.setIsWritable(p.isWritable());
            importDoc(p.getDoc(), q.getDoc());
            dest.add(q);
        }
    }

    private void importDoc(String doc, Collection<String> dest) {
        if (doc != null) {
            String[] lines = patternNewLine.split(doc);
            // on saute les lignes vides du début (pour compat avec le vieux GenType)
            int i = 0;
            while (i < lines.length && lines[i].isEmpty()) i++;
            for (; i < lines.length; i++) dest.add(lines[i]);
        }
    }

    Pattern patternDot = Pattern.compile(".", Pattern.LITERAL);
    Pattern patternColonColon = Pattern.compile("::", Pattern.LITERAL);
    Pattern patternNewLine = Pattern.compile("\n", Pattern.LITERAL);
    Pattern patternUnderscore = Pattern.compile("_", Pattern.LITERAL);

    protected HashSet<String> importedNow = new HashSet<>();

    class OldOrNewComponent {
        public OldOrNewComponent(Component component, boolean isNew) {
            this.component = component;
            this.isNew = isNew;
        }
        Component component;
        boolean isNew;
    }
    protected OldOrNewComponent addComponentIfAbsent(String nature, String typeName, String implOrLanguage) {
        Component component = findRegisteredComponent( nature,  typeName,  implOrLanguage);
        if (component == null) {
            String fullname = typeName + "/" + implOrLanguage;
            String key = nature + ":" + fullname;
            component = new QComponent();
            component.setXmlID(key);
            component.setTypeName(typeName);
            component.setFullName(fullname);
            component.setImplName(implOrLanguage);
            component.setImplDir(context.workspace.getComponentImplDir(typeName, implOrLanguage).getPath());
            context.system.getComponents().add(component);
            return new OldOrNewComponent(component, true);
        }
        return new OldOrNewComponent(component, false);
    }

    protected Component findRegisteredComponent(String nature, String typeName, String implOrLanguage) {
        String fullname = typeName + "/" + implOrLanguage;
        String key = nature + ":" + fullname;
        for (Component c : context.system.getComponents()) {
            if (c.getXmlID().equals(key)) {
                return c;
            }
        }
        return null;
    }

    /**
     * On associe à chaque lib du système une implémentation en binding SOFTARC_C de la même lib.
     * (soit elle-même, soit une implémentation utilisée par ailleurs, soit une implémentation 
     * créée spécifiquement)
     */
    void addStubImplementations() throws IOException {
        
        LinkedHashMap<String, Component> cComponents = new LinkedHashMap<>();
        LinkedHashSet<Component> otherComponents = new LinkedHashSet<>();
        
        for (Component c : context.system.getComponents()) {
            if (c.getIsLibrary()) {
                // warning: getIsCComponent() is not yet computed!
                if (c.getApiVariant().equals(LanguagesHandler.defaultBinding)) {
                    cComponents.put(c.getTypeName(), c);
                    c.setCComponent(c);
                } else {
                    otherComponents.add(c);
                }
            }
        }

        for (Component c : otherComponents) {
            Component cc = null;
            String typeName = c.getTypeName();
            /*
             * First case: if there is a C implementation of the component already used in the system, use it.
             */
            cc = cComponents.get(typeName);

            if (cc == null) {
                /*
                 * Second case: create a new implementation in C language.
                 */
                cc = importLibrary(typeName, LanguagesHandler.defaultBinding);

                // Nota: Pour les stubs, usedLibraries sera calculé par XRef, à l'aide des vraies
                // dépendances structurelles
                // entre déclarations dans le ComponentType (pas celle indiquées dans une
                // implémentation).
                // cf. XRef.computeAllLibraries()
            }
            // à ce point on a forcément une implémentation C (soit trouvée, soit créée)
            assert cc != null;

            cc.setCComponent(cc);
            c.setCComponent(cc);
            // add the newly created to cComponentsMap, so that it can be used several times
            cComponents.put(typeName, cc);
        }
    }
}
