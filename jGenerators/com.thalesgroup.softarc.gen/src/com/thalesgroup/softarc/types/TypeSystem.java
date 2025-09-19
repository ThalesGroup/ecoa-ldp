/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.impl.QEnumValue;
import com.thalesgroup.softarc.sf.impl.QParameter;
import com.thalesgroup.softarc.sf.impl.QTypeDefinition;
import com.thalesgroup.softarc.sf.impl.QVariantField;

/**
 * Cette classe représente de façon autonome un système de types SOFTARC.
 * 
 * Elle est destinée aux cas où on a besoin de manipuler des types SOFTARC, en dehors de GenSoftarc (donc sans avoir à disposition
 * une instanciation complète du formalisme). Elle permet de recréer, à partir d'une description textuelle, les types SOFTARC, en
 * utilisant les classes du formalisme (classe QTypeDefinition et dépendances). Typiquemement, cette description figure à la fin
 * du fichier Mapping.xml.
 */
public class TypeSystem {

    protected Map<String, TypeDefinition> allTypes = new LinkedHashMap<>();
    private static Map<TypeDefinition, EnumWrapper> allEnumWrappers = new LinkedHashMap<>();

    public TypeSystem() {
        registerPredef("boolean8");
        registerPredef("int16");
        registerPredef("uint16");
        registerPredef("int32");
        registerPredef("uint32");
        registerPredef("int64");
        registerPredef("int8");
        registerPredef("char8");
        registerPredef("uint8");
        registerPredef("float32");
        registerPredef("double64");
    }

    private void register(String fullname, TypeDefinition type) {
        type.setTypeName(fullname);
        type.setName(stripPackage(fullname));
        type.setXmlID(fullname);
        type.setQName(fullname); // 'qName' est utilisé par ValueWriterJava pour générer les noms de classes
        allTypes.put(fullname, type);
    }

    private String stripPackage(String name) {
        int pos = name.lastIndexOf('.');
        if (pos == -1)
            return name;
        else
            return name.substring(pos + 1);
    }

    private void registerPredef(String name) {
        QTypeDefinition type = new QTypeDefinition();
        type.isPredef = true;
        type.setIsNumeric(true);
        type.setIsScalar(true);
        type.setIsInteger(!name.equals("boolean8") && !name.equals("float32") && !name.equals("double64"));
        type.setRealType(type);
        register(name, type);
    }

    public TypeDefinition createTypeFromDescription(String description) throws UndefinedTypeException, IllegalArgumentException {
        LinkedList<String> words = new LinkedList<String>(Arrays.asList(description.split(",")));
        QTypeDefinition type = new QTypeDefinition();

        String kind = words.remove();
        if (kind.equals("record")) {
            type.isRecord = true;
            while (!words.isEmpty()) {
                QParameter f = new QParameter();
                f.name = words.remove();
                f.type = getType(words.remove());
                type.getFields().add(f);
            }
        } else if (kind.equals("variantrecord")) {
            type.isVariantRecord = true;
            // variantrecord,<selectname>,<selecttype>,<nb_fields>[,<name>,<type>]*,<nb_unionfields>[,<when>,<name>,<type>]*[,defaultname,defaulttype]
            type.selectName = words.remove();
            type.baseType = getType(words.remove());
            int nbFields = Integer.parseInt(words.remove());
            for (int i = 0; i < nbFields; i++) {
                QParameter f = new QParameter();
                f.name = words.remove();
                f.type = getType(words.remove());
                type.getFields().add(f);
            }
            int nbUnionFields = Integer.parseInt(words.remove());
            for (int i = 0; i < nbUnionFields; i++) {
                QVariantField f = new QVariantField();
                f.when = words.remove();
                f.name = words.remove();
                f.type = getType(words.remove());
                type.getUnionFields().add(f);
            }
            if (!words.isEmpty()) {
                QParameter p = new QParameter();
                p.name = words.remove();
                p.type = getType(words.remove());
                type.setDefaultUnionField(p);
            }
        } else if (kind.equals("simple")) {
            type.isSimple = true;
            type.baseType = getType(words.remove());
        } else if (kind.equals("enum")) {
            type.isEnum = true;
            type.baseType = getType(words.remove());
            while (!words.isEmpty()) {
                QEnumValue symbol = new QEnumValue();
                symbol.name = words.remove();
                symbol.valnum = Long.parseLong(words.remove());
                type.getEnumValues().add(symbol);
            }
        } else if (kind.equals("fixedarray")) {
            type.isFixedArray = true;
            type.arraySize = Integer.parseInt(words.remove());
            type.baseType = getType(words.remove());
        } else if (kind.equals("array") || kind.equals("list")) {
            type.isArray = true;
            type.arraySize = Integer.parseInt(words.remove());
            type.baseType = getType(words.remove());
        } else if (kind.equals("map")) {
            type.isMap = true;
            type.arraySize = Integer.parseInt(words.remove());
            type.keyType = getType(words.remove());
            type.baseType = getType(words.remove());
        } else if (kind.equals("string")) {
            type.isString = true;
            type.length = Integer.parseInt(words.remove());
        } else {
            throw new IllegalArgumentException("invalid type description: " + description);
        }
        resolveRealType(type);
        return type;
    }

    void resolveRealType(TypeDefinition td) {
        TypeDefinition rt = td.getBaseType();

        if (rt != null) {
            if (rt.getIsSimple() || rt.getIsEnum()) {
                rt = rt.getRealType();
            }
        } else {
            rt = td;
        }
        td.setRealType(rt);
    }

    public synchronized void defineType(String name, String description) throws UndefinedTypeException, IllegalArgumentException {
        if (description != null && !allTypes.containsKey(name)) {
            register(name, createTypeFromDescription(description));
        }
    }

    public synchronized TypeDefinition getType(String name) throws UndefinedTypeException {
        TypeDefinition t = allTypes.get(name);
        if (t == null)
            throw new UndefinedTypeException(name);
        return t;
    }

    public static synchronized EnumWrapper getEnumWrapper(TypeDefinition t) {
        EnumWrapper ew = allEnumWrappers.get(t);
        if (ew == null) {
            ew = new EnumWrapper(t);
            allEnumWrappers.put(t, ew);
        }
        return ew;
    }

}
