/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools;

import java.io.File;

/**
 * This class defines different ways to handle file generation/regeneration.
 *
 */
public enum FileGenerationPolicy {

    Overwrite, // always generate the file
    Preserve, // do not generate anything if a file already exists
    CreateNew, // if a file already exists, create another one with the same name and extension ".new"
    Never; // never generate the file

    public File getRealFile(File nominalFile, AbstractLogger gen) {

        switch (this) {

        case Never:
            return null;

        case Preserve:
            if (nominalFile.exists()) {
                gen.report(nominalFile, ReportStatus.PRESERVED);
                return null;
            }
            break;

        case CreateNew:
            if (nominalFile.exists()) {
                gen.report(nominalFile, ReportStatus.PRESERVED);
                return new File(nominalFile.getAbsolutePath() + ".new");
            }
            break;

        case Overwrite:
            break;
        }
        return nominalFile;
    }

}