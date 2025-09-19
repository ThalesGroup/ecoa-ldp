/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import technology.ecoa.model.datatype.CTLibrary;

public class Library extends TypesContainer {

    public Library(String name, CTLibrary library) {
        super(name, library.getLibraryTypes());

        _library = library;
    }

    public CTLibrary getLibrary() {
        return _library;
    }

    private final CTLibrary _library;

}
