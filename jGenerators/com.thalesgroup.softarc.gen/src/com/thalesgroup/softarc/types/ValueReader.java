/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;

import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.VariantField;

public class ValueReader extends AbstractValueReader {

    private String input;
    private CharacterIterator iterator;
    private char c;
    private StringBuilder buf = new StringBuilder();
    private static final char STRING_DELIMITER = '\'';

    public ValueReader() {
		super(true);
	}
    
    public ValueReader(boolean autoCheckBounds) {
		super(autoCheckBounds);
	}
    
	public static Value parseValue(String string, TypeDefinition type) throws SyntaxError {
        ValueReader reader = new ValueReader(true);
        return reader.read(string, type);
    }

    public Value read(String string, TypeDefinition type) throws SyntaxError {
        input = string;
        iterator = new StringCharacterIterator(string);
        c = iterator.first();
        skipWhiteSpace();
        if (atEnd() && type.getFields().isEmpty() && type.getIsRecord()) {
            return new RecordValue(type);
        }
        Value ret = read(type);
        skipWhiteSpace();
        if (!atEnd())
            error("extra characters found after parsing value");
        return ret;
    }

    @Override
    protected Value readRecord(TypeDefinition type) throws SyntaxError {
        Collection<Parameter> fields = type.getFields();
        RecordValue r = new RecordValue(type);
        expect('{');
        int i = 0;
        for (Parameter field : fields) {

            if (i > 0)
                separator();

            // read the value of the element (recursion)
            r.fields[i++] = read(field.getType());

            skipWhiteSpace();
        }
        // tolerate an extra ',' at end
        if (c == ',') {
            next();
            skipWhiteSpace();
        }
        if (c == '}') {
            next();
            skipWhiteSpace();
        }
        return r;
    }

    @Override
    protected Value readString(TypeDefinition type) throws SyntaxError {
        String s = null;

        if (c == STRING_DELIMITER) {
            s = string();
            if (s.length() > type.getLength()) {
                error("string '%s' is too long, length is %d, maximum length is %d", s, s.length(), type.getLength());
            }
        } else {
            error("expecting \' as a string delimiter");
        }

        return new StringValue(type, s);
    }

    @Override
    protected Value readVariant(TypeDefinition type) throws SyntaxError {
        expect('{');
        VariantValue vv = new VariantValue(type);
        TypeDefinition selectType = type.getBaseType();
        vv.selector = read(selectType);
        if (vv.selector == null) {
            error("cannot determine selector value for type " + type);
        }
        try {
            long selectorKey = vv.selector.toIntegerKey();
            int i = 0;
            for (Parameter field : type.getFields()) {
                separator();
                vv.fields[i++] = read(field.getType());
                skipWhiteSpace();
            }
            for (VariantField unionfield : type.getUnionFields()) {
                Value when = parseValue(unionfield.getWhen(), selectType);
                if (selectorKey == when.toIntegerKey()) {
                    separator();
                    vv.union = read(unionfield.getType());
                    vv.unionName = unionfield.getName();
                    skipWhiteSpace();
                }
            }
        } catch (Error e) {
            error("value of selector must be integer or enum value and is '%s'", vv.selector);
        }
        // tolerate an extra ',' at end
        optional(',');
        optional('}');
        return vv;
    }

    @Override
    protected Value readArray(TypeDefinition type, int minSize, int maxSize) throws SyntaxError {
        assert type != null;
        TypeDefinition elementType = type.getBaseType();
        assert elementType != null;
        assert minSize <= maxSize;
        ArrayValue a = new ArrayValue(type, maxSize);
        int actualSize = 0;
        try {
            if (c == '{') {
                next();
                skipWhiteSpace();
                int i = 0;
                int multiply;
                while (c != '}' && !atEnd()) {
                    if (i > 0)
                        separator();

                    // tolerate an extra ',' at end
                    if (c == '}')
                        break;

                    // manage optional [n] indicating repetition of n times the same value
                    multiply = 1;
                    if (c == '[') {
                        next();
                        multiply = (int) parseInteger();
                        expect(']');
                    }

                    // read the value of the element (recursion)
                    Value v = read(elementType);

                    skipWhiteSpace();
                    while (multiply >= 1) {
                        a.v[i++] = v;
                        multiply--;
                    }
                }
                optional(',');
                optional('}');
                actualSize = i;
            } else if (c == STRING_DELIMITER) {
                // array written as a string
                if (!elementType.getIsPredef())
                    error("string litterals are only allowed for arrays of predef types");

                String s = string();
                int i = 0;
                switch (elementType.getName()) {
                case "float32":
                case "double64":
                    error("string litterals are only allowed for arrays of integer types");
                case "boolean8":
                    // an array of booleans can be written for example as: '010001'
                    // (only 0 and 1 are allowed)
                    for (StringCharacterIterator it = new StringCharacterIterator(s); it
                            .current() != StringCharacterIterator.DONE; it.next()) {
                        switch (it.current()) {
                        case '0':
                        case '1':
                            a.v[i++] = new IntegerValue(elementType, Long.valueOf(it.current() - '0'));
                            break;
                        default:
                            error("array of boolean can only contain '0' and '1' characters", type.getName(), maxSize);
                        }
                    }
                    break;
                case "char8":
                    // all integer types ... : convert each character to its numeric (Unicode) value
                    for (StringCharacterIterator it = new StringCharacterIterator(s); it
                            .current() != StringCharacterIterator.DONE; it.next())
                        a.v[i++] = new IntegerValue(elementType, it.current());

                    break;

                default:
                    // all integer types ... : convert each character to its numeric (Unicode) value
                    for (StringCharacterIterator it = new StringCharacterIterator(s); it
                            .current() != StringCharacterIterator.DONE; it.next())
                        a.v[i++] = new IntegerValue(elementType, Long.valueOf(it.current()));
                    // add zero after last char, if possible
                    if (i < maxSize)
                        a.v[i++] = new IntegerValue(elementType, 0L);
                }
                actualSize = i;
            } else
                error("array syntax must start with { or ' and is %c", c);
        } catch (ArrayIndexOutOfBoundsException e) {
            error("arrays of type %s must have %d elements maximum", type.getName(), maxSize);
        }
        if (actualSize < minSize)
            error("array of type %s must have exactly %d elements and has %d", type.getName(), minSize, actualSize);
        a.resize(actualSize);
        assert a.v.length <= type.getArraySize();
        return a;
    }

    protected EnumerationValue readEnum(TypeDefinition type) throws SyntaxError {
        EnumerationValue v;
        if (Character.isDigit(c) || c == '-') {
            long num = parseInteger();
            v = TypeSystem.getEnumWrapper(type).mapNumberKey.get(num);
            if (v == null)
                error("invalid enum value for %s: %d", type, num);
        } else {
            String id = parseId();
            v = TypeSystem.getEnumWrapper(type).getValue(id);
            if (v == null)
                error("invalid enum value for %s: %s", type, id);
        }
        return v;
    }

    private long toBoolean(char c) throws SyntaxError {
        if (c == '0')
            return 0;
        if (c == '1')
            return 1;
        error("invalid syntax for boolean");
        return 0;
    }

    @Override
    protected Value readBoolean(TypeDefinition type) throws SyntaxError {
        return new IntegerValue(type, parseBoolean());
    }

    protected long parseBoolean() throws SyntaxError {
        if (Character.isDigit(c)) {
            long ret = toBoolean(c);
            next();
            return ret;
        }
        String id = parseId();
        if (id.equalsIgnoreCase("true") || id.equalsIgnoreCase("1"))
            return 1;
        if (id.equalsIgnoreCase("false") || id.equalsIgnoreCase("0"))
            return 0;
        error("invalid value '%s' for boolean", id);
        return 0;
    }

    private String parseId() throws SyntaxError {
        buf.setLength(0);
        if (Character.isJavaIdentifierStart(c)) {
            add(c);
        } else
            if (c == StringCharacterIterator.DONE)
                error("unexpected end of input (expected an identifier)");
            else
                error("unexpected character '%c' for identifier", c);
        while (Character.isJavaIdentifierPart(c))
            add(c);
        return buf.toString();
    }

    @Override
    protected Value readInteger(TypeDefinition type) throws SyntaxError {
        return new IntegerValue(type, parseInteger());
    }

    protected long parseInteger() throws SyntaxError {
        buf.setLength(0);

        if (c == '0') {
        	add(c);
        }
        if (c == 'x') {
        	add(c);
        }
        if (c == '-') {
            add(c);
        }
        while (Character.isDigit(c) || isHexLetter(c)) {
            add(c);
        }
        String s = buf.toString();
        if (s.isEmpty())
            error("expecting integer number");
        try {
            return Long.decode(s);
        } catch (NumberFormatException e) {
            error("invalid integer number format '%s'", s);
            return Long.MAX_VALUE;
        }
    }

    @Override
    protected Value readFloat(TypeDefinition type) throws SyntaxError {
        return new FloatValue(type, parseFloat());
    }
    
    @Override
    protected Value readDouble(TypeDefinition type) throws SyntaxError {
        return new DoubleValue(type, parseDouble());
    }
    
    protected String validateFloatting() {
        buf.setLength(0);

        if (c == '-') {
            add(c);
        }
        addDigits();
        if (c == '.') {
            add(c);
            addDigits();
        }
        if (c == 'e' || c == 'E') {
            add(c);
            if (c == '+' || c == '-') {
                add(c);
            }
            addDigits();
        }
        if (c == 'f' || c == 'F') {
            add(c);
        }

        return buf.toString();
    }
    
    protected float parseFloat() throws SyntaxError {
        String s = validateFloatting();
        if (s.isEmpty())
            error("expecting floating point number");
        try {
            float f = Float.valueOf(s);
            if (Float.isInfinite(f) || Float.isNaN(f))
                throw new NumberFormatException("infinite or not a number");
            return f;
        } catch (NumberFormatException e) {
            error("invalid floating point number format : %s", e.getMessage());
            return 0;
        }
    }

    protected double parseDouble() throws SyntaxError {
        String s = validateFloatting();
        if (s.isEmpty())
            error("expecting floating point number");
        try {
            double d = Double.valueOf(s);
            if (Double.isInfinite(d) || Double.isNaN(d))
                throw new NumberFormatException("infinite or not a number");
            return d;
        } catch (NumberFormatException e) {
            error("invalid floating point number format : %s", e.getMessage());
            return 0;
        }
    }

    @Override
    protected Value readCharacter(TypeDefinition type) throws SyntaxError {
        Character result = null;

        buf.setLength(0);

        // Première forme : 'a', '+'
        if (c == '\'') {
            next();
            result = Character.valueOf(c);
            next();
            if (c != '\'') {
                error("expecting character in 'a' form");
            }
            next();
            return new IntegerValue(type, result);
        }
        // Deuxième forme : hexadécimale ou entière
        else  {
            return new IntegerValue(type, parseInteger());
        }
    }

    private boolean isHexLetter(char c2) {
        return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private int addDigits() {
        int ret;
        for (ret = 0; Character.isDigit(c); ++ret) {
            add(c);
        }
        return ret;
    }

    private String string() {
        buf.setLength(0);
        next();
        while (c != STRING_DELIMITER && !atEnd()) {
            add(c);
        }
        next();
        return buf.toString();
    }

    private void add(char cc) {
        buf.append(cc);
        next();
    }

    private char next() {
        c = iterator.next();
        return c;
    }

    private boolean atEnd() {
        return c == CharacterIterator.DONE;
    }

    private void skipWhiteSpace() {
        while (Character.isWhitespace(c)) {
            next();
        }
    }

    private void optional(char ec) throws SyntaxError {
        if (c == ec) {
            next();
            skipWhiteSpace();
        }
    }

    private void expect(char ec) throws SyntaxError {
        if (atEnd())
            error("expecting character '%c' and found end of string", ec);
        if (c != ec)
            error("expecting character '%c' and found '%c'", ec, c);
        next();
        skipWhiteSpace();
    }

    private void separator() throws SyntaxError {
        expect(',');
    }

    protected void error(String fmt, Object... args) throws SyntaxError {
        String msg = String.format(fmt, args);
        throw new SyntaxError(String.format("%s at index %d in '%s'", msg, iterator.getIndex(), input));
    }
}
