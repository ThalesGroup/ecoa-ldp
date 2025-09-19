/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

public interface ValueVisitor {

    // terminals

    void visitBoolean(boolean value) throws Exception;

    void visitInteger(long value) throws Exception;

    void visitFloat(float value) throws Exception;
    
    void visitDouble(double value) throws Exception;

    void visitEnum(long valnum, String symbol) throws Exception;

    void visitString(String value, int maxLength) throws Exception;

    // structured values

    /**
     * For an array or a fixedarray:
     * <li>first call beginArray(size)
     * <li>before each element, call beforeElement()
     * <li>visit the element
     * <li>after all elements have been visited, call endArray()
     */
    void beginArray(int size, boolean isFixedSize) throws Exception;

    void beforeElement() throws Exception;

    void endArray() throws Exception;

    /**
     * For a record or variantrecord:
     * <li>first call beginRecord()
     * <li>before each field, call beforeField(name)
     * <li>visit the field's value
     * <li>after all fields have been visited, call endArray()
     * 
     * For a variantrecord, the selector is the first field, with empty name; a union field is considered as a normal field.
     */
    void beginRecord() throws Exception;

    void beforeField(String name) throws Exception;

    void endRecord() throws Exception;

}