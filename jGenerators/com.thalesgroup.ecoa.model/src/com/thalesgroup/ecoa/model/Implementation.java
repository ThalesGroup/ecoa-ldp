/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import technology.ecoa.model.implementation.CTComposite;
import technology.ecoa.model.implementation.CTImplementation;
import technology.ecoa.model.implementation.CTLanguage;

import com.thalesgroup.softarc.tools.Requirement;

public class Implementation {

    protected final CTImplementation _implementation;
    private String _name;
    private String _componentTypeName;
    private CTLanguage _languageInfo;

	/**
	 * @param implementation may be null in case of a Composite
	 * @param typeName
	 * @param implementationName
	 */
	public Implementation(CTImplementation implementation, String typeName, String implementationName) {
		_implementation = (implementation != null ? implementation : new CTImplementation());
		_name = implementationName;
		_componentTypeName = typeName;

		if (implementation == null) {
			_implementation.setComposite(new CTComposite());
		}

		CTLanguage[] a = { _implementation.getLanguageC(), _implementation.getLanguageCpp(),
				_implementation.getLanguageAda(), _implementation.getLanguageJava(),
				_implementation.getLanguagePython(), _implementation.getLanguageRust() };
		for (CTLanguage l : a) {
		    if (l != null)
		        _languageInfo = l;
		}
	}

    public String getName() {
        return _name;
    }

    public String getTypeName() {
        return _componentTypeName;
    }

    public CTImplementation getImplementation() {
        return _implementation;
    }

    @Requirement(ids = { "GenFramework-SRS-REQ-131" })
    public boolean isComposite() {
        return _implementation.getComposite() != null;
    }

    @Requirement(ids = { "GenFramework-SRS-REQ-130" })
    public Language getLanguageType() {
        Language lang = null;
        if (_implementation.getLanguageC() != null)
            lang = Language.C;
        else if (_implementation.getLanguageCpp() != null)
            lang = Language.CPP;
        else if (_implementation.getLanguageAda() != null)
            lang = Language.ADA;
        else if (_implementation.getLanguageJava() != null)
            lang = Language.JAVA;
        else if (_implementation.getLanguagePython() != null)
            lang = Language.PYTHON;
        else if (_implementation.getLanguageRust() != null)
            lang = Language.RUST;
        return lang;
    }

    public boolean isEcoa() {
        if (_implementation.getLanguageAda() != null || _implementation.getLanguageC() != null
                || _implementation.getLanguageCpp() != null || _implementation.getLanguageJava() != null
                || _implementation.getLanguagePython() != null || _implementation.getLanguageRust() != null)
            return false;
        else
            return true;
    }
    
    public long getStack() {
        return this._languageInfo.getStack();
    }

    public long getExternalStack() {
        return this._languageInfo.getExternalStack();
    }

}
