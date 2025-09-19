/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class StringJoiner {

    public static String stringJoin(List<String> strings, String joinner) {
        String ret = "";
        Iterator<String> it = strings.iterator();
        while (it.hasNext()) {
            ret += it.next();
            if (it.hasNext())
                ret += joinner;
        }
        return ret;
    }

    public static String stringJoin(String[] strings, String joiner) {
        return stringJoin(Arrays.asList(strings), joiner);
    }

}
