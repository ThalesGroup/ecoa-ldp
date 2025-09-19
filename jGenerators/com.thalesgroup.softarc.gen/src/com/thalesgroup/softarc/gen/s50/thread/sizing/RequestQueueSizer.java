/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.thalesgroup.softarc.sf.Request;
import com.thalesgroup.softarc.sf.impl.QRequest;

// Classe utilitaire pour déterminer la taille minimale à allouer pour gérer
// un ensemble de requêtes gérées par le module sarc_fifo.
//
// Le scénario pour déterminer les caractéristiques d'une file est le suivant :
//
//  rs = new RequestQueueSizer();
//  rs.init ();
//  rs.add_request (id1, size1, count1);
//  rs.add_request (id2, size2, count2);
//   ...
//  rs.add_request (idN, sizeN, countN);
//
//  allocated_size = rs.get_size (true);
//  requests = rs.get_constraints ();

public class RequestQueueSizer {
    // Size, in bytes, of SARC_FifoHeader MW type
    private static final long header_size_fifo = 24;

    // Size, in bytes, of SARC_FifoCapacity MW type
    private static final long capacity_size = 8;

    // Size, in bytes, of SARC_FifoNode MW types
    private static final long node_size = 12;

    /* Cette option permet de retrouver un comportement antérieur à l'évolution [#3084],
     * concernant le dimensionnement des FIFOs. Il existait un certain niveau de mutualisation
     * entre les différents récepteurs d'une même opération. */
    private static final boolean shared_fifos = false;

    private final long header_size;

    // Number of different data managed by the queue
    public long data_count = 0;

    // Set of elementary requests, indexed and ordered by request identifier
    private final Map<Long, Request> requests = new TreeMap<>();

    // Sizer of the memory for requests'parameters
    private final VrSetSizer vr;

    public RequestQueueSizer(VrSetSizer p_vr) {
        this.vr = p_vr;
        this.header_size = header_size_fifo;
    }

    // Reset internal state to start a new sizing.
    public void init() {
        // Une requête n'étant qu'un identifiant de requête et ses paramètres
        // sérialisés au format "réseau", il n'y a pas de talon systématique à
        // appliquer
        this.requests.clear();
        this.data_count = 0;
    }

    /**
     * Take into account a specific request. 'size' is the parameters size, 'count' the maximum number of requests of this 'id'
     * that could be buffered.
     */
    public void add_request(long p_id, long p_size, long p_count) {
        if (p_count > 0) {
            Request request = requests.get(p_id);
            if (request == null) {
                request = new QRequest();
                request.setId(p_id);
                request.setParameterSize(p_size);
                requests.put(p_id, request);
            } else {
                assert request.getId() == p_id;
                assert request.getParameterSize() == p_size;
            }

            long delta = p_count;
            if (shared_fifos) {
                delta -= request.getCapacity();
            }
            if (delta > 0) {
                request.setCapacity(request.getCapacity() + delta);
                // Use request's characteristics for sizing
                if (p_size > 0 && vr != null)
                    vr.add_request(p_id, p_size, delta);
                
                data_count += delta;
            }
        }
    }

    /**
     * @return Size used by the fifo in memory (without the parameters)
     */
    public long get_size() {
        long result = this.header_size;

        result += this.requests.size() * RequestQueueSizer.capacity_size;

        result +=this.data_count * RequestQueueSizer.node_size;

        return result;
    }

    // Number of data to manage. Useful only when request queue is not
    // constrained.
    public long get_capacity() {
        return this.data_count;
    }

    // Set of elementary requests, ordered per request identifier. Useful only
    // when request queue is constrained
    public List<Request> get_constraints() {
        List<Request> result = new ArrayList<Request>(this.requests.values());

        return result;
    }

}
