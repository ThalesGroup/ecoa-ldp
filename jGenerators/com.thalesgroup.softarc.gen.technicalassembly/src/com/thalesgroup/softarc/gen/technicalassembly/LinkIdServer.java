/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASOpRef;
import technology.ecoa.model.assembly.ASOperationLink;
import technology.ecoa.model.assembly.ASRequestResponseLink;

public class LinkIdServer {

    // REQ-008
    private final static int MINIMUM = 1000;

    private HashSet<Integer> used = new HashSet<>();

    private int constrain(int candidate) {
        int result = candidate;
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

    int getLinkId(int candidate) {
        int value = candidate;
        for (;;) {
            value = constrain(value);
            if (used.add(value)) {
                break;
            }
            value++;
        }
        return value;
    }

    public void reserve(int value) {
        used.add(value);
    }

    void allocateId(ASOperationLink link) {
        ArrayList<ASOpRef> linkElements = new ArrayList<ASOpRef>();
        if (link instanceof ASEventLink) {
            linkElements.addAll(((ASEventLink) link).getSender());
            linkElements.addAll(((ASEventLink) link).getReceiver());
        }
        if (link instanceof ASDataLink) {
            linkElements.addAll(((ASDataLink) link).getWriter());
            linkElements.addAll(((ASDataLink) link).getReader());
        }
        if (link instanceof ASRequestResponseLink) {
            linkElements.add(((ASRequestResponseLink) link).getClient());
            linkElements.addAll(((ASRequestResponseLink) link).getServer());
        }
        TreeSet<String> set = new TreeSet<String>();
        for (ASOpRef le : linkElements) {
            set.add(le.getInstance());
            set.add(le.getOperation());
        }
        link.setId((long) (this.getLinkId(set.hashCode())));
    }

}
