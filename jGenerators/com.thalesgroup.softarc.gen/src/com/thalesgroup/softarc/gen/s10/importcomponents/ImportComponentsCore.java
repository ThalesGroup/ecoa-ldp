/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s10.importcomponents;

import java.io.IOException;
import java.util.Comparator;

import com.thalesgroup.ecoa.model.ComponentType;
import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.ecoa.model.Models;
import com.thalesgroup.softarc.gen.common.languageHandler.CLanguageHandler;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.InstanceAttribute;
import com.thalesgroup.softarc.sf.InstancePInfo;
import com.thalesgroup.softarc.sf.PInfo;
import com.thalesgroup.softarc.sf.impl.QAssembly;
import com.thalesgroup.softarc.sf.impl.QExternInterface;
import com.thalesgroup.softarc.sf.impl.QInstance;
import com.thalesgroup.softarc.sf.impl.QInstanceAttribute;
import com.thalesgroup.softarc.sf.impl.QInstancePInfo;
import com.thalesgroup.softarc.sf.impl.QSystem;
import com.thalesgroup.softarc.sf.impl.QVariable;

import technology.ecoa.model.assembly.ASIdentifiedMemberValue;
import technology.ecoa.model.assembly.ASInstance;
import technology.ecoa.model.assembly.ASMemberValue;
import technology.ecoa.model.componenttype.CTComponentType;

public class ImportComponentsCore extends ImportComponent {

    Language interfaceLanguage = Language.C;

    @Override
    public void execute() throws IOException {

        selectLanguages();

        importAssembly();
        addStubImplementations();
        sortComponents();
    }
    
    public void selectLanguages() {
        // Select languages supported by this generator:
        CLanguageHandler.init(); // default language: must be first
    }

    void sortComponents() {
        
        context.system.getComponents().sort(new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                if (o1.getIsLibrary() && !o2.getIsLibrary())
                    return -1;
                if (o2.getIsLibrary() && !o1.getIsLibrary())
                    return 1;
                return 0;
            }
        });
    }

    void importAssembly() throws IOException {
        // TR-SARC-GEN-REQ-098
        QSystem sys = new QSystem();
        context.system = sys;

        // TR-SARC-GEN-REQ-099
        for (ASInstance i : context.ASFILE.getInstance()) {
            importComponent(i.getComponentType(), i.getImplementation());
        }

        importSystemInterface(interfaceLanguage);

        // TR-SARC-GEN-REQ-118_119 done in GenTechnicalAssembly

        // TR-SARC-GEN-REQ-120
        Assembly assembly = new QAssembly();
        sys.setAssembly(assembly);

        for (ASInstance ins : context.ASFILE.getInstance()) {
            // TR-SARC-GEN-REQ-121
            Instance instance = new QInstance();
            instance.setName(ins.getName());

            Component c = findRegisteredComponent("component", ins.getComponentType(), ins.getImplementation());
            if (c == null) {
                errorInternal("component %s.%s not found", ins.getComponentType(), ins.getImplementation());
            }
            instance.setType(c);

            importInstanceAttributes(instance, ins);
            importInstancePInfos(instance, ins);
            checkIfEachInstanceFromComponentWithPinfoHasPinfoDefined(instance);
            importInstanceVariables(instance, ins);
            assembly.getInstances().add(instance);
        }

    }

    void importSystemInterface(Language language) throws IOException {
        // TR-SARC-GEN-REQ-117
        String ctName = context.ASFILE.getComponentType();
        if (ctName != null) {
            QExternInterface component = new QExternInterface();
            component.setLanguage(language.name());
            component.setApiVariant("SOFTARC_" + language);
            component.setTypeName(ctName);
            component.setFullName(component.getTypeName() + "/" + language);
            component.setXmlID("component:" + component.getFullName());

            CTComponentType ct = Models._componenttype.load(context.workspace.getComponentType(ctName, false), gen);

            importUsedLibraries(component, new ComponentType(ctName, ct));
            importOperations(ct, component);

            context.system.setInterface(component);
        }
    }

    void importInstanceAttributes(Instance instance, ASInstance ins) {
        for (ASMemberValue attr : ins.getPropertyValue()) {
            // TR-SARC-GEN-REQ-122
            InstanceAttribute attribute = new QInstanceAttribute();
            attribute.setName(attr.getName());
            attribute.setValue(attr.getValue());

            instance.getAttributes().add(attribute);
        }
    }

    void importInstancePInfos(Instance instance, ASInstance ins) {
        for (ASMemberValue pinfo_value : ins.getPinfoValue()) {
            InstancePInfo pinfo = new QInstancePInfo();
            pinfo.setName(pinfo_value.getName());
            pinfo.setParent(instance);
            pinfo.setPath(pinfo_value.getValue());
            pinfo.setIdentifier(instance.getName() + "_" + pinfo_value.getName());
            pinfo.setId(-1);

            instance.getPinfos().add(pinfo);
        }
    }
    
    private void checkIfEachInstanceFromComponentWithPinfoHasPinfoDefined(Instance instance) {
    	// For each pinfo defined in component, we check if the instance has an InstancePinfo for each.
    	Component componentType = instance.getType();
    	for(PInfo rom : componentType.getPinfos()) {
    		if (!instance.getPinfos().stream().anyMatch(instancePinfo -> instancePinfo.getName().equals(rom.getName()))) {
    			errorModel("instance %s in '%s' shall define a 'pinfoValue' attribute for PINFO %s", instance.getName(),
                        context.workspace.getAssemblyFile(context.workspace.currentDeploymentName), rom.getName());
    		}
    	}
    }

    void importInstanceVariables(Instance instance, ASInstance ins) throws IOException {
        for (ASIdentifiedMemberValue attr : ins.getVariableInit()) {
            QVariable v = new QVariable();
            v.setId(attr.getId());
            v.setIntialValue(attr.getValue());
            v.setName(attr.getName());
            instance.getVariables().add(v);
        }
    }

    // private String antiCollision(String newId, Collection<String> existingIds) {
    // if (!existingIds.contains(newId))
    // return newId;
    // for (int index = 1; true; index++) {
    // String result = newId + Integer.toString(index);
    // if (!existingIds.contains(result))
    // return result;
    // }
    // }
}
