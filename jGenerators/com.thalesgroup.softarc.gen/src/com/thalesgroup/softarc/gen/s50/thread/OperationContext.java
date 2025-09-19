/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread;

import com.thalesgroup.softarc.gen.s50.thread.sizing.TypeSizeContext;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.Link;
import com.thalesgroup.softarc.sf.Thread;

// Dimensionnements d'une opération

public class OperationContext {
    // Vrai ssi l'opération est un service synchrone
    public final boolean async;

    // Dimensionnement des paramètres d'entrée
    // (y compris paramètres techniques)
    public final TypeSizeContext in;

    // Dimensionnement des paramètres de sortie
    public final TypeSizeContext out;

    // Dimensionnement de la donnée utile dans le cas d'une opération de type DATA
    public final TypeSizeContext data;

    public Instance instance;

    public Link link;

    public OperationContext(boolean is_asynchronous, Instance instance, Link link) {
        this.async = is_asynchronous;
        this.instance = instance;
        this.link = link;

        this.in = new TypeSizeContext();
        this.out = new TypeSizeContext();
        this.data = new TypeSizeContext();
    }

    public void updateBufferSize(long bufferSize) {
        long currentBufferSize = instance.getBufferSize();

        if (bufferSize > currentBufferSize) {
            instance.setBufferSize(bufferSize);
            if (!instance.getIsExtern()) {
                Thread thread = this.instance.getThread();
                thread.setBufferInSize(thread.getBufferInSize() + bufferSize);

                if (thread.getBufferSize() < bufferSize) {
                    thread.setBufferSize(bufferSize);
                }
            }
        }
    }
}
