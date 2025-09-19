/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.util.HashSet;

/**
 * Cette classe permet de "valider" des noms au regard des contraintes suivantes:
 * 
 * <li>Chaque nom doit respecter une taille maximale. La partie qui dépasse est tronquée.
 * 
 * <li>Chaque nom doit être unique (différent des précédents). En cas de conflit, un suffixe numérique est ajouté pour le
 * différencier des précédents.
 */
public class NameValidator {

    private HashSet<String> usedNames = new HashSet<String>();
    private int maxSize;

    /**
     * @param maxSize
     *            maximum size for anly name (including the added suffix, if any)
     */
    public NameValidator(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @param basename
     *            the original, suggested name, to be used without modifications if possible.
     * @return the validated name, derived from basename and respecting the naming constraints.
     */
    public String validate(String basename) {
        String alteration = "";
        int i = 1;
        for (;;) {
            int prefixMax = maxSize - alteration.length();
            String prefix = basename;
            if (prefix.length() > prefixMax)
                prefix = prefix.substring(0, prefixMax);
            String name = prefix + alteration;
            if (usedNames.add(name) == true) {
                return name;
            }
            alteration = Integer.toString(i);
            i++;
        }
    }

}
