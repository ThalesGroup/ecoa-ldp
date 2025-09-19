/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common.languageHandler;

import com.thalesgroup.ecoa.model.Language;

public class CLanguageHandler extends LanguageHandler {

    public static void init() {
        LanguagesHandler.register("SOFTARC_C", new CLanguageHandler("_", "_", "SARC"));
        LanguagesHandler.register("ECOA_C", new CLanguageHandler("__", "_", "ECOA"));
    }

    CLanguageHandler(String packageAndNameSeparator, String enumValueSeparator, String predefPackageName) {
        super(Language.C, packageAndNameSeparator, enumValueSeparator, predefPackageName, cKeywords, "h", new String[] { "c" });
    }

    final static String cKeywords[] = { "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else",
            "enum", "extern", "float", "for", "goto", "if", "int", "long", "register", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while" };
}
