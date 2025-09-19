/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import technology.ecoa.model.componenttype.CTData;
import technology.ecoa.model.componenttype.CTEvent;
import technology.ecoa.model.componenttype.CTOperation;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.CTRequestResponse;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.componenttype.CTWrittenData;
import technology.ecoa.model.assembly.ASDefaultDataValue;

public class Operation implements Comparable<Operation> {

    public Long id;
    public String name;
    public CTOperation origin;
    public boolean writeOnly = false;
    public ComponentInstance instance;
    public ASDefaultDataValue defaultvalue;
    public final EDR type;

    public Operation(ComponentInstance instance, String name, CTOperation ctop) {
        super();
        this.instance = instance;
        this.name = name;
        this.origin = ctop;

        if (origin instanceof CTEvent)
            type = EDR.Event;
        else if (origin instanceof CTData)
            type = EDR.Data;
        else if (origin instanceof CTRequestResponse)
            type = EDR.RequestResponse;
        else
            throw new Error();
    }

    @Override
    public int compareTo(Operation o) {
        int ret = instance.compareTo(o.instance);
        if (ret == 0)
            ret = name.compareTo(o.name);
        return ret;
    }

    @Override
    public String toString() {
        if (instance == null)
            return name;
        else
            return instance.fullName + "." + name;
    }

    public boolean isConcrete() {
        return instance != null && !instance.isComposite();
    }

    public boolean isExternalOperationOf(ComponentInstance composite) {
        return this.instance == composite;
    }
    
    public Class<?> apparentType(ComponentInstance fromComposite) {
        Class<? extends CTOperation> result = origin.getClass();
        if (instance == fromComposite) {
            // reverse the type (because it is used from the inside of the composite)
            switch (type) {
            case Data:
                if (result == CTWrittenData.class)
                    result = CTReadData.class;
                else if (result == CTReadData.class)
                    result = CTWrittenData.class;
                else
                    throw new Error();
                break;
            case Event:
                if (result == CTReceivedEvent.class)
                    result = CTSentEvent.class;
                else if (result == CTSentEvent.class)
                    result = CTReceivedEvent.class;
                else
                    throw new Error();
                break;
            case RequestResponse:
                if (result == CTRequestReceived.class)
                    result = CTRequestSent.class;
                else if (result == CTRequestSent.class)
                    result = CTRequestReceived.class;
                else
                    throw new Error();
                break;
            }
        }
        return result;
    }
    
    public boolean isOfApparentType(ComponentInstance fromComposite, Class<? extends CTOperation> operationClass) {
        return apparentType(fromComposite).isAssignableFrom(operationClass);
    }
}
