/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s20.xref;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.UniqueList;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguageHandler;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguagesHandler;

import technology.ecoa.model.datatype.EPredef;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.ConstantDefinition;
import com.thalesgroup.softarc.sf.EnumValue;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.InstanceAttribute;
import com.thalesgroup.softarc.sf.Operation;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.Topic;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.Variable;
import com.thalesgroup.softarc.sf.VariantField;
import com.thalesgroup.softarc.sf.When;
import com.thalesgroup.softarc.sf.impl.QOperationEvent;
import com.thalesgroup.softarc.sf.impl.QWhen;
import com.thalesgroup.softarc.types.Value;
import com.thalesgroup.softarc.types.ValueReader;
import com.thalesgroup.softarc.types.ValueWriter;
import com.thalesgroup.softarc.types.ValueWriters;
import com.thalesgroup.softarc.types.AbstractValueReader.SyntaxError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class XRefCore extends AbstractPass {

    public static final String PASS_ARG_KEY_PREDEF_LIB_NAME = "predefLibName";

    final ValueWriters writers = new ValueWriters();
    final ValueReader reader = new ValueReader(false);

    @Override
    public void execute() {
        createPredefLibs();

        createRedundantCollections();
        computeLanguageFlags();

        resolveReverseReferences();
        createAllTypesAndConstantsMap();

        normalizeTypeDefinitions();

        resolveTypes();
        resolveVariables();

        // TR-SARC-GEN-REQ-142
        sortTypesBasedOnDependencies();

        resolveRealTypes();

        computeIslocal();

        computeIsScalar();
        computeIsSwitchable();

        resolveConstants();
        resolveRanges();
        // Les ranges sont resolus, on peut verifier les range
        reader.setAutoCheckBounds(true);
        resolveValues();
        buildEnumTypes();

        // TR-SARC-GEN-REQ-141
        computeQName();
        computeQType();

        computeNeedsCheckAndValidation();
        computeAllLibraries();

        checkTypes();
        checkVariables();

        adjustVariousObjects();

        defineEnumValues();
        sortEnumValues();

        checkRecord();

        addVirtualEvents();
    }

    void createPredefLibs() {
        // Identifie les languages utilisés dans ce système, crée les predefLibs
        // correspondantes
        // TR-SARC-GEN-REQ-136_137
        // Nota: la lib ECOA a déjà été créee avant car elle doit avoir un stub
        // en C.
        // C avec API SOFTARC est toujours utilisé pour des raisons internes
        useLanguage("C", LanguagesHandler.defaultBinding);

        context.system.getComponentsIncludingInterface().addAll(context.system.getComponents());
        if (context.system.getInterface() != null) {
            context.system.getComponentsIncludingInterface().add(context.system.getInterface());
        }

        for (Component comp : context.system.getComponentsIncludingInterface()) {
            useLanguage(comp.getLanguage(), comp.getApiVariant());
        }
    }

    void useLanguage(String lang, String apiVariant) {
        if (lang.isBlank())
            return;
        if (!usedLanguages.containsKey(apiVariant)) {
            LanguageHandler handler = LanguagesHandler.get(apiVariant);
            info(String.format("Using language %s with API %s", lang, apiVariant));
            usedLanguages.put(apiVariant, handler);
            context.system.getPredefLib().add(handler.predefTypesLib);
            if (context.args.containsKey(PASS_ARG_KEY_PREDEF_LIB_NAME)) {
                handler.predefTypesLib.setPackage((String) context.args.get(PASS_ARG_KEY_PREDEF_LIB_NAME));
                handler.predefTypesLib.setFullName(handler.predefTypesLib.getTypeName() + '/'
                        + handler.predefTypesLib.getImplName());
                for (TypeDefinition td : handler.predefTypesLib.getTypes()) {
                    td.setQName(handler.getQName(td));
                }
            }
        }
    }

    void createRedundantCollections() {
        allComponentsAndLibraries.addAll(context.system.getComponentsIncludingInterface());
        allComponentsAndLibraries.addAll(context.system.getPredefLib());

        for (Component component : allComponentsAndLibraries) {
            allTypesList.addAll(component.getTypes());
        }
    }

    void resolveReverseReferences() {
        for (Component component : allComponentsAndLibraries) {
            for (TypeDefinition td : component.getTypes()) {
                td.setParent(component);
            }
        }
        for (Instance i : context.system.getAssembly().getInstancesIncludingExtern()) {
            for (InstanceAttribute a : i.getAttributes()) {
                a.setParent(i);
            }
        }
    }

    void createAllTypesAndConstantsMap() {
        for (Component component : allComponentsAndLibraries) {
            Set<String> usednames = new LinkedHashSet<String>();
            
            // types
            for (TypeDefinition d : component.getTypes()) {
                if (usednames.add(d.getName()) != true) {
                    errorModel("duplicate definition of type definition %s", d.getName());
                }
                typesDictionnary.put(d, d.getName(), component);
            }

            // constants
            for (ConstantDefinition d : component.getConstants()) {
                if (usednames.add(d.getName()) != true) {
                    errorModel("duplicate definition of constant definition %s", d.getName());
                }
                constantsDictionnary.put(d, d.getName(), component);
            }
        }
    }

    void normalizeTypeDefinitions() {

        for (TypeDefinition td : allTypesList) {

            td.setIsChar8Array(td.getIsFixedArray() && td.getBaseTypeName().equals("char8"));
            td.setIsDocumented(!td.getDoc().isEmpty());
            td.setHasUnit(td.getUnit() != null);
            for (EnumValue ev : td.getEnumValues()) {
                ev.setIsDocumented(!ev.getDoc().isEmpty());
            }
        }
    }

    private TypeDefinition lookupTypeName(String reference, Component fromComponent) {
        TypeDefinition result = typesDictionnary.lookup(reference, fromComponent);
        if (result == null)
            errorModel("cannot resolve type reference '%s', used from %s/%s in %s", reference, fromComponent.getTypeName(),
                    fromComponent.getImplName(), fromComponent.getLanguage());
        return result;
    }

    private ConstantDefinition lookupConstantName(String reference, Component fromComponent) {
        ConstantDefinition result = constantsDictionnary.lookup(reference, fromComponent);
        if (result == null)
            errorModel("cannot resolve reference to constant '%s', used from %s/%s", reference, fromComponent.getTypeName(),
                    fromComponent.getImplName());
        return result;
    }


    void resolveTypes() {
        // TR-SARC-GEN-REQ-139
        for (TypeDefinition t : allTypesList) {
            if (!t.getBaseTypeName().isEmpty()) {
                t.setBaseType(lookupTypeName(t.getBaseTypeName(), t.getParent()));
                t.setBaseTypeName(t.getBaseType().getTypeName());
                // type = baseType
                t.setType(t.getBaseType());
            }
            if (!t.getKeyTypeName().isEmpty()) {
                t.setKeyType(lookupTypeName(t.getKeyTypeName(), t.getParent()));
                t.setKeyTypeName(t.getKeyType().getTypeName());
            }
        }

        // TR-SARC-GEN-REQ-140
        for (Component component : context.system.getComponentsIncludingInterface()) {
            for (Operation op : component.getOperations()) {
                resolveParameters(op.getInParameters(), component);
                resolveParameters(op.getOutParameters(), component);
            }
            // TR-SARC-GEN-REQ-144
            // Type and qualified name of instance attributes resolution
            resolveParameters(component.getAttributes(), component);
            resolveParameters(component.getVariables(), component);
            for (TypeDefinition td : component.getTypes()) {
                resolveParameters(td.getFields(), component);
                resolveParameters(td.getUnionFields(), component);
                resolveParameter(td.getDefaultUnionField(), component);
            }
        }

        for (Component component : context.system.getComponentsIncludingInterface()) {
            for (OperationData op : component.getData()) {
                op.setType(lookupTypeName(op.getTypeName(), component));
                op.setTypeName(op.getType().getTypeName());
                notifyComponentDependency(component, op.getType().getParent());
            }
        }
    }

    void resolveParameters(Collection<? extends Parameter> parameters, Component fromComponent) {
        for (Parameter p : parameters) {
            resolveParameter(p, fromComponent);
        }
    }

    void resolveParameter(Parameter p, Component fromComponent) {
        if (p != null) {
            p.setType(lookupTypeName(p.getTypeName(), fromComponent));
            p.setTypeName(p.getType().getTypeName());
            allParametersList.add(p);
            notifyComponentDependency(fromComponent, p.getType().getParent());
        }
    }

    void resolveRealTypes() {
        for (TypeDefinition td : allTypesList) {
            TypeDefinition rt = td.getBaseType();

            if (rt != null) {
                if (rt.getIsSimple() || rt.getIsEnum()) {
                    rt = rt.getRealType();
                }
            } else {
                rt = td;
            }
            td.setRealType(rt);

            td.setRealTypeIsLocal(td.getRealType().getParent().equals(td.getParent()));
        }
    }

    /**
     * In all types, replace symbolic constants by their value.
     */
    private void resolveConstants() {

        for (Component component : allComponentsAndLibraries) {
            for (ConstantDefinition cst : component.getConstants()) {
                cst.setType(lookupTypeName(cst.getTypeName(), component).getRealType());
                cst.setTypeName(cst.getType().getTypeName());
                cst.setValueOrConstant(resolveConstant(cst.getValueOrConstant(), component));
                notifyComponentDependency(component, cst.getType().getParent());
            }
        }

        for (Component component : allComponentsAndLibraries) {
            for (TypeDefinition td : component.getTypes()) {

                td.setArraySizeOrConstant(resolveConstant(td.getArraySizeOrConstant(), td.getParent()));

                td.setMinRange(resolveConstant(td.getMinRange(), component));
                td.setMaxRange(resolveConstant(td.getMaxRange(), component));

                for (EnumValue ev : td.getEnumValues()) {
                    ev.setValnumOrConstant(resolveConstant(ev.getValnumOrConstant(), td.getParent()));
                }
            }
        }

    }

    final static BigDecimal Epsilon = new BigDecimal(BigInteger.ONE, 6); // 10^-6
    final static BigDecimal EpsilonRelativeBigger = BigDecimal.ONE.add(Epsilon);
    final static BigDecimal EpsilonRelativeSmaller = BigDecimal.ONE.subtract(Epsilon);
    final static BigDecimal NegativeOne = new BigDecimal("-1");

    private void resolveRanges() {

        for (Component component : allComponentsAndLibraries) {
            for (TypeDefinition td : component.getTypes()) {
                if (td.getIsNumeric()) {

                    // Inherit constraints from parent types.
                    TypeDefinition baseType = td;
                    HashSet<TypeDefinition> parents = new HashSet<TypeDefinition>();
                    while (baseType != null && parents.add(baseType)) {
                        baseType = baseType.getBaseType();
                        if (td.getMinRange().isEmpty()) {
                            td.setMinRange(baseType.getMinRange());
                        }
                        if (td.getMaxRange().isEmpty()) {
                            td.setMaxRange(baseType.getMaxRange());
                        }
                    }

                    // Keep a copy of values as expressed in the input models
                    // (after constants resolutions, but without any change of value or syntax).
                    td.setMinRangeOriginal(td.getMinRange());
                    td.setMaxRangeOriginal(td.getMaxRange());

                    try {
                        if (isFloatingType(td.getRealType())) {

                            BigDecimal minRangePhysicalType = new BigDecimal(td.getRealType().getMinPhysical());
                            BigDecimal maxRangePhysicalType = new BigDecimal(td.getRealType().getMaxPhysical());
                            BigDecimal minRange = new BigDecimal(reader.read(td.getMinRange(), td).toString());
                            BigDecimal maxRange = new BigDecimal(reader.read(td.getMaxRange(), td).toString());

                            if (minRange.compareTo(BigDecimal.ZERO) == 0) {
                                // rien a faire
                            } else if (minRange.compareTo(BigDecimal.ONE) > 0) {
                                // minRange > 1, on l'approche de 1
                                minRange = minRange.multiply(EpsilonRelativeSmaller);
                            } else if (minRange.compareTo(NegativeOne) < 0) {
                                // minRange < -1, on l'eloigne de -1
                                minRange = minRange.multiply(EpsilonRelativeBigger);
                            } else if (minRange.compareTo(BigDecimal.ZERO) > 0) {
                                // minRange in [0;1], on l'eloigne de 1
                                minRange = minRange.subtract(Epsilon);
                            } else {
                                // minRange in [-1;0], on l'approche de -1
                                minRange = minRange.subtract(Epsilon);
                            }
                            
                            if (maxRange.compareTo(BigDecimal.ZERO) == 0) {
                                // rien a faire
                            } else if (maxRange.compareTo(BigDecimal.ONE) > 0) {
                                // maxRange > 1, on l'eloigne de 1
                                maxRange = maxRange.multiply(EpsilonRelativeBigger);
                            } else if (maxRange.compareTo(NegativeOne) < 0) {
                                // maxRange < -1, on l'approche de -1
                                maxRange = maxRange.multiply(EpsilonRelativeSmaller);
                            } else if (maxRange.compareTo(BigDecimal.ZERO) > 0) {
                                // maxRange in [0;1], on l'approche de 1
                                maxRange = maxRange.add(Epsilon);
                            } else {
                                // maxRange in [-1;0], on l'eloigne de -1
                                maxRange = maxRange.add(Epsilon);
                            }

                            // Si finalement la tolerance fait un over/underflow, on sature
                            if (minRange.compareTo(minRangePhysicalType) < 0) {
                                minRange = minRangePhysicalType;
                            }

                            if (maxRange.compareTo(maxRangePhysicalType) > 0) {
                                maxRange = maxRangePhysicalType;
                            }

                            td.setMinRange(minRange.toString());
                            td.setMaxRange(maxRange.toString());
                        } else {
                            td.setMinRange(reader.read(td.getMinRange(), td).toString());
                            td.setMaxRange(reader.read(td.getMaxRange(), td).toString());
                        }

                    } catch (SyntaxError e) {
                        errorModel("Invalid range values for %s : %s", td, e.getMessage());
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void resolveValues() {

        for (Component component : allComponentsAndLibraries) {
            for (TypeDefinition td : component.getTypes()) {
                if (!td.getArraySizeOrConstant().isEmpty()) {
                    String language = td.getParent().getLanguage();
                    // Pour les externes
                    if (language.isEmpty()) {
                        language = Language.C.toString();
                    }
                    td.setArraySize(resolveValueInt(td.getArraySizeOrConstant(),
                            LanguagesHandler.getSoftarc(language).predefTypes.get(EPredef.UINT_32)));
                }

                if (td.getIsNumeric()) {

                    td.setMinRangeValue(resolveValue(td.getMinRange(), td));
                    td.setMaxRangeValue(resolveValue(td.getMaxRange(), td));

                }

                if (td.getIsEnum()) {
                    TypeDefinition firstNumericBaseType = td.getBaseType();
                    while (!firstNumericBaseType.getIsNumeric()) {
                        firstNumericBaseType = firstNumericBaseType.getBaseType();
                    }
                    for (EnumValue ev : td.getEnumValues()) {
                        if (!ev.getValnumOrConstant().isEmpty()) {
                            ev.setValnum(resolveValueInt(ev.getValnumOrConstant(), firstNumericBaseType));
                        }
                    }

                }

            }
        }

        for (Component component : allComponentsAndLibraries) {
            for (ConstantDefinition cst : component.getConstants()) {
                cst.setRealValue(resolveValue(cst.getValueOrConstant(), cst.getType()));
            }
        }
    }

    private void buildEnumTypes() {
        for (Component component : allComponentsAndLibraries) {
            for (TypeDefinition td : component.getTypes()) {
                if (td.getIsEnum()) {
                    if (td.getBaseType().getIsEnum()) {
                        for (EnumValue ev : td.getEnumValues()) {
                            boolean isFound = false;
                            // check that enum based on enum have the same values ant not more
                            for (EnumValue parentEv : td.getBaseType().getEnumValues()) {
                                if (ev.getName().equals(parentEv.getName())) {
                                    if (!ev.getValnumOrConstant().equals(parentEv.getValnumOrConstant())) {
                                        errorModel("Value %s (%s) of %s has not the same value in parent type %s",
                                                ev.getValnumOrConstant(), ev.getName(), td, td.getBaseType());
                                    }
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                errorModel("Value %s (%s) in %s is not present in parent type %s", ev.getValnumOrConstant(),
                                        ev.getName(), td, td.getBaseType());
                            }

                        }

                    }
                    
                    // Force base type to real type, from now everything is based on real (preDef) type, 
                    // range has been checked and enum and base type act as disjoined from each other (no strong typing)
                    td.setBaseType(td.getRealType());
                    td.setBaseTypeName(td.getRealType().getName());
                    td.setBaseTypeIsLocal(td.getRealType().getParent() == td.getParent());
                    td.setType(td.getRealType());
                }
            }
        }
    }

    private String resolveConstant(String valueOrConstantReference, Component fromComponent) {
        if (valueOrConstantReference.startsWith("%") && valueOrConstantReference.endsWith("%")) {
            String name = valueOrConstantReference.replaceAll("%", "");
            ConstantDefinition cst = lookupConstantName(name, fromComponent);
            return resolveConstant(cst.getValueOrConstant(), fromComponent);
        }
        return valueOrConstantReference;
    }

    private String resolveValue(String litteral, TypeDefinition td) {
        String language = td.getParent().getLanguage();
        if (language.isEmpty()) {
            return litteral;
        } else {
            try {
                ValueWriter writer = writers.getValueWriter(language);
                return writer.write(reader.read(litteral, td.getRealType()));
            } catch (Exception e) {
                errorModel("Invalid value for %s : %s", td, e.getMessage());
                return "";
            }
        }
    }

    private long resolveValueInt(String value, TypeDefinition td) {
        try {
            Value v = reader.read(value, td);
            return Long.parseLong(v.toString());
        } catch (NumberFormatException | SyntaxError e) {
            errorModel("Expected integer value '%s' used in definition of '%s': %s", value, td, e.getMessage());
        }
        return 0;
    }

    void resolveVariables() {
        for (Instance instance : context.system.getAssembly().getInstances()) {
            for (Variable v : instance.getVariables()) {

                for (Parameter variableType : instance.getType().getVariables()) {
                    if (v.getName().equals(variableType.getName())) {
                        v.setType(variableType.getType());
                        break;
                    }
                }
                if (v.getType() == null) {
                    errorModel("variable '%s' in %s is not declared in instance %s", v.getName(), instance.getName(), instance.getType()
                            .getXmlID());
                }
            }
        }
    }

    void checkVariables() {
        for (Instance instance : context.system.getAssembly().getInstances()) {
            for (Variable v : instance.getVariables()) {
                String typeName = v.getType().getRealType().getName();
                if (!typeName.equals("uint32") && !typeName.equals("int32")) {
                    errorModel("variable '%s' in instance %s shall be based on int32 or uint32 type", v.getName(), instance.getName());
                }
            }
        }
    }

    // ////////////////

    void sortTypesBasedOnDependencies() {
        for (Component component : allComponentsAndLibraries) {

            // Build input list of types to be sorted, and compute their local
            // dependencies
            Collection<TypeDefinition> list = component.getTypes();

            // Verify that reserved names are not used
            if (!component.getIsPredefLib()) {
                List<TypeDefinition> ill_named_types = new ArrayList<TypeDefinition>();

                for (TypeDefinition t : list) {
                    try {
                        EPredef.fromValue(t.getName());
                        ill_named_types.add(t);
                    } catch (IllegalArgumentException e) {
                        // Nothing to do
                    }
                }
                if (ill_named_types.size() > 0) {
                    StringBuffer message = new StringBuffer("Following reserved typenames cannot be used in '"
                            + component.getPackage() + "':");

                    for (TypeDefinition t : ill_named_types) {
                        message.append(" '" + t.getName() + "'");
                    }
                    errorModel(message.toString());
                }
            }

            // Sort types by dependencies
            try {
                sortTypes(list, true);
            } catch (CircularDependencyException e) {
                errorModel("Circular type dependencies found, involving type %s.%s", e.circular_type.getParent().getPackage(),
                        e.circular_type.getName());
            }
        }

        try {
            sortTypes(allTypesList, false);
        } catch (CircularDependencyException e) {
            errorInternal("Circular type dependencies found in global list of types, involving type %s.%s", e.circular_type
                    .getParent().getPackage(), e.circular_type.getName());
        }

    }

    @SuppressWarnings("serial")
    class CircularDependencyException extends Exception {

        TypeDefinition circular_type;

        public CircularDependencyException(TypeDefinition circular_type) {
            this.circular_type = circular_type;
        }
    }

    private void sortTypes(Collection<TypeDefinition> list, boolean locally) throws CircularDependencyException {

        // Compute dependencies for all types
        UniqueList<TypeAndDependencies> inputList = new UniqueList<TypeAndDependencies>();
        for (TypeDefinition td : list) {
            TypeAndDependencies dep = new TypeAndDependencies(td, locally);
            inputList.add(dep);
        }

        if (!locally) {
            // Enrich "usedLibraries" for components, computed from type
            // dependencies (not from implementations).
            // For stubs, usedLibraries are empty at this point.
            for (TypeAndDependencies dep : inputList) {
                Component user = dep.definition.getParent();
                for (TypeDefinition td : dep.dependencies) {
                    notifyComponentDependency(user, td.getParent());
                }
            }
        }

        // Clear the output
        list.clear();

        while (!inputList.isEmpty()) {
            // Search for a type which depends on no other in inputList
            TypeAndDependencies candidate = null;
            for (TypeAndDependencies t : inputList) {
                if (t.dependencies.isEmpty()) {
                    candidate = t;
                    break;
                }
            }

            if (candidate == null) {
                throw new CircularDependencyException(inputList.get(0).definition);
            }
            // Remove it from the input
            inputList.remove(candidate);
            // Add it to the output
            list.add(candidate.definition);
            // Update remaining types
            for (TypeAndDependencies other : inputList) {
                other.dependencies.remove(candidate.definition);
            }
        }
    }

    protected void notifyComponentDependency(Component user, Component used) {
        
        if (user != used && !used.getIsPredefLib()) {
            // Warning: Stubs are C components, but as they are copied from non-C components, 
            // they may be defined with non-C types
            // => ensure that C components only use C components
            if (user.getLanguage().equals("C"))
                used = used.getCComponent();
            
            if (used != null && user != used && !user.getUsedLibraries().contains(used)) {
                gen.debug("notifyComponentDependency: %s -> %s", user, used);
                // check that language is the same, if defined (except for system interface)
                if (!user.getLanguage().isEmpty() && !used.getLanguage().isEmpty()
                        && !user.getLanguage().equals(used.getLanguage()))
                    errorInternal("%s cannot use %s because their language is different", user.toString(), used.toString());
                user.getUsedLibraries().add(used);
            }
        }
    }
    
    void treatOperation(Operation o, LanguageHandler languageHandler) {
        for (Parameter v : o.getInParameters()) {
            v.setName(languageHandler.avoidKeywords(v.getName()));
            v.setQName(languageHandler.getQName(v.getName()));
        }
        for (Parameter v : o.getOutParameters()) {
            v.setName(languageHandler.avoidKeywords(v.getName()));
            v.setQName(languageHandler.getQName(v.getName()));
        }
    }

    // ////////////////

    void computeQName() {
        for (Component c : allComponentsAndLibraries) {
            String language = c.getLanguage();
            if (!language.isEmpty()) {
                LanguageHandler languageHandler = LanguagesHandler.get(c.getApiVariant());
                for (TypeDefinition td : c.getTypes()) {

                    // avoid language keywords
                    td.setName(languageHandler.avoidKeywords(td.getName()));
                    for (Parameter v : td.getFields()) {
                        v.setName(languageHandler.avoidKeywords(v.getName()));
                        v.setQName(languageHandler.getQName(v.getName()));
                    }
                    for (Parameter v : td.getUnionFields()) {
                        v.setName(languageHandler.avoidKeywords(v.getName()));
                        v.setQName(languageHandler.getQName(v.getName()));
                    }
                    for (EnumValue v : td.getEnumValues()) {
                        v.setName(languageHandler.avoidKeywords(v.getName()));
                        v.setQName(languageHandler.getQValue(td, v.getName()));
                    }

                    // calcul de qName, qWhen, wWhen1
                    if (td.getQName().isEmpty()) {
                        td.setQName(languageHandler.getQName(td));
                        if (td.getIsVariantRecord()) {
                            Set<String> enumValues = new LinkedHashSet<>(); 
                            if (EPredef.fromValue(td.getRealType().getName()) == EPredef.BOOLEAN_8) {
                                td.setIsVariantRecordBooleanBased(true);
                                enumValues.add("true");
                                enumValues.add("false");
                            } else if (td.getType().getIsEnum()) {
                                td.setIsVariantRecordEnumBased(true);
                                for (EnumValue e : td.getBaseType().getEnumValues()) {
                                    enumValues.add(e.getName());
                                }                               
                            }
                            for (VariantField p : td.getUnionFields()) {
                                p.setName(languageHandler.avoidKeywords(p.getName()));
                                p.setQName(languageHandler.getQName(p.getName()));
                                if (td.getIsVariantRecordBooleanBased()) {
                                    boolean isTrue = false;
                                    try {
                                        isTrue = (Byte.valueOf(p.getWhen()) != 0);
                                    } catch (NumberFormatException e) {
                                        if (p.getWhen().toLowerCase().equals("true")) {
                                            isTrue = true;
                                        } else if (p.getWhen().toLowerCase().equals("false")) {
                                            isTrue = false;
                                        } else {
                                            errorModel("Invalid value for when='" + p.getWhen() + "' in '" + c.getTypeName()
                                                    + ":" + td.getName() + "'");
                                        }
                                    }
                                    enumValues.remove(isTrue ? "true" : "false");
                                    p.setQWhen(languageHandler.getBooleanValue(isTrue));
                                } else {
                                    p.setWhen(languageHandler.avoidKeywords(p.getWhen()));
                                    if (td.getIsVariantRecordEnumBased()) {
                                        if (!enumValues.remove(p.getWhen())) {
                                            errorModel(
                                                    "Invalid value for when='%s' of union for type '%s:%s' : it does not exist in the selector type '%s'",
                                                    p.getWhen(), c.getTypeName(), td.getName(),
                                                    td.getBaseType().getName());
                                        }
                                    }
                                    p.setQWhen(languageHandler.getQValue(td.getBaseType(), p.getWhen()));
                                }
                            }
                            
                            td.setQWhen1(td.getUnionFields().iterator().next().getQWhen());
                            if (td.getDefaultUnionField() != null) {
                                Parameter p = td.getDefaultUnionField();
                                p.setName(languageHandler.avoidKeywords(p.getName()));
                                p.setQName(languageHandler.getQValue(td, p.getName()));
                            }
                            
                            for (String e : enumValues) {
                                When p = new QWhen();
                                p.setWhen(languageHandler.avoidKeywords(e));
                                p.setQWhen(languageHandler.getQValue(td.getBaseType(), p.getWhen()));
                                td.getUnspecifiedEnumValues().add(p);
                            }
                        }
                    }

                    languageHandler.computeStuff(td);
                }
                // qName des constantes
                for (ConstantDefinition cst : c.getConstants()) {
                    cst.setName(languageHandler.avoidKeywords(cst.getName()));
                    cst.setQName(languageHandler.getQName(cst, c));
                }
                
                for (OperationEvent o : c.getReceivedEvents()) {
                    treatOperation(o, languageHandler);
                }
                for (OperationEvent o : c.getSentEvents()) {
                    treatOperation(o, languageHandler);
                }
                for (OperationRequestResponse o : c.getProvidedRequestResponses()) {
                    treatOperation(o, languageHandler);
                }
                for (OperationRequestResponse o : c.getRequiredRequestResponses()) {
                    treatOperation(o, languageHandler);
                }
                for (OperationData o : c.getData()) {
                    treatOperation(o, languageHandler);
                }
                for (OperationData o : c.getReadData()) {
                    treatOperation(o, languageHandler);
                }
                
                for (Parameter v : c.getAttributes()) {
                    v.setName(languageHandler.avoidKeywords(v.getName()));
                    v.setQName(languageHandler.getQName(v.getName()));
                }
                
                for (Parameter v : c.getVariables()) {
                    v.setName(languageHandler.avoidKeywords(v.getName()));
                    v.setQName(languageHandler.getQName(v.getName()));
                }
                
                for (Topic v : c.getTopics()) {
                    v.setName(languageHandler.avoidKeywords(v.getName()));
                    v.setName(languageHandler.getQName(v.getName()));
                }
            }
        }
    }

    void computeLanguageFlags() {
        for (Component component : allComponentsAndLibraries) {
            component.setIsCComponent(component.getLanguage().equals(Language.C.name()));
            component.setIsCppComponent(component.getLanguage().equals(Language.CPP.name()));
        }
    }

    private boolean isFloatingType(TypeDefinition td) {
        return td.getRealType().getTypeName().equals(EPredef.FLOAT_32.value())
                || td.getRealType().getTypeName().equals(EPredef.DOUBLE_64.value());
    }

    private boolean isBooleanType(TypeDefinition td) {
        return td.getRealType().getTypeName().equals(EPredef.BOOLEAN_8.value());
    }

    void computeIsScalar() {
        for (TypeDefinition td : allTypesList) {
            td.setIsNumeric(td.getIsPredef() || td.getIsSimple());
            td.setIsScalar(td.getIsNumeric() || td.getIsEnum());
            td.setIsInteger(td.getRealType().getIsPredef() && !isBooleanType(td) && !isFloatingType(td));
        }
    }

    void computeIsSwitchable() {
        for (TypeDefinition td : allTypesList) {
            // predef : already defined by language handler
            if (td.getIsEnum() || td.getIsSimple()) {
                td.setIsSwitchable(td.getRealType().getIsSwitchable());
            }
            // info("isSwitchable(%s)=%s", td.getFullName(),
            // td.getIsSwitchable());
        }
    }

    void computeNeedsCheckAndValidation() {
        for (TypeDefinition td : allTypesList) {
            if (td.getIsPredef()) {
                td.setNeedsMinCheck(compareValue(td.getMinRange(), td.getRealType().getMinPhysical()) != 0);
                td.setNeedsMaxCheck(compareValue(td.getMaxRange(), td.getRealType().getMaxPhysical()) != 0);
                td.setNeedsCheck(td.getNeedsMinCheck() || td.getNeedsMaxCheck());
                td.setNeedsValidation(false);
            } else if (td.getIsSimple()) {
                td.setNeedsMinCheck(td.getRealType().getNeedsMinCheck()
                        || compareValue(td.getMinRange(), td.getRealType().getMinPhysical()) != 0);
                td.setNeedsMaxCheck(td.getRealType().getNeedsMaxCheck()
                        || compareValue(td.getMaxRange(), td.getRealType().getMaxPhysical()) != 0);
                td.setNeedsCheck(td.getNeedsMinCheck() || td.getNeedsMaxCheck());
                td.setNeedsValidation(false);
            } else if (td.getIsArray() || td.getIsList() || td.getIsMap()) {
                // the size can always be checked
                td.setNeedsCheck(true);
                td.setNeedsValidation(td.getBaseType().getNeedsValidation());
            } else if (td.getIsFixedArray()) {
                // a fixed array can be checked iff the type of its elements can
                // be checked
                td.setNeedsCheck(td.getBaseType().getNeedsCheck());
                td.setNeedsValidation(td.getBaseType().getNeedsValidation());
            } else if (td.getIsEnum()) {
                td.setNeedsCheck(true);
                td.setNeedsValidation(false);
            } else if (td.getIsRecord()) {
                // a record can be checked iff one of its fields can be checked
                for (Parameter f : td.getFields()) {
                    if (f.getType().getNeedsCheck())
                        td.setNeedsCheck(true);
                    if (f.getType().getNeedsValidation())
                        td.setNeedsValidation(true);
                }
            } else if (td.getIsVariantRecord()) {
                for (Parameter f : td.getFields()) {
                    if (f.getType().getNeedsCheck())
                        td.setNeedsCheck(true);
                    if (f.getType().getNeedsValidation())
                        td.setNeedsValidation(true);
                }
                for (Parameter f : td.getUnionFields()) {
                    if (f.getType().getNeedsCheck())
                        td.setNeedsCheck(true);
                    if (f.getType().getNeedsValidation())
                        td.setNeedsValidation(true);
                }
                if (td.getBaseType().getNeedsCheck())
                    td.setNeedsCheck(true);
                if (td.getBaseType().getNeedsValidation())
                    td.setNeedsValidation(true);
            } else if (td.getIsString()) {
                td.setNeedsCheck(false);
                td.setNeedsValidation(true);
            }
        }
    }

    void computeQType() {
        for (Parameter p : allParametersList) {
            p.setQType(p.getType().getQName());
        }
        for (TypeDefinition t : allTypesList) {
            if (!t.getBaseTypeName().isEmpty()) {
                t.setQType(t.getBaseType().getQName());
            }
        }
        for (Component component : context.system.getComponentsIncludingInterface()) {
            for (OperationData op : component.getData()) {
                op.setQType(op.getType().getQName());
            }
        }
    }

    void adjustVariousObjects() {
        for (Parameter p : allParametersList) {
            p.setIsDocumented(!p.getDoc().isEmpty());
        }
        for (Component component : context.system.getComponentsIncludingInterface()) {
            for (Operation op : component.getOperations()) {
                op.setIsDocumented(!op.getDoc().isEmpty());
                op.setHasInParameters(!op.getInParameters().isEmpty());
                op.setHasOutParameters(!op.getOutParameters().isEmpty());
                op.setHasParameters(op.getHasOutParameters() || op.getHasInParameters());
                op.setHasDocumentedInParameters(hasDoc(op.getInParameters()));
                op.setHasDocumentedOutParameters(hasDoc(op.getOutParameters()));
                op.setHasDocumentedParameters(op.getHasDocumentedInParameters() || op.getHasDocumentedOutParameters());
            }

            for (TypeDefinition t : component.getTypes()) {
                if (t.getNeedsValidation()) {
                    component.setNeedsValidation(true);
                    break;
                }
            }
        }
    }

    private boolean hasDoc(Collection<Parameter> list) {
        for (Parameter p : list)
            if (p.getIsDocumented())
                return true;
        return false;
    }

    void defineEnumValues() {
        for (TypeDefinition td : allTypesList) {
            if (td.getIsEnum()) {
            	td.setIsEnumBooleanBased(EPredef.fromValue(td.getRealType().getName()) == EPredef.BOOLEAN_8);
            	// Register used values
                Set<String> usednames = new LinkedHashSet<String>();
                Set<Long> usedvalues = new LinkedHashSet<Long>();
                for (EnumValue value : td.getEnumValues()) {
                    if (value.getIsUserDefined()) {
                        if (usednames.add(value.getName()) != true) {
                            errorModel("duplicate name %s in enumeration %s", value.getName(), td.getXmlID());
                        }
                        if (usedvalues.add(value.getValnum()) != true) {
                            errorModel("duplicate value %d in enumeration %s", value.getValnum(), td.getXmlID());
                        }
                    }
                }

                long val = 0;
                for (EnumValue v : td.getEnumValues()) {
                    if (!v.getIsUserDefined()) {
                        // Loop until we find a free value
                        while (usedvalues.contains(val)) {
                            val++;
                        }
                        // Set enum value
                        v.setValnum(val);
                        val++;
                    }
                }

                // check that enums based on boolean8 have 2 values
                if (!td.getRealType().getIsInteger()) {
                    if (td.getEnumValues().size() != 2)
                        errorModel("Enum %s is based on boolean8, but has %d values instead of 2", td, td.getEnumValues().size());
                }

                // set ValnumLitteral attributes
                for (EnumValue ev : td.getEnumValues()) {
                    if (!td.getRealType().getIsInteger()) {
                        // enum types based on boolean8 are *not* based on the boolean type of the language (e.g. in Ada)
                        ev.setValnumLitteral(Long.toString(ev.getValnum()));
                    } else {
                        ev.setValnumLitteral(resolveValue(Long.toString(ev.getValnum()), td.getRealType()));
                    }
                }
            }
        }
    }

    void sortEnumValues() {
        for (TypeDefinition td : allTypesList) {
            if (td.getIsEnum()) {
                UniqueList<EnumValue> list = new UniqueList<>();
                list.addAll(td.getEnumValues());
                Comparator<EnumValue> enumValuesComparator = new Comparator<EnumValue>() {
                    @Override
                    public int compare(EnumValue o1, EnumValue o2) {
                        return Long.compare(o1.getValnum(), o2.getValnum());
                    }
                };
                Collections.sort(list, enumValuesComparator);
                td.getSortedEnumValues().addAll(list);
            }
        }
    }

    void addVirtualEvents() {
        for (Component component : context.system.getComponents()) {
            for (OperationData op : component.getReadData()) {
                if (op.getNotify()) {
                    QOperationEvent notify = new QOperationEvent();
                    notify.setName("sarc_notify_" + op.getName());
                    notify.setDataNotification(op);
                    notify.setVirtual(true);
                    notify.setXmlID("op:" + component.getFullName() + "/" + notify.getName());
                    notify.setIsReceived(true);
                    component.getReceivedEvents().add(notify);
                    component.getOperations().add(notify);
                }
            }
        }
    }

    void checkRecord() {
        for (TypeDefinition td : allTypesList) {
            if (td.getIsRecord() || td.getIsVariantRecord()) {
                List<Parameter> list = td.getFields();
                Set<String> usednames = new LinkedHashSet<String>();
                for (Parameter p : list) {
                    if (usednames.add(p.getName()) != true) {
                        errorModel("duplicate name %s in record %s", p.getName(), td.getXmlID());
                    }
                }
            }
        }
    }

    private int compareValue(String v1, String v2) {
        BigDecimal value1 = new BigDecimal(v1);
        BigDecimal value2 = new BigDecimal(v2);
        return value1.compareTo(value2);
    }

    void checkTypes() {
        final HashSet<String> predefNames = new HashSet<>();
        for (EPredef p : EPredef.values()) {
            predefNames.add(p.value());
        }

        for (TypeDefinition td : allTypesList) {

            // Vérifie qu'aucun type ne réutilise le nom d'un type prédéfini
            if (!td.getIsPredef()) {
                if (predefNames.contains(td.getName()))
                    errorModel("name of type '%s', defined in %s, is reserved", td.getName(), td.getParent().getTypeName());
            }

            if (td.getIsSimple()) {
                TypeDefinition baseType = td.getBaseType();
                // Vérification que tous les types simples dérivent d'un autre
                // type simple ou
                // prédéfini
                if (baseType == null || (!baseType.getIsSimple() && !baseType.getIsPredef()))
                    errorModel("simple type %s shall be based on a simple or predef type", td);

                if (td.getIsNumeric()) {
                    // Vérification des bornes entre elles
                    if (compareValue(td.getMinRange(), td.getMaxRange()) > 0) {
                        errorModel("simple type %s: bounds are inverted: minrange=%s, maxrange=%s", td, td.getMinRange(),
                                td.getMaxRange());
                    }

                    // Vérification de la borne inférieure
                    if (compareValue(td.getMinRange(), td.getRealType().getMinPhysical()) < 0) {
                        errorModel("simple type %s: minrange (%s) is inferior to physical min (%s)", td, td.getMinRange(), td
                                .getRealType().getMinPhysical());
                    }

                    // Vérification de la borne supérieure
                    if (compareValue(td.getMaxRange(), td.getRealType().getMaxPhysical()) > 0) {
                        errorModel("simple type %s: maxrange (%s) is superior to physical max (%s)", td, td.getMaxRange(), td
                                .getRealType().getMaxPhysical());
                    }

                    // Vérification de la borne inférieure avec le parent
                    if (compareValue(td.getMinRange(), td.getBaseType().getMinRange()) < 0) {
                        errorModel("simple type %s: minrange (%s) is inferior to parent type (%s)", td, td.getMinRange(), td
                                .getBaseType().getName());
                    }

                    // Vérification de la borne supérieure avec le parent
                    if (compareValue(td.getMaxRange(), td.getBaseType().getMaxRange()) > 0) {
                        errorModel("simple type %s: maxrange (%s) is superior to parent type (%s)", td, td.getMaxRange(), td
                                .getBaseType().getName());
                    }
                }
            }
			if (td.getIsMap()) {
				if (!td.getKeyType().getIsPredef())
					errorModel("key type of map type '%s' shall be a predefined type", td.getTypeName());
				if (!td.getKeyType().getIsInteger())
					errorModel("key type of map type '%s' shall be an integer type", td.getTypeName());
			}
        }
    }

    void computeIslocal() {
        // type attributs
        for (TypeDefinition t : allTypesList) {
            if (!t.getBaseTypeName().isEmpty()) {
                t.setBaseTypeIsLocal(t.getBaseType().getParent() == t.getParent());
            }
            if (!t.getKeyTypeName().isEmpty()) {
                t.setKeyTypeIsLocal(t.getKeyType().getParent().equals(t.getParent()));
            }
        }
        // Parameters
        for (Component c : allComponentsAndLibraries) {
            for (Operation op : c.getOperations()) {
                for (Parameter p : op.getInParameters()) {
                    if (p != null) {
                        p.setIsLocal(p.getType().getParent().equals(c));
                    }
                }
                for (Parameter p : op.getOutParameters()) {
                    if (p != null) {
                        p.setIsLocal(p.getType().getParent().equals(c));
                    }
                }
            }
            for (Parameter p : c.getAttributes()) {
                if (p != null) {
                    p.setIsLocal(p.getType().getParent().equals(c));
                }
            }
            for (Parameter p : c.getVariables()) {
                if (p != null) {
                    p.setIsLocal(p.getType().getParent().equals(c));
                }
            }
            for (TypeDefinition td : c.getTypes()) {
                for (Parameter p : td.getFields()) {
                    if (p.getType().getParent() != null) {
                        p.setIsLocal(p.getType().getParent().equals(c));
                    }
                }
                for (Parameter p : td.getUnionFields()) {
                    if (p != null) {
                        p.setIsLocal(p.getType().getParent().equals(c));
                    }
                }
                if (td.getDefaultUnionField() != null) {
                    td.getDefaultUnionField().setIsLocal(td.getDefaultUnionField().getType().getParent().equals(c));
                }
            }
        }
    }

    private void computeAllLibraries() {
        for (Component component : context.system.getComponentsIncludingInterface()) {
            Set<Component> dependencies = new LinkedHashSet<Component>();
            addDependencies(component, dependencies);
            dependencies.remove(component);
            component.getAllLibraries().addAll(dependencies);
            gen.debug("Libraries used by %s:", component);
            gen.debug(" - usedLibraries: %s", component.getUsedLibraries());
            gen.debug(" - allLibraries:  %s", component.getAllLibraries());
        }
    }

    private void addDependencies(Component lib, Set<Component> dependencies) {
        if (lib != null && dependencies.add(lib)) {
            // recursively compute its dependencies
            for (Component dep : lib.getUsedLibraries()) {
                addDependencies(dep, dependencies);
            }
        }
    }

    private HashMap<String, LanguageHandler> usedLanguages = new HashMap<>();

    private Dictionnary<TypeDefinition> typesDictionnary = new Dictionnary<>();
    private Dictionnary<ConstantDefinition> constantsDictionnary = new Dictionnary<>();
    
    /**
     * Liste de tous les composants et librairies, y compris l'interface du système, les libs predefs et les stubs.
     */
    protected UniqueList<Component> allComponentsAndLibraries = new UniqueList<Component>();

    /** Liste de tous les types définis dans allComponentsAndLibraries. */
    private UniqueList<TypeDefinition> allTypesList = new UniqueList<TypeDefinition>();

    private UniqueList<Parameter> allParametersList = new UniqueList<Parameter>();
}
