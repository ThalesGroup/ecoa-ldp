/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import technology.ecoa.model.componenttype.CTComponentType;
import technology.ecoa.model.componenttype.CTData;
import technology.ecoa.model.componenttype.CTEvent;
import technology.ecoa.model.componenttype.CTOperation;
import technology.ecoa.model.componenttype.CTProperties;
import technology.ecoa.model.componenttype.QualifiedField;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.componenttype.CTRequestResponse;
import technology.ecoa.model.componenttype.CTTrigger;
import technology.ecoa.model.componenttype.CTTriggers;
import technology.ecoa.model.componenttype.CTVariables;
import technology.ecoa.model.componenttype.CTWrittenData;
import technology.ecoa.model.componenttype.EComponentKind;

import com.thalesgroup.softarc.tools.Requirement;

@Requirement(ids = { "GenFramework-SRS-REQ-126", "GenFramework-SRS-REQ-132" })
public final class ComponentType extends TypesContainer {

    private final String notify_prefix = "sarc_notify";
    private final String trigger_prefix = "sarc_trigger";

    public ComponentType(String name, CTComponentType ct) {
        super(name, null);

        _componentType = ct;

        registerAttributes();
        registerVariables();
        registerTriggers();
        registerOperations();
        /*
         * recompute the set of used libraries because new types (used by Attributes and Operations) have been added to
         * ComponentType._usedTypes
         */
        super.registerUsedLibraries();
    }

    private void registerAttributes() {
        CTProperties attributes = _componentType.getProperties();
        if (attributes != null) {
            for (QualifiedField attribute : attributes.getProperty()) {
                _attributes.put(attribute.getName(), attribute);
                _usedTypes.add(attribute.getType());
            }
        }
    }

    private void registerTriggers() {
        CTTriggers trigs = _componentType.getTriggers();

        if (trigs != null) {
            for (CTTrigger trig : trigs.getTrigger()) {
                _allTriggers.put(trig.getName(), trig);

                // trigger back interface is generated for each trigger
                CTSentEvent triggerEvent = new CTSentEvent();
                triggerEvent.setName(this.trigger_prefix + '_' + trig.getName());
                this.addEventSent(triggerEvent);
            }
        }
    }

    // Wrap generic variable to an enum type

    private void registerVariables() {
        CTVariables variables = this._componentType.getVariables();

        if (variables != null) {
            for (QualifiedField variable : variables.getVariable()) {
                this._variables.put(variable.getName(), variable);
                this._usedTypes.add(variable.getType());
            }
        }
    }

    private void registerOperations() {
        registerOperations(_componentType.getOperations().getAllOperations());

        for (CTReceivedEvent notify_event : _notifyEvents) {
            this.addEventReceived(notify_event);
        }

        // In any case, generic notification interface are added to all
        // component types

        if (this._componentType.getKind() != EComponentKind.PERIODIC_TRIGGER_MANAGER) {
            CTSentEvent event = new CTSentEvent();

            event.setName(this.notify_prefix);
            this.addEventSent(event);

        }
    }

    // ! Register operations from internal CTComponentType, and create new
    // ! interfaces from interpretation of these operations'attributes

    private void registerOperations(Collection<CTOperation> collection) {
        for (CTOperation operation : collection) {
            if (operation instanceof CTReadData) {
                addDataRead((CTReadData) operation);
            } else if (operation instanceof CTWrittenData) {
                addDataWritten((CTWrittenData) operation);
            } else if (operation instanceof CTReceivedEvent) {
                addEventReceived((CTReceivedEvent) operation);
            } else if (operation instanceof CTSentEvent) {
                addEventSent((CTSentEvent) operation);
            } else if (operation instanceof CTRequestSent) {
                addRequestResponseRequired((CTRequestSent) operation);
            } else if (operation instanceof CTRequestReceived) {
                addRequestResponseProvided((CTRequestReceived) operation);
            } else {
                throw new IllegalStateException(UNEXPECTED + operation.getClass());
            }
        }
    }

    // ! Add operation to the appropriate operations'list

    private void addToAllOps(OperationType p_op_type, CTOperation p_operation) {
        List<CTOperation> operations = this._allOps.get(p_op_type);
        if (operations == null) {
            operations = new ArrayList<CTOperation>();
            this._allOps.put(p_op_type, operations);
        }
        operations.add(p_operation);
    }

    private void addDataRead(CTReadData data) {
        _dataRead.put(data.getName(), data);

        if (!_allDataOps.containsKey(data.getName())) {
            _allDataOps.put(data.getName(), new ArrayList<CTData>());
        }
        _allDataOps.get(data.getName()).add(data);

        _usedTypes.add(data.getType());

        this.addToAllOps(OperationType.data_read, data);

        // Manage corresponding notification event

        if (data.isNotifying()) {
            CTReceivedEvent notify_event = new CTReceivedEvent();

            notify_event.setName(this.notify_prefix + "_" + data.getName());
            this._notifyEvents.add(notify_event);
        }
    }

    private void addDataWritten(CTWrittenData data) {
        _dataWritten.put(data.getName(), data);

        if (!_allDataOps.containsKey(data.getName())) {
            _allDataOps.put(data.getName(), new ArrayList<CTData>());
        }
        _allDataOps.get(data.getName()).add(data);

        _usedTypes.add(data.getType());

        this.addToAllOps(OperationType.data_written, data);

        // Manage corresponding reading interface (and so the associated
        // notification event)

        if (!data.isWriteOnly()) {
            CTReadData read_data = new CTReadData();

            // ACHTUNG!!! recopie des champs communs 'à la main'.
            // Manquera-t-il des champs à l'avenir ?
            // Réponse partielle : oui, c'est déjà arrivé (défaut n°905)
            read_data.setName(data.getName());
            read_data.setType(data.getType());
            read_data.setMaxVersions(data.getMaxVersions());
            read_data.setNotifying(data.isNotifying());

            addDataRead(read_data);
        }
    }

    private void addEventReceived(CTReceivedEvent event) {
        _allEventOps.put(event.getName(), event);
        _eventReceived.put(event.getName(), event);
        for (QualifiedField p : event.getParameter()) {
            _usedTypes.add(p.getType());
        }
        this.addToAllOps(OperationType.event_received, event);
    }

    private void addEventSent(CTSentEvent event) {
        _allEventOps.put(event.getName(), event);
        _eventSent.put(event.getName(), event);
        for (QualifiedField p : event.getParameter()) {
            _usedTypes.add(p.getType());
        }
        this.addToAllOps(OperationType.event_sent, event);
    }

    private void addRequestResponseRequired(CTRequestSent service) {
        _allSrvcOps.put(service.getName(), service);
        _servicesRequired.put(service.getName(), service);
        for (QualifiedField p : service.getParameter()) {
            _usedTypes.add(p.getType());
        }
        for (QualifiedField p : service.getOut()) {
            _usedTypes.add(p.getType());
        }
        this.addToAllOps(OperationType.service_required, service);
    }

    private void addRequestResponseProvided(CTRequestReceived service) {
        _allSrvcOps.put(service.getName(), service);
        _servicesProvided.put(service.getName(), service);
        for (QualifiedField p : service.getParameter()) {
            _usedTypes.add(p.getType());
        }
        for (QualifiedField p : service.getOut()) {
            _usedTypes.add(p.getType());
        }
        this.addToAllOps(OperationType.service_provided, service);
    }

    public CTComponentType getComponentType() {
        return _componentType;
    }

    public EComponentKind getKind() {
        return this._componentType.getKind();
    }

    public QualifiedField getParameter(String name) {
        return _attributes.get(name);
    }

    public QualifiedField getVariable(String name) {
        QualifiedField result = null;

        result = this._variables.get(name);

        return result;
    }

    public boolean hasOperation(String name) {
        return _allDataOps.containsKey(name) || _allEventOps.containsKey(name) || _allSrvcOps.containsKey(name);
    }

    // writer and reader links can share the same name

    public List<CTData> getDataOperation(String name) {
        return _allDataOps.get(name);
    }

    public CTReadData getReadDataOperation(String name) {
        return this._dataRead.get(name);
    }

    public CTWrittenData getWrittenDataOperation(String name) {
        return this._dataWritten.get(name);
    }

    public CTEvent getEventOperation(String name) {
        return _allEventOps.get(name);
    }

    public CTSentEvent getSentEventOperation(String name) {
        return this._eventSent.get(name);
    }

    public CTRequestResponse getRequestResponseOperation(String name) {
        return _allSrvcOps.get(name);
    }

    public CTRequestSent getRequiredRequestResponseOperation(String name) {
        return this._servicesRequired.get(name);
    }

    public CTTrigger getTrigger(String name) {
        return _allTriggers.get(name);
    }

    public List<CTOperation> getOperations(OperationType... opTypes) {
        ArrayList<CTOperation> operations = new ArrayList<CTOperation>();
        for (OperationType opType : opTypes) {
            if (_allOps.containsKey(opType)) {
                operations.addAll(_allOps.get(opType));
            }
        }
        return operations;
    }

    public Map<String, CTRequestResponse> getRequestResponses() {
        return _allSrvcOps;
    }

    public Map<String, CTTrigger> getTriggers() {
        return _allTriggers;
    }

    public Boolean getHasTriggers() {
        return (!this.getTriggers().isEmpty());
    }

    public Map<String, CTEvent> getEvents() {
        return _allEventOps;
    }

    public Map<String, ? extends List<CTData>> getData() {
        return _allDataOps;
    }

    public List<CTOperation> getAllOperations() {
        return getOperations(OperationType.values());
    }

    public Map<String, CTReadData> getDataRead() {
        return _dataRead;
    }

    public Map<String, CTWrittenData> getDataWritten() {
        return _dataWritten;
    }

    public Map<String, CTReceivedEvent> getEventReceived() {
        return _eventReceived;
    }

    public Map<String, CTSentEvent> getEventSent() {
        return _eventSent;
    }

    public Map<String, CTRequestReceived> getRequestResponsesProvided() {
        return _servicesProvided;
    }

    public Map<String, CTRequestSent> getRequestResponsesRequired() {
        return _servicesRequired;
    }

    private final Map<String, QualifiedField> _attributes = new LinkedHashMap<String, QualifiedField>();

    private final Map<String, QualifiedField> _variables = new LinkedHashMap<String, QualifiedField>();

    private final Map<String, CTReadData> _dataRead = new LinkedHashMap<String, CTReadData>();

    private final Map<String, CTWrittenData> _dataWritten = new LinkedHashMap<String, CTWrittenData>();

    private final Map<String, CTReceivedEvent> _eventReceived = new LinkedHashMap<String, CTReceivedEvent>();

    private final Map<String, CTSentEvent> _eventSent = new LinkedHashMap<String, CTSentEvent>();

    private final Map<String, CTRequestReceived> _servicesProvided = new LinkedHashMap<String, CTRequestReceived>();

    private final Map<String, CTRequestSent> _servicesRequired = new LinkedHashMap<String, CTRequestSent>();

    private final Map<String, ArrayList<CTData>> _allDataOps = new LinkedHashMap<String, ArrayList<CTData>>();

    private final Map<String, CTTrigger> _allTriggers = new LinkedHashMap<String, CTTrigger>();

    private final Map<String, CTEvent> _allEventOps = new LinkedHashMap<String, CTEvent>();

    private final Map<String, CTRequestResponse> _allSrvcOps = new LinkedHashMap<String, CTRequestResponse>();

    private final Map<OperationType, List<CTOperation>> _allOps = new LinkedHashMap<OperationType, List<CTOperation>>();

    private final ArrayList<CTReceivedEvent> _notifyEvents = new ArrayList<CTReceivedEvent>();

    private final CTComponentType _componentType;

    private static final String UNEXPECTED = "unexpected ";

}
