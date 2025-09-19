/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

/**
 * The different types of operations defining the interface of SOFTARC components, including their "direction".
 */
public enum OperationType {

    /**
     * An event received by a component.
     */
    event_received,
    /**
     * An event sent by a component.
     */
    event_sent,
    /**
     * A service provided by a component, i.e. the component is the server.
     */
    service_provided,
    /**
     * A service required (or "called") by a component, i.e. the component is the client.
     */
    service_required,
    /**
     * A data accessed in read mode only. (pronounce: "data red")
     */
    data_read,
    /**
     * A data accessed in read-write mode, or write only. Note that a data_written may "hide" a data_read.
     */
    data_written;

}
