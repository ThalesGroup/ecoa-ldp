package com.thalesgroup.softarc.gen.s50.thread;

import java.util.ArrayList;
import java.util.Collection;

// Classe utilitaire au découpage en thread d'aiguillage

// Une union de coopératives permet de faire l'association entre un nom
// d'instance productrice et la coopérative à laquelle elle appartient.
//
// Lorsque l'ensemble des opérations ont été réparties dans les différentes
// coopératives, provoquant un premier regroupement des producteurs en
// coopératives, voire de coopératives en une coopérative plus importante,
// une dernière fusion intervient en considérant les échéances associées à
// chacune
//
// Scénario type:
//
//   CooperativeUnion CU = new CooperativeUnion ();
//
//   CU.dispatch_operation (eventLink, eventLink.getSender ());
//   CU.dispatch_operation (dataLink, dataLink.getWriter ());
//   CU.dispatch_operation (serviceLink, serviceLink.getClient ());
//   CU.dispatch_operation (serviceLink, serviceLink.getServer ());
//   ...
//   dispatches = CU.get_dispatch_threads ();

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.thalesgroup.softarc.sf.DispatchedLink;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.Dispatch;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.Link;
import com.thalesgroup.softarc.sf.MaskExternalElement;
import com.thalesgroup.softarc.sf.MaskInfo;
import com.thalesgroup.softarc.sf.impl.QDispatch;
import com.thalesgroup.softarc.sf.impl.QDispatchedLink;
import com.thalesgroup.softarc.sf.impl.QMaskExternalElement;

import technology.ecoa.model.deployment.IOInterfaceIn;

public class CooperativeUnion
{

    // Ensemble des coopératives par nom d'instance productrice.
    // La clé est le nom du port de réception (in ou in_out).
    private final Map<String, Dispatch> dispatchThreads = new LinkedHashMap<>();
        
    private void addMaskInfo(MaskInfo info, Dispatch dispatch, DispatchedLink link) {
		MaskExternalElement element = new QMaskExternalElement();
		element.setOperation(link.getOperationName());
    	element.setDispatch(dispatch);
    	element.setExternalId(link.getExternalId());
    	element.setShift(info.getShiftCount());
		info.setShiftCount(info.getShiftCount() + 1);
		info.getExternalEmitters().add(element);
    }

    public void dispatchExternalOperation(Link pOperation, MaskInfo maskInfo, List<ExternalOperationInfo> externalChannels, boolean pIncoming) {
    	
        // Fusion des coopératives concernant les producteurs
        for (ExternalOperationInfo info : externalChannels) {
            Dispatch dispatchThread = this.getDispatchThread(info.channel.getName());
            final IOInterfaceIn ioInterfaceIn = (IOInterfaceIn) (info.channel);

            // Modification de l'échéance si nécessaire
            Long prio = ioInterfaceIn.getRelativePriority();
            if (prio == null) {
                prio = Constants.HIGHEST_PRIORITY;
            }
            if (prio > dispatchThread.getRelativePriority()) {
                dispatchThread.setRelativePriority(prio);
            }

            // Ajout de l'opération au thread d'aiguillage
            DispatchedLink op_link = new QDispatchedLink();
            op_link.setId(pOperation.getId());
            op_link.setLink(pOperation);
            op_link.setNbProducers(ioInterfaceIn.getNbProducers().longValue());
            op_link.setExternalId(info.getId());
            op_link.setOperationName(info.operation.getName());

            if (pOperation instanceof EventLink) {
                EventLink eventLink = (EventLink) pOperation;
                
                addMaskInfo(maskInfo, dispatchThread, op_link);
            	// les événements de notification sont a filtrer
                if (eventLink.getNotifiedData() == null) {
                	dispatchThread.getDispatchedLinks().add(op_link);
                }
            }

            else if (pOperation instanceof DataLink) {
            	addMaskInfo(maskInfo, dispatchThread, op_link);
                dispatchThread.getDispatchedLinks().add(op_link);
            }

            else /* if (p_operation instanceof RequestResponseLink) */ {
            	if (pIncoming) {
                    // appel de service
            		addMaskInfo(maskInfo, dispatchThread, op_link);
                    dispatchThread.getDispatchedLinks().add(op_link);
                } else {
                    // retour de service
                	addMaskInfo(maskInfo, dispatchThread, op_link);
                    dispatchThread.getRequestResponseOuts().add(op_link);
                }
            }
        }
    }

    // Ensemble des threads d'aiguillage résultant de la répartition des
    // opérations
    public Collection<Dispatch> getDispatchThreads() {
        
        Collection<Dispatch> result = this.dispatchThreads.values();

        // Tri des éléments 'dispatchedLinks' et 'serviceOuts' de chaque thread
        // par identifiant d'opération croissant. On en profite pour supprimer
        // les doublons.
        for (Dispatch dispatchThread : result) {
            sort(dispatchThread.getDispatchedLinks());
            sort(dispatchThread.getRequestResponseOuts());
        }

        return result;
    }

    private void sort(Collection<DispatchedLink> collection) {

        ArrayList<DispatchedLink> list = new ArrayList<DispatchedLink>(collection);
        collection.clear();

        for (int i = 0; i < list.size(); ++i) {
            DispatchedLink ei = list.get(i);

            for (int j = i + 1; j < list.size(); ++j) {
                DispatchedLink ej = list.get(j);

                if (ei.getId() == ej.getId()) {
                    if (ei.getExternalId() == ej.getExternalId()) {
                        list.remove(j);
                        j--;
                    }
                } else if (ei.getId() > ej.getId()) {
                    list.set(i, ej);
                    list.set(j, ei);
                    ei = ej; // car ei va continuer à servir
                }
            }
        }
        collection.addAll(list);
    }

    private Dispatch getDispatchThread(String pProducerName) {
        Dispatch result = this.dispatchThreads.get(pProducerName);

        if (result == null) {
            result = new QDispatch();
            result.setStack(Constants.SARC_DEFAULT_THREAD_STACK_SIZE);
            result.setChannelName(pProducerName);
            this.dispatchThreads.put(pProducerName, result);
        }

        return result;
    }

}
