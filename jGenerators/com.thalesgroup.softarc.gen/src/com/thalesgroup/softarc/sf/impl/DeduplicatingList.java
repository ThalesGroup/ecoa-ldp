/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.sf.impl;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that throws exceptions when trying to insert duplicates.
 */
public class DeduplicatingList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 67897623L;

    @Override
    public boolean add(E e) {
        if (contains(e))
            return false;
        return super.add(e);
    }

    @Override
    public void add(int index, E element) {
        if (!contains(element))
            super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (var e : c)
            add(e);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new IllegalArgumentException();
    }
}
