/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common.languageHandler;

import com.thalesgroup.ecoa.model.Language;

public class CppLanguageHandler extends LanguageHandler {

    public static void init() {
        LanguagesHandler.register("SOFTARC_CPP", new CppLanguageHandler(Language.CPP, "::", "::", "sarc"));
    }
    
    CppLanguageHandler(Language lang, String packageAndNameSeparator, String enumValueSeparator, String predefPackageName) {
        super(lang, packageAndNameSeparator, enumValueSeparator, predefPackageName, keywords, "hpp", cppExtensions);
    }

    @Override
    public String getBooleanValue(boolean isTrue) {
        if (isTrue)
            return "true";
        else
            return "false";
    }
    
    private static final String cppExtensions[] = { "cpp", "C", "cc", "cp", "cxx", "CPP", "c++" };

    final static String keywords[] = { "auto", "double", "int", "struct", "break", "else", "long", "switch", "case", "enum",
            "register", "typedef", "char", "extern", "return", "union", "const", "float", "short", "unsigned", "continue", "for",
            "signed", "void", "default", "goto", "sizeof", "volatile", "do", "if", "static", "while", "asm", "dynamic_cast",
            "namespace", "reinterpret_cast", "bool", "explicit", "new", "static_cast", "catchfalse", "operator", "template",
            "class", "friend", "private", "this", "const_cast", "inline", "public", "throw", "delete", "mutable", "protected",
            "true", "try", "typeid", "typename", "using", "virtual", "wchar_t" };
}
