/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s20.xref;

import java.util.HashMap;
import java.util.regex.Pattern;

import com.thalesgroup.softarc.gen.common.languageHandler.LanguageHandler;
import com.thalesgroup.softarc.gen.common.languageHandler.LanguagesHandler;
import com.thalesgroup.softarc.sf.Component;

public class Dictionnary<T> {

    /**
     * Dictionnaire contenant l'ensemble des types et constantes de toutes les libs.
     * 
     * Clé = "library.name.language.apiVariant"
     * 
     * Pour les types predef, library="", sinon library est le nom de la lib.
     */
    private final HashMap<String, T> map = new HashMap<String, T>();

    private final Pattern patternDot = Pattern.compile(".", Pattern.LITERAL);

    private String key(String name, String library, String apiVariant) {
        // Par défaut, on utilise les implémentations en C, qui existent toujours
        if (apiVariant.isEmpty())
            apiVariant = LanguagesHandler.defaultBinding;
        
        return library + '.' + name + '.' + apiVariant;
    }

    /**
     * @param o object to store in the dictionnary
     * @param name simple name of the object
     * @param library name of the library where the object is defined
     * @param language language for which the object is defined
     */
    void put(T o, String name, Component c) {

        final String key = key(name, c.getTypeName(), c.getApiVariant());

        T existingType = map.put(key, o);
        if (existingType != null) {
            throw new Error("duplicate definition of type definition " + key);
        }
        // info("Adding type %s", key);
    }

    /** @return un TypeDefinition ou ConstantDefinition, ou null si pas trouvé */
    T lookup(String reference, Component c) {
        T result = null;
        String library = c.getTypeName();

        // on découpe selon les points (2 mots maxi)
        String[] words = patternDot.split(reference, -1);

        if (words.length == 2) { // 2 mots: type + le nom court

            library = words[0];
            reference = words[1];
            result = map.get(key(reference, library, c.getApiVariant()));
            if (result != null)
                return result;

        } else if (words.length == 1) { // un seul mot

            // 1. définition locale avec le langage voulu
            result = map.get(key(reference, library, c.getApiVariant()));
            if (result != null)
                return result;

            // 2. predef
            result = map.get(key(reference, LanguageHandler.predefLibName, c.getApiVariant()));
        }

        return result;
    }
}
