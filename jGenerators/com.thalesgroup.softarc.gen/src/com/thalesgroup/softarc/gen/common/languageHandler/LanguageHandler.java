/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common.languageHandler;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.thalesgroup.ecoa.model.Language;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.ConstantDefinition;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.impl.QComponent;
import com.thalesgroup.softarc.sf.impl.QTypeDefinition;

import technology.ecoa.model.datatype.EPredef;

public abstract class LanguageHandler {

    public String getQName(TypeDefinition type) {
    	return type.getParent().getPackage() + packageAndNameSeparator + type.getName();
    }
    
    public String getQName(String name) {
        return name;
    }

    public String getQName(ConstantDefinition cst, Component fromComponent) {
        return fromComponent.getPackage() + packageAndNameSeparator + cst.getName();
    }

    public String getBooleanValue(boolean isTrue) {
        if (isTrue)
            return predefTypesLib.getPackage() + this.packageAndNameSeparator + "TRUE";
        else
            return predefTypesLib.getPackage() + this.packageAndNameSeparator + "FALSE";
    }

    public String getQValue(TypeDefinition type, String value) {
        if (type.getIsEnum())
            return getQName(type) + enumValueSeparator + value;
        else
            return value;
    }

    public String getDefaultValue(TypeDefinition type) {
        if (type.getIsPredef()) {
            return "0";
        } else if (type.getIsEnum()) {
            return type.getQName() + enumValueSeparator + type.getEnumValues().iterator().next().getName();
        } else if (type.getIsSimple()) {
            BigDecimal _proposal = new BigDecimal(getDefaultValue(type.getBaseType()));

            if (type.getMinRange() != null) {
                BigDecimal _minRange = new BigDecimal(type.getMinRange());

                if (_proposal.compareTo(_minRange) < 0) {
                    _proposal = _minRange;
                }
            }
            if (type.getMaxRange() != null) {
                BigDecimal _maxRange = new BigDecimal(type.getMaxRange());

                if (_proposal.compareTo(_maxRange) > 0) {
                    _proposal = _maxRange;
                }
            }

            return _proposal.toString();
        } else if (type.getIsString() || type.getIsChar8Array()) {
            return "\"\"";
        } else {
            return "";
        }
    }

    public boolean isSwitchable(EPredef t) {
        return false;
    }

    public static final String predefLibName = "";

    public HashMap<EPredef, TypeDefinition> predefTypes = new HashMap<>();
    public Component predefTypesLib;

    final public Language language;

    public String packageAndNameSeparator;
    public String enumValueSeparator;
    public String headerextension;
    public String[] sourceextension;

    HashSet<String> keywordsSet = new HashSet<String>();

    boolean isKeyword(String s) {
        return keywordsSet.contains(s);
    }

    public String avoidKeywords(String s) {
        while (isKeyword(s))
            s = s + "_";
        return s;
    }

    public void computeStuff(TypeDefinition type) {
    }

    LanguageHandler(Language lang, String packageAndNameSeparator, String enumValueSeparator, String predefPackageName,
            String[] keywords, String headerextension, String[] sourceextension) {

        this.packageAndNameSeparator = packageAndNameSeparator;
        this.enumValueSeparator = enumValueSeparator;
        this.language = lang;
        this.keywordsSet = new HashSet<String>(Arrays.asList(keywords));
        this.headerextension = headerextension;
        this.sourceextension = sourceextension;

        predefTypesLib = new QComponent();
        predefTypesLib.setIsLibrary(true);
        predefTypesLib.setIsPredefLib(true);
        predefTypesLib.setLanguage(lang.name());
        predefTypesLib.setTypeName(predefLibName);
        predefTypesLib.setImplName(lang.name());
        predefTypesLib.setPackage(predefPackageName);
        predefTypesLib.setFullName(predefTypesLib.getTypeName() + '/' + predefTypesLib.getImplName());
        predefTypesLib.setXmlID("predeflib:" + predefTypesLib.getFullName());

        for (EPredef t : EPredef.values()) {
            // TODO "uint64" défini dans le métamodèle ECOA mais non supporté par SOFTARC.
            // Bypass à retirer avec la FR 3773
            if (t == EPredef.UINT_64)
                continue;

            QTypeDefinition td = new QTypeDefinition();
            definePredefTypeAttributes(td, t);
            switch (lang) {
            case C:
            case CPP:
                switch (t) {
                case BOOLEAN_8:
                    td.setMaxPhysical("255");
                    break;
                default:
                    break;
                }
                break;
            default:
                break;
            }
            predefTypesLib.getTypes().add(td);
            predefTypes.put(t, td);
        }
    }

    private final void definePredefTypeAttributes(QTypeDefinition td, EPredef t) {
        td.setName(t.value());
        td.setTypeName(t.value());
        td.setParent(predefTypesLib);
        td.setIsPredef(true);

        td.setTypeName(td.getName());
        td.setXmlID('.' + td.getName() + "." + predefTypesLib.getLanguage());
        BigDecimal valMin = null;
        BigDecimal valMax = null;
        switch (t) {
        case BOOLEAN_8:
            valMin = BigDecimal.ZERO;
            valMax = BigDecimal.ONE;
            break;
        case UINT_8:
        case CHAR_8:
            valMin = BigDecimal.ZERO;
            valMax = new BigDecimal("255");
            break;
        case UINT_16:
            valMin = BigDecimal.ZERO;
            valMax = new BigDecimal("65535");
            break;
        case UINT_32:
            valMin = BigDecimal.ZERO;
            valMax = new BigDecimal("4294967295");
            break;
        case UINT_64:
            valMin = BigDecimal.ZERO;
            valMax = null; // TODO FR 3773
            break;
        case INT_8:
            valMin = new BigDecimal("-128");
            valMax = new BigDecimal("127");
            break;
        case INT_16:
            valMin = new BigDecimal("-32768");
            valMax = new BigDecimal("32767");
            break;
        case INT_32:
            valMin = new BigDecimal("-2147483648");
            valMax = new BigDecimal("2147483647");
            break;
        case INT_64:
            valMin = new BigDecimal("-9223372036854775808");
            valMax = new BigDecimal("9223372036854775807");
            break;
        case FLOAT_32:
            valMin = new BigDecimal("-3.40282346e38");
            valMax = new BigDecimal("3.40282346e38");
            break;
        case DOUBLE_64:
            valMin = new BigDecimal("-1.7976931348623157e308");
            valMax = new BigDecimal("1.7976931348623157e308");
            break;
        default:
            throw new Error("Unsupported type: " + t.name());
        }
        td.setMinRange(valMin.toString());
        td.setMaxRange(valMax.toString());
        // default, correct for most cases but not all:
        td.setMinPhysical(valMin.toString());
        td.setMaxPhysical(valMax.toString());

        td.setIsSwitchable(isSwitchable(t));
    }
}