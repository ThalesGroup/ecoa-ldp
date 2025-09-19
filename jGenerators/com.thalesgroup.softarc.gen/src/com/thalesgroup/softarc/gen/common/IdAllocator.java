/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.util.HashSet;

import com.thalesgroup.softarc.tools.InconsistentModelError;

import java.io.IOException;

public class IdAllocator {

    protected HashSet<Long> allocated = new HashSet<Long>();
    private long counter = 1;
    private String label;

    public IdAllocator(String label, long initialValue) {
        this.label = label;
        counter = initialValue;
    }

    public boolean isAllocated(long value) {
        return allocated.contains(value);
    }

    public long allocate() throws IOException {
        long value;
        do {
            value = counter;
            counter++;
        } while (allocated.contains(value));
        allocated.add(value);
        return value;
    }

    public long allocate(Long constraint) throws IOException {
        if (constraint == null) {
            return allocate();
        } else {
            if (allocated.contains(constraint)) {
                throw new InconsistentModelError("Id " + constraint + " is already allocated for " + label);
            }
            allocated.add(constraint);
            return constraint;
        }
    }

    public long allocateFrom(Long suggestion) throws IOException {
        counter = suggestion;
        return allocate();
    }

    public long allocateFromString(String suggestion) throws IOException {
        counter = (long) suggestion.hashCode();
        return allocate();
    }

}
