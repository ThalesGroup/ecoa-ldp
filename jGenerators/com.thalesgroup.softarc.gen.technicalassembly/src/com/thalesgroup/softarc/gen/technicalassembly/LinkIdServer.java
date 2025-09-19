/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.HashSet;

public class LinkIdServer {

    // REQ-008
    private final static int MINIMUM = 1000;

    private HashSet<Long> used = new HashSet<Long>();

    private long constrain(long candidate) {
        long result = candidate;
        // REQ-008
        // REQ-009
        if (result < 0)
            result = -result;
        result %= 65536;
        result += MINIMUM;
        if (result % 2 != 0)
            result++;
        return result;
    }

    long getLinkId(int candidate) {
        long value = candidate;
        for (;;) {
            value = constrain(value);
            if (!used.contains(value)) {
                break;
            }
            value++;
        }
        used.add(value);
        return value;
    }

}
