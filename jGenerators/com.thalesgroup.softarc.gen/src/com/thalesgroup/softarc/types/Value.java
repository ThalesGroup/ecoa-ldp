/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

/**
 * Representation of actual values of Softarc types. Used for component attributes, and default values of data, but also useful
 * for debug & observation, etc.
 * 
 * This class and it subclasses have minimal dependencies: they only depend on interface "TypeDefinition".
 * 
 */
public abstract class Value {
    public TypeDefinition type;

    public Value[] getFieldsValues() {
        if (this instanceof RecordValue) {
            return ((RecordValue) this).fields;
        }
        return null;
    }

    public long toIntegerKey() {
        throw new Error();
    }
}
