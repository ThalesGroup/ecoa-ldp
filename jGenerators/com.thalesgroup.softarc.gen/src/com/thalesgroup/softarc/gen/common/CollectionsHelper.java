/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

public abstract class CollectionsHelper {
    private final static Random random;

    static {
        random = new Random();
    }

    public static <T> T first(Collection<T> collection) {
        try {
            return collection.iterator().next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static <T> T get(Collection<T> collection, int index) {
        List<T> list = new ArrayList<>(collection);
        return list.get(index);
    }

    public static <T> T sample(Collection<T> collection) {
        int index = randomIndex(collection);
        return get(collection, index);
    }

    public static <T> List<T> sample(Collection<T> collection, int sampleSize) {
        if (collection.size() < sampleSize) {
            throw new Error();
        }

        if (collection.size() == sampleSize) {
            List<T> list = new ArrayList<>(collection);
            Collections.shuffle(list);
            return list;
        } else {
            List<T> list = new ArrayList<>();
            List<T> sourceList = new ArrayList<>(collection);
            Set<Integer> selectedIndices = new HashSet<>();
            while (selectedIndices.size() < sampleSize) {
                int index;
                do {
                    index = randomIndex(collection);
                } while (selectedIndices.contains(index));
                selectedIndices.add(index);
                list.add(sourceList.get(index));
            }
            return list;
        }
    }

    private static int randomIndex(Collection<?> collection) {
        return random.nextInt(collection.size());
    }
}
