/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that throws exceptions when trying to insert duplicates.  
 */
public class UniqueList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 1L;

    private void check(E e) {
        if (contains(e))
            throw new IllegalArgumentException("element is not unique:" + e);
    }

    private void checkAll(Collection<? extends E> c) {
        for (E e : c) {
            check(e);
        }
    }

    @Override
    public boolean add(E e) {
        check(e);
        return super.add(e);
    }

    @Override
    public void add(int index, E element) {
        check(element);
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        checkAll(c);
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        checkAll(c);
        return super.addAll(index, c);
    }

}
