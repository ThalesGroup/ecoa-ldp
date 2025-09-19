/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import com.thalesgroup.softarc.sf.TypeDefinition;

public abstract class AbstractValueReader {

    private boolean autoCheckBounds;
    Value value;

    public AbstractValueReader(boolean autoCheckBounds) {
        this.autoCheckBounds = autoCheckBounds;
    }

    public void setAutoCheckBounds(boolean autoCheckBounds) {
        this.autoCheckBounds = autoCheckBounds;
    }

    protected void error(String fmt, Object... args) throws SyntaxError {
        throw new SyntaxError(String.format(fmt, args));
    }

    @SuppressWarnings("serial")
    public class SyntaxError extends Exception {
        public SyntaxError(String msg) {
            super(msg);
        }
    }

    protected Value read(TypeDefinition type) throws SyntaxError {
        if (type.getIsPredef()) {
            switch (type.getName()) {
            case "float32":
                value = readFloat(type);
                break;
            case "double64":
                value = readDouble(type);
                break;
            case "boolean8":
                value = readBoolean(type);
                break;
            case "char8":
                value = readCharacter(type);
                break;
            default:
                value = readInteger(type);
                break;
            }
            if (autoCheckBounds)
                checkBounds(type);
        }

        else if (type.getIsSimple()) {
            value = read(type.getBaseType());
            value.type = type; // force attached type to be the simple type, and not the real type
            if (autoCheckBounds)
                checkBounds(type);
        }

        else if (type.getIsEnum()) {
            value = readEnum(type);
        }

        else if (type.getIsArray()) {
            value = readArray(type, 0, (int) type.getArraySize());
        }

        else if (type.getIsFixedArray()) {
            int size = (int) type.getArraySize();
            value = readArray(type, size, size);
        }

        else if (type.getIsRecord()) {
            value = readRecord(type);
        }

        else if (type.getIsVariantRecord()) {
            value = readVariant(type);
        }

        else if (type.getIsString()) {
            value = readString(type);
        }

        else {
            error("Cannot read type %s", type.getName());
        }
        return value;
    }

    protected void checkBounds(TypeDefinition type) throws SyntaxError {
        if (value instanceof DoubleValue) {
            double x = ((DoubleValue) value).v;
            if (!type.getMinRange().isEmpty() && x < Double.valueOf(type.getMinRange()))
                error("Value %s is too small for type %s (min : %s) %s", x, type.getName(), type.getMinRange(), Double.valueOf(type.getMinRange()));
            if (!type.getMaxRange().isEmpty() && x > Double.valueOf(type.getMaxRange()))
                error("Value %s is too big for type %s (max : %s) %s", x, type.getName(), type.getMaxRange(), Double.valueOf(type.getMaxRange()));
        } 
        else if (value instanceof FloatValue) {
            float x = ((FloatValue) value).v;
            if (!type.getMinRange().isEmpty() && x < Float.valueOf(type.getMinRange()))
                error("Value %s is too small for type %s (min : %s) %s", x, type.getName(), type.getMinRange(), Float.valueOf(type.getMinRange()));
            if (!type.getMaxRange().isEmpty() && x > Float.valueOf(type.getMaxRange()))
                error("Value %s is too big for type %s (max : %s) %s", x, type.getName(), type.getMaxRange(), Float.valueOf(type.getMaxRange()));
        } else if (value instanceof IntegerValue) {
            long x = ((IntegerValue) value).v;
            if (!type.getMinRange().isEmpty() && x < Long.valueOf(type.getMinRange()))
                error("Value %s is too small for type %s (min : %s)", x, type.getName(), type.getMinRange());
            if (!type.getMaxRange().isEmpty() && x > Long.valueOf(type.getMaxRange()))
                error("Value %s is too big for type %s (max : %s)", x, type.getName(), type.getMaxRange());
        }
    }

    protected abstract Value readCharacter(TypeDefinition type) throws SyntaxError;

    protected abstract Value readDouble(TypeDefinition type) throws SyntaxError;

    protected abstract Value readFloat(TypeDefinition type) throws SyntaxError;

    protected abstract Value readInteger(TypeDefinition type) throws SyntaxError;

    protected abstract Value readBoolean(TypeDefinition type) throws SyntaxError;

    protected abstract Value readEnum(TypeDefinition type) throws SyntaxError;

    protected abstract Value readArray(TypeDefinition type, int minSize, int maxSize) throws SyntaxError;

    protected abstract Value readVariant(TypeDefinition type) throws SyntaxError;

    protected abstract Value readString(TypeDefinition type) throws SyntaxError;

    protected abstract Value readRecord(TypeDefinition type) throws SyntaxError;

}