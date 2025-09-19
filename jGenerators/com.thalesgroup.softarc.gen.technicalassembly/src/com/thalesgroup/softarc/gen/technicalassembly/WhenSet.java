/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.util.TreeSet;

public class WhenSet {
    public TreeSet<Condition> whencondition;

    public WhenSet() {
        this.whencondition = new TreeSet<Condition>();
    }

    public WhenSet(WhenSet other) {
        this();
        this.addAll(other);
    }

    public void addAll(WhenSet other) {
        this.whencondition.addAll(other.whencondition);
    }
}
