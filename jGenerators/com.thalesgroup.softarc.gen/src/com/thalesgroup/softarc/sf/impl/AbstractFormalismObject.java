/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.sf.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AbstractFormalismObject {

    // noms spéciaux pour les champs:
    public static final String XML_ID = "xmlID";
    public static final String NAME = "name";
    public static final String ID = "id";

    /**
     * Initialise tous les champs d'un objet quelconque du formalisme.
     * <li>String => ""
     * <li>List => new ArrayList<>()
     * <li>boolean => false
     * <li>long => 0
     */
    void initAttributes(Class<?> c) {

        if (c != null) {
            initAttributes(c.getSuperclass());

            for (Field f : c.getDeclaredFields())
                try {
                    if ((f.getModifiers() & Modifier.FINAL) == 0) {
                        if (f.getType().isAssignableFrom(String.class))
                            if (f.getName().equals(XML_ID))
                                f.set(this, "UNDEFINED");
                            else
                                f.set(this, "");
                        else if (isMultiple(f))
                            if (getSimpleType(f).isInterface()) {
                                // for relations (to other formalism objects): do not allow duplicates
                                f.set(this, new DeduplicatingList<>());
                            } else {
                                // for attributes: allow duplicates
                                f.set(this, new ArrayList<>());
                            }
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    public static boolean isMultiple(Field f) {
        return f.getType().isAssignableFrom(List.class);
    }

    static boolean isReference(Field f) {
        return ((f.getModifiers() & Modifier.PROTECTED) == 0);
    }

    static Class<?> getSimpleType(Field f) {
        Type genericType = f.getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType)
            return (Class<?>) ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments()[0];
        else
            return f.getType();
    }

    private static List<Field> getObjectFields(Class<?> startClass) {

        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null) {
            List<Field> parentClassFields = getObjectFields(parentClass);
            for (Field c : startClass.getDeclaredFields())
                if ((c.getModifiers() & Modifier.STATIC) == 0)
                    parentClassFields.add(c);
            return parentClassFields;
        }
        return new ArrayList<Field>();
    }

    public List<Field> getObjectFields() {
        return getObjectFields(this.getClass());
    }

    AbstractFormalismObject() {
        initAttributes(this.getClass());
    }

    @Override
    public String toString() {
        try {
            Field f = this.getClass().getField(XML_ID);
            if (f != null)
                return f.get(this).toString();
        } catch (Exception e) {
        }
        try {
            Field f = this.getClass().getField(NAME);
            if (f != null)
                return f.get(this).toString();
        } catch (Exception e) {
        }
        try {
            Field f = this.getClass().getField(ID);
            if (f != null)
                return f.get(this).toString();
        } catch (Exception e) {
        }
        return super.toString();
    }

    static boolean isDefaultValue(Object obj) {
        if (obj == null)
            return true;
        if (obj instanceof Boolean && (Boolean) obj == false)
            return true;
        if (obj instanceof String && ((String) obj).isEmpty())
            return true;
        if (obj instanceof Long && (Long) obj == 0)
            return true;
        if (obj instanceof List && ((List<?>) obj).isEmpty())
            return true;
        return false;
    }

}
