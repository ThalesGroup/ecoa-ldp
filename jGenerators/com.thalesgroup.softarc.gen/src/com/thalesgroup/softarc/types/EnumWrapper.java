/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.types;

import java.util.HashMap;
import java.util.Map;

import com.thalesgroup.softarc.sf.EnumValue;
import com.thalesgroup.softarc.sf.TypeDefinition;

public class EnumWrapper {

    EnumWrapper(TypeDefinition type) {
        for (EnumValue value : type.getEnumValues()) {
            EnumerationValue v = new EnumerationValue(this, type, value);
            if (mapSymbolKey.put(value.getName().toLowerCase(), v) != null)
                throw new Error();
            if (mapNumberKey.put(value.getValnum(), v) != null)
                throw new Error();
        }
    }

    /**
     * A map from symbol (converted to *lowercase*) to EnumValue
     */
    private final Map<String, EnumerationValue> mapSymbolKey = new HashMap<String, EnumerationValue>();
    public final Map<Long, EnumerationValue> mapNumberKey = new HashMap<Long, EnumerationValue>();

    public EnumerationValue getValue(String symbolOrNumber) {
        try {
            long n = Long.parseLong(symbolOrNumber);
            return mapNumberKey.get(n);
        } catch (NumberFormatException e) {
            return mapSymbolKey.get(symbolOrNumber.toLowerCase());
        }
    }

    public boolean equalsSymbolOrNumber(String symbolOrNumber1, String symbolOrNumber2) {
        return getValue(symbolOrNumber1) == getValue(symbolOrNumber2);
    }

}
