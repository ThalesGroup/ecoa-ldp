/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common.languageHandler;

import java.util.HashMap;

import com.thalesgroup.softarc.tools.InconsistentModelError;

public final class LanguagesHandler {

    private final static HashMap<String, LanguageHandler> supportedLanguages = new HashMap<>();

    public static String defaultBinding = "SOFTARC_C";

    public static LanguageHandler get(String apiVariant) {

        if (apiVariant == null || apiVariant.isBlank())
            return referenceLanguage;
        
        LanguageHandler result = supportedLanguages.get(apiVariant);
        if (result == null)
            throw new InconsistentModelError(apiVariant + " is not a supported language binding");
        return result;
    }
    
    public static String getDefaultApiTypeForLanguage(String language) {

        return "SOFTARC_" + language;
    }
    
    public static LanguageHandler getSoftarc(String language) {

        return get(getDefaultApiTypeForLanguage(language));
    }
    
    /**
     * The first registered handler is used as the reference language (typically: C)
     */
    private static LanguageHandler referenceLanguage = null;
    
    static void register(String name, LanguageHandler h) {
        if (referenceLanguage == null) {
            referenceLanguage = h;
        }
        supportedLanguages.putIfAbsent(name, h);

        h.predefTypesLib.setApiVariant(name);
        // Après la création de tous les handlers, on associe chaque lib predef à la lib predef C.
        h.predefTypesLib.setCComponent(referenceLanguage.predefTypesLib);
    }
    
    private LanguagesHandler() {
    }

}
