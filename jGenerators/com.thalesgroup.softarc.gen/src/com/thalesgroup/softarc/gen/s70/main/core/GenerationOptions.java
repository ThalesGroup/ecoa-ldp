/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s70.main.core;

public class GenerationOptions {

    public boolean checkWCET;
    public boolean checkOccurenceRate;
    public boolean hasSafeReaders;
    public boolean simulation;
    public boolean observeWithMetrics = false;
    public boolean initializeParameters;

    /**
     * True <==> start_mode in ["synchronized", "fast"]
     */

    public boolean autoStart;

    /**
     * True <==> start_mode == "fast"
     */

    public boolean fastStart;

}
