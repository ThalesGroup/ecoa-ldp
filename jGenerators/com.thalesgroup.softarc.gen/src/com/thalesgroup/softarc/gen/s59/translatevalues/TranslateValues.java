/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s59.translatevalues;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.gen.common.AbstractPass;
import java.io.IOException;

import java.util.Collection;

import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.DataVersion;
import com.thalesgroup.softarc.sf.Executable;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.InstanceAttribute;
import com.thalesgroup.softarc.sf.Platform;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.types.AbstractValueReader.SyntaxError;
import com.thalesgroup.softarc.types.Value;
import com.thalesgroup.softarc.types.ValueReader;
import com.thalesgroup.softarc.types.ValueWriter;
import com.thalesgroup.softarc.types.ValueWriters;

/**
 * Cette passe transforme les valeurs des types SOFTARC depuis la syntaxe SOFTARC vers la syntaxe propre au langage cible.
 */
public class TranslateValues extends AbstractPass {

    final ValueWriters writers = new ValueWriters();
    final ValueReader reader = new ValueReader(true);

    @Override
    public void execute() throws IOException {
    	
    	ValueWriter cValueWriter = writers.getValueWriter(Language.C.name());

        // traduction de InstanceAttribute.value vers le langage cible
        for (Executable exe : context.system.getMapping().getExecutables()) {
            for (Instance i : exe.getInstances()) {
                ValueWriter writer = writers.getValueWriter(i.getType().getLanguage());
                for (InstanceAttribute a : i.getAttributes()) {
                    try {
                        // info("attribute %s of type %s with value: %s", a.getName(), a.getType(), a.getValue());
                        Value value = reader.read(a.getValue(), a.getType());
                        if (value != null) {
                            String stringValue = writer.write(value);

                            /* create a multiline string to initialize a python variantrecord/record */
                            if (i.getType().getLanguage().equals("PYTHON")) {
                                if (a.getType().getIsVariantRecord() || a.getType().getIsRecord()) {                                
                                    String init = "";
                                    for(String line : stringValue.split("\n")) {
                                        init += "self." + a.getName() + '.' + line + '\n';
                                    }
                                    stringValue = init;
                                }
                            }

                            if (stringValue != null) {
                            	a.setValue(stringValue);
                            	a.setCValue(cValueWriter.write(value));
                            }
                                
                        }
                        if (a.getValue().isEmpty()) {
                            errorModel("cannot translate to %s the value of attribute '%s' of type %s in instance %s: '%s'",
                                    i.getType().getLanguage(), a.getName(), a.getType().getXmlID(), i.getName(), a.getValue());
                        }
                        // info("translated to: %s", a.getValue());
                    } catch (Exception e) {
                        errorModel("syntax error in value of attribute '%s' of type %s in instance %s: %s", a.getName(),
                                a.getType().getXmlID(), i.getName(), e.getMessage());
                    }
                }
            }
        }

        for (Platform platform : context.system.getMapping().getPlatforms()) {
            for (DataVersion d : platform.getDataVersions()) {
                DataLink dataLink = d.getDataLink();
                if (dataLink != null && !dataLink.getDefaultValue().isEmpty()) {

                    TypeDefinition type = findDataLinkType(dataLink.getWriters());

                    if (type == null) {
                        type = findDataLinkType(dataLink.getReaders());

                        if (type == null) {
                            errorInternal("cannot determine the type of datalink %s", d.getId());
                            return; // to avoid a warning
                        }
                    }
                    d.setType(type);

                    if (!type.getIsPredef()) {
                        if (!platform.getDispatcher().getDispatcherRequiredComponentTypes()
                                .contains(type.getParent().getCComponent()))
                            platform.getDispatcher().getDispatcherRequiredComponentTypes().add(type.getParent().getCComponent());
                    }

                    // Convert default value from SOFTARC syntax to target language syntax,
                    // and raise syntax errors, if any.
                    String string = dataLink.getDefaultValue();
                    try {
                        Value value = reader.read(string, dataLink.getDefaultValueType());

                        d.setDefaultValue(cValueWriter.write(value));
                        d.setHasDefaultValue(true);
                        platform.setHasDefaultValues(true);

                    } catch (Exception e) {
                        errorModel("syntax error in value '%s' for default value of datalink %d, of type %s", string, d.getId(),
                                dataLink.getDefaultValueType());
                    }
                }
            }
        }
    }

    private TypeDefinition findDataLinkType(Collection<DataLinkElement> elements) {
        for (DataLinkElement p : elements) {
            TypeDefinition type = p.getPort().getData().getType();
            if (type != null) {
                return type.getCType();
            }
        }
        return null;
    }

}
