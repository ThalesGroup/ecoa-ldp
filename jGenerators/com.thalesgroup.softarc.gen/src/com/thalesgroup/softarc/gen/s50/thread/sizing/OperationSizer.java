/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

import java.util.Collection;

import java.io.IOException;
import com.thalesgroup.softarc.gen.s50.thread.OperationContext;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;

public class OperationSizer {

    private final boolean simulation;

    public OperationSizer() {
        this(false);
    }

    public OperationSizer(boolean isSimulation) {
        this.simulation = isSimulation;
    }

    // Analyze parameters'list field-by-field and compute its various sizes

    private void computeParametersSize(Component componentTypeModel, Collection<Parameter> inList, TypeSizeContext sizeCtxt) {
        for (Parameter parameter : inList) {

            TypeSizeContext typeSizeCtxt = new TypeSizeContext();
            TypeSizer.computeTypeSize(parameter.getType(), typeSizeCtxt);

            sizeCtxt.add_field(typeSizeCtxt);
        }
    }

    // Dimensionne les ressources en entrée et en sortie d'un service

    public void computeRequestResponseSize(OperationRequestResponse operationDefinition, OperationContext operationCtxt) throws IOException {
        Component componentTypeModel = operationCtxt.instance.getType();
        Collection<Parameter> inList = operationDefinition.getInParameters();
        Collection<Parameter> outList = operationDefinition.getOutParameters();

        // A requestID is always prepend, even if it may not be useful to
        // service caller
        TypeSizeContext id_size = new TypeSizeContext();

        TypeSizer.computePredefTypeSize("int32", id_size);

        // Input parameters
        operationCtxt.in.add_field(id_size);
        if (this.simulation) {
            operationCtxt.in.add_field(id_size); // client instance
            operationCtxt.in.add_field(id_size); // client activation
        }
        computeParametersSize(componentTypeModel, inList, operationCtxt.in);
        operationCtxt.in.finalize();
        operationDefinition.setSize(operationCtxt.in.raw_size);

        // Output parameters
        operationCtxt.out.add_field(id_size);
        if (this.simulation) {
            operationCtxt.out.add_field(id_size); // server instance
            operationCtxt.out.add_field(id_size); // server activation
        }
        computeParametersSize(componentTypeModel, outList, operationCtxt.out);
        operationCtxt.out.finalize();
        operationDefinition.setSizeOut(operationCtxt.out.raw_size);
    }

    // Dimensionne les ressources nécessaires à la circulation d'un événement

    public void computeEventSize(OperationEvent operationDefinition, OperationContext operationCtxt) throws IOException {
        Component componentTypeModel = operationCtxt.instance.getType();
        Collection<Parameter> parameterList;

        if (this.simulation) {
            TypeSizeContext id_size = new TypeSizeContext();
            TypeSizer.computePredefTypeSize("int32", id_size);
            operationCtxt.in.add_field(id_size); // sender instance
            operationCtxt.in.add_field(id_size); // sender activation
        }
        parameterList = operationDefinition.getInParameters();
        computeParametersSize(componentTypeModel, parameterList, operationCtxt.in);
        operationCtxt.in.finalize();
        operationDefinition.setSize(operationCtxt.in.raw_size);
    }

    // Dimensionne les ressources nécessaires à la circulation d'une donnée

    public void computeDataSize(OperationData operationDefinition, OperationContext operationCtxt) throws IOException {
        // A 'republication' flag is always prepend, in order to distinguish
        // regular publications from the ones provoked by a REPUBLISH request
        TypeSizeContext flag_size = new TypeSizeContext();

        TypeSizer.computePredefTypeSize("boolean8", flag_size);
        operationCtxt.in.add_field(flag_size);

        // Data itself
        TypeSizeContext data_size = new TypeSizeContext();

        TypeSizer.computeTypeSize(operationDefinition.getType(), data_size);
        operationCtxt.in.add_field(data_size);
        operationCtxt.data.add_field(data_size);

        // Finalize
        operationCtxt.in.finalize();
        operationCtxt.data.finalize();
        operationDefinition.setSize(operationCtxt.in.raw_size);
    }
}
