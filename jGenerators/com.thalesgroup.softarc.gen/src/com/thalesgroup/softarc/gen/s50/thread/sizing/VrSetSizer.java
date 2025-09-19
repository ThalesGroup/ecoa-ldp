/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.thalesgroup.softarc.sf.Request;
import com.thalesgroup.softarc.sf.impl.QRequest;

// Classe utilitaire pour déterminer la taille d'un ensemble de VRs,
// identifiés par id de requête, pour stocker des paramètres de taille
// identique.
// Un VR permet de mémoriser les paramètres d'un évènement, d'une demande de service ou
// d'un retour de service.
// Une demande et un retour ayant le même id, il faut un VrSetSizer pour les évènements+demandes, et un autre pour les retours.
//
public class VrSetSizer {

    // Set of VRs, indexed and ordered by request identifier.
    // Each VR has a id (the request id), a specific size of parameters, and a capacity.
    private final Map<Long, Request> repositories = new TreeMap<>();

    public final String description;
    
    public VrSetSizer(String description) {
        this.description = description;
    }
    
    /**
     * Take into account a specific request. 'size' is the parameters size, 'count' the maximum number of requests of this 'id'
     * that could be buffered.
     */
    public void add_request(long p_id, long p_size, long p_count) {
        if (p_count > 0 && p_size > 0) {
            Request repo = repositories.get(p_id);
            if (repo == null) {
                repo = new QRequest();
                repo.setId(p_id);
                repo.setParameterSize(p_size);
                repo.setCapacity(1);  // one version for storage of the "reference" version (the last published)
                repositories.put(p_id, repo);
            } else {
                assert repo.getId() == p_id;
                assert repo.getParameterSize() == p_size;
            }
            repo.setCapacity(repo.getCapacity() + p_count);
        }
    }

    /**
     * @return Sum of DataVR sizes.
     */
    public long get_total_size() {
        long result = 0;

        for (Request req : getRepositories())
            result += get_size_of_VR(req);

        return result;
    }

    public Collection<Request> getRepositories() {
        return this.repositories.values();
    }

    /**
     * @return Total size of the VR associated to the request (O if no DataVR).
     */
    private static long get_size_of_VR(Request repo) {

        if (repo.getParameterSize() == 0) {
            return 0;
        } else {
            return DataVrSizer.computeDataVrShmGlobalSize(repo.getCapacity(), repo.getParameterSize());
        }
    }

    /**
     * @return Total size of the VR associated to the request (O if no DataVR).
     */
    public long get_size_of_VR(long p_id) {

        return get_size_of_VR(this.repositories.get(p_id));
    }

    /**
     * Provide information about data sizes used. This information may be useful in case of saturation.
     */
    public ArrayList<String> get_sizing_information() {
        ArrayList<String> result = new ArrayList<String>();
        
        result.add ("Sizing information for " + description + ":");
        long net_size = 0;
        for (Request it : getRepositories()) {
            result.add (String.format(" - for request %5d: %3d elements of size %5d", it.getId(), it.getCapacity(), it.getParameterSize()));
            net_size += it.getCapacity() * it.getParameterSize();
        }
        long raw_size = get_total_size();
        result.add (String.format(" Net size = %d bytes = %d Mb", net_size, net_size / 1024 / 1024));
        result.add (String.format(" Raw size = %d bytes = %d Mb", raw_size, raw_size / 1024 / 1024));

        return result;
    }

}
