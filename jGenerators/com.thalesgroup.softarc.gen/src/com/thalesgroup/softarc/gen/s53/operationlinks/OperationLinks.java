/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s53.operationlinks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import java.io.IOException;

import com.thalesgroup.softarc.gen.s50.thread.ExternalOperationInfo;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.EntryPoint;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Instance;
import com.thalesgroup.softarc.sf.Link;
import com.thalesgroup.softarc.sf.OperationLink;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Port;
import com.thalesgroup.softarc.sf.PortData;
import com.thalesgroup.softarc.sf.PortEvent;
import com.thalesgroup.softarc.sf.PortRequestResponse;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.ThreadBase;
import com.thalesgroup.softarc.sf.impl.QOperationLink;
import com.thalesgroup.softarc.sf.impl.QRouteExtern;
import technology.ecoa.model.deployment.IOInterface;
import technology.ecoa.model.deployment.IOOperation;
import technology.ecoa.model.deployment.IOSection;

public class OperationLinks extends AbstractPass {

    @Override
    public void execute() throws IOException {
        for (EventLink link : context.system.getAssembly().getEventLinks()) {

            for (EventLinkSender sender : link.getSenders()) {
                QOperationLink ol = createOperationLink(link, sender.getPort());
                ol.setEventLink(link);
                ol.setEvent(sender.getPort().getEvent());
                ol.getWhenconditions().addAll(sender.getWhenconditions());
                ol.getDestinationThreads().addAll(
                        link.getReceivers().stream()
                                .map(receiver -> Optional.ofNullable(receiver)
                                        .map(EventLinkSender::getPort)
                                        .map(Port::getInstance)
                                        .map(Instance::getThread)
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                );
                for (ExternalOperationInfo info : getOutChannelsForLink(link)) {
                    QRouteExtern extern = new QRouteExtern();
                    extern.externalId = Long.parseLong(info.operation.getId());
                    extern.outChannelName = info.channel.getName();
                    ol.getExternalRoutes().add(extern);
                }
                sender.getPort().getInstance().getSentEventLinks().add(ol);
                // Use callbackId in eventLink to differentiate the timer event sent
                // to itself and to the receiver
                ol.setCallbackId(ol.getId() + 1);
            }

            for (EventLinkReceiver receiver : link.getReceivers()) {
                QOperationLink ol = createOperationLink(link, receiver.getPort());
                ol.setEventLink(link);
                ol.setEvent(receiver.getPort().getEvent());
                ol.setIsActivating(receiver.getActivating());
                ol.setIsReceivedEvent(true);
                ol.getWhenconditions().addAll(receiver.getWhenconditions());
                receiver.getPort().getInstance().getReceivedEventLinks().add(ol);
            }
        }

        for (DataLink link : context.system.getAssembly().getDataLinks()) {
            LinkedHashMap<PortData, OperationLink> readersMap = new LinkedHashMap<>();

            // attention: on peut être reader et writer à la fois
            for (DataLinkElement reader : link.getReaders()) {
                QOperationLink ol = createOperationLink(link, reader.getPort());
                ol.setDataLink(link);
                ol.setData(reader.getPort().getData());
                ol.getWhenconditions().addAll(reader.getWhenconditions());
                ol.setIsDataRead(true);
                reader.getPort().getInstance().getReadDataLinks().add(ol);
                readersMap.put(reader.getPort(), ol);
            }

            for (DataLinkElement writer : link.getWriters()) {
                OperationLink ol = readersMap.get(writer.getPort());
                if (ol == null) {
                    ol = createOperationLink(link, writer.getPort());
                    ol.setDataLink(link);
                    ol.setData(writer.getPort().getData());
                }

                if (writer.getPort().getReference() == link) {
                    ol.setIsReference(true);
                }
                ol.getWhenconditions().addAll(writer.getWhenconditions());
                ThreadBase ownThread = ol.getPort().getInstance().getThread();
                ol.getDestinationThreads().addAll(
                        link.getReaders().stream()
                                .map(receiver -> Optional.ofNullable(receiver)
                                        .map(DataLinkElement::getPort)
                                        .map(Port::getInstance)
                                        .map(Instance::getThread)
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .filter(thread -> thread != ownThread)
                                .collect(Collectors.toList())
                );
                for (ExternalOperationInfo info : getOutChannelsForLink(link)) {
                    QRouteExtern extern = new QRouteExtern();
                    extern.externalId = Long.parseLong(info.operation.getId());
                    extern.outChannelName = info.channel.getName();
                    ol.getExternalRoutes().add(extern);
                }
                writer.getPort().getInstance().getWrittenDataLinks().add(ol);
            }

            link.getThreadsLinked().addAll(link.getReaders().stream()
                    .map(receiver -> receiver.getPort().getInstance().getThread()).collect(Collectors.toList()));

            link.getThreadsLinked()
                    .addAll(link.getReaders().stream().filter(receiver -> receiver.getPort().getInstance().getIsExtern())
                            .map(receiver -> receiver.getPort().getInstance().getExternalThread()).collect(Collectors.toList()));

            link.getThreadsLinked().addAll(link.getWriters().stream()
                    .map(receiver -> receiver.getPort().getInstance().getThread()).collect(Collectors.toList()));

            link.getThreadsLinked()
                    .addAll(link.getWriters().stream().filter(receiver -> receiver.getPort().getInstance().getIsExtern())
                            .map(receiver -> receiver.getPort().getInstance().getExternalThread()).collect(Collectors.toList()));
        }

        for (RequestResponseLink link : context.system.getAssembly().getRequestResponseLinks()) {

            RequestResponseLinkReceiver client = link.getClient();

            for (RequestResponseLinkReceiver server : link.getServers()) {
                QOperationLink olClient = createOperationLink(link, client.getPort());
                olClient.setRequestResponseLink(link);
                olClient.setRequestResponse(client.getPort().getRequestResponse());
                olClient.setClient(client.getPort().getInstance());
                olClient.setServer(server.getPort().getInstance());
                olClient.setIsActivating(client.getActivating());
                olClient.setXmlID(olClient.getXmlID() + '/' + olClient.getServer().getName());
                olClient.setIsRequiredRequestResponse(true);
                olClient.setIsAnswer(0);
                olClient.getWhenconditions().addAll(client.getWhenconditions());
                if(server.getPort().getInstance().getIsExtern()) {
                    for (ExternalOperationInfo info : getOutChannelsForLink(link)) {
                        QRouteExtern extern = new QRouteExtern();
                        extern.externalId = Long.parseLong(info.operation.getId());
                        extern.outChannelName = info.channel.getName();
                        olClient.getExternalRoutes().add(extern);
                    }
                } else {
                    olClient.getDestinationThreads().add(server.getPort().getInstance().getThread());
                }
                client.getPort().getInstance().getRequiredRequestResponsesLinks().add(olClient);

                QOperationLink olServer = createOperationLink(link, server.getPort());
                olServer.setRequestResponseLink(link);
                olServer.setRequestResponse(server.getPort().getRequestResponse());
                olServer.setClient(client.getPort().getInstance());
                olServer.setIsActivating(server.getActivating());
                olServer.setIsProvidedRequestResponse(true);
                olServer.setIsAnswer(1);
                olServer.getWhenconditions().addAll(server.getWhenconditions());
                if(client.getPort().getInstance().getIsExtern()) {
                    for (ExternalOperationInfo info : getOutChannelsForLink(link)) {
                        QRouteExtern extern = new QRouteExtern();
                        extern.externalId = Long.parseLong(info.operation.getId());
                        extern.outChannelName = info.channel.getName();
                        olServer.getExternalRoutes().add(extern);
                    }
                } else {
                    if(client.getPort().getInstance().getThread() != null) {
                        olServer.getDestinationThreads().add(client.getPort().getInstance().getThread());
                    }
                }
                // Callback for request response is alsways id + 1
                // cf: createSortedOperations in Containers.java
                olServer.setCallbackId(link.getId() + 1);
                server.getPort().getInstance().getProvidedRequestResponsesLinks().add(olServer);
            }

        }

        for (Instance instance : context.system.getAssembly().getInstances()) {
            for (OperationLink ol : instance.getRequiredRequestResponsesLinks()) {
                resolveRequiredRequestResponse(ol);
            }
        }
    }

    private QOperationLink createOperationLink(Link link, Port port) {
        QOperationLink ol = new QOperationLink();
        ol.setId(link.getId());
        ol.setXmlID("oplink:" + port.getInstance().getName() + '/' + port.getOperation().getName() + '/' + link.getId());
        ol.setParent(port.getInstance());
        ol.setPort(port);
        port.setIsLinked(true);
        port.getInstance().getAllLinks().add(ol);
        return ol;
    }

    private void resolveRequiredRequestResponse(OperationLink ol) throws IOException {
        assert ol.getIsRequiredRequestResponse();
        /*
         * Note: Ce code porté de l'ancien GenMain devrait probablement être regroupé avec Containers.fillMap()
         */
        int rank = 0;
        for (RequestResponseLinkReceiver slr : ol.getRequestResponseLink().getServers()) {
            Instance server = slr.getPort().getInstance();

            // Server is the first of the list
            if (rank == 0) {
                ol.setServer(server);
                detectDirectRequestResponseCall(ol);
            }
            // Otherwise, this is a redundant server. Let's create a virtual link
            // to represent this relation (one remind that service links are 1-1)
            else {
                OperationLink failoverlink = createOperationLink(ol.getRequestResponseLink(), ol.getPort());
                failoverlink.setXmlID(ol.getXmlID() + '/' + rank);
                failoverlink.setServer(server);
                failoverlink.setRequestResponseLink(ol.getRequestResponseLink());
                failoverlink.setRequestResponse(ol.getRequestResponse());
                failoverlink.setClient(ol.getClient());
                failoverlink.setIsActivating(ol.getIsActivating());
                failoverlink.setIsRequiredRequestResponse(true);
                detectDirectRequestResponseCall(failoverlink);
                ol.getFailoverLinks().add(failoverlink);
            }

            rank++;
        }
    }

    private void detectDirectRequestResponseCall(OperationLink ol) throws IOException {
        // Server and client are running in the same thread and client calls the
        // server synchronously : it is a direct server!
        if (!ol.getRequestResponse().getIsAsynchronous() && ol.getServer().getThread() == ol.getClient().getThread()) {

            ol.setIsDirect(true);
            // update the corresponding OperationLink in server
            Instance server = ol.getServer();
            for (OperationLink serverLink : server.getProvidedRequestResponsesLinks()) {
                if (serverLink.getId() == ol.getId()) {

                    if (serverLink.getRequestResponse().getIsDeferred()) {
                        errorModel(
                                "Deadlock detected: deferred service '%s' provided by '%s' is linked to a synchronous service required by '%s', and both instances are in the same thread. "
                                        + "Solution is to make it immediate, or not synchronous, or to deploy the 2 instances in different tasks.",
                                serverLink.getRequestResponse().getName(), server.getName(), serverLink.getClient().getName());
                    }

                    // copy operation name of server side to the client side (server operation name
                    // will be used in the client's
                    // container)
                    ol.setOperationInServer((OperationRequestResponse) serverLink.getPort().getOperation());

                    serverLink.setIsDirect(true);
                    // update the list of direct service entry points in the server
                    for (EntryPoint ep : server.getRequestResponseEntryPoints()) {
                        if (ep.getPort() == serverLink.getPort()) {
                            server.getDirectRequestResponseEntryPoints().add(ep);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<ExternalOperationInfo> getOutChannelsForLink(Link link) {

        ArrayList<ExternalOperationInfo> result = new ArrayList<>();

        if (link instanceof DataLink) {
            DataLink l = (DataLink) link;
            for (DataLinkElement le : l.getReaders()) {
                PortData port = le.getPort();
                if (port.getInstance().getIsExtern()) {
                    result.addAll(getOutChannelsForOperation(port.getOperation().getName(), null));
                }
            }
        }
        if (link instanceof EventLink) {
            EventLink l = (EventLink) link;
            for (EventLinkReceiver le : l.getReceivers()) {
                PortEvent port = le.getPort();
                if (port.getInstance().getIsExtern()) {
                    result.addAll(getOutChannelsForOperation(port.getOperation().getName(), null));
                }
            }
        }
        if (link instanceof RequestResponseLink) {
            RequestResponseLink l = (RequestResponseLink) link;
            for (RequestResponseLinkReceiver le : l.getServers()) {
                PortRequestResponse port = le.getPort();
                if (port.getInstance().getIsExtern()) {
                    result.addAll(getOutChannelsForOperation(port.getOperation().getName(), le.getOutChannelName()));
                }
            }
            {
                RequestResponseLinkReceiver le = l.getClient();
                PortRequestResponse port = le.getPort();
                if (port.getInstance().getIsExtern()) {
                    result.addAll(getOutChannelsForOperation(port.getOperation().getName(), le.getOutChannelName()));
                }
            }
        }
        return result;
    }

    private ArrayList<ExternalOperationInfo> getOutChannelsForOperation(String operationName, String preDeterminedChannelName) {
        return getExternalChannelsForOperation(operationName, preDeterminedChannelName, false);
    }

    private ArrayList<ExternalOperationInfo> getExternalChannelsForOperation(String operationName, String preDeterminedChannelName, boolean isIn) {

        ArrayList<ExternalOperationInfo> result = new ArrayList<ExternalOperationInfo>();

        IOSection externalIo = context.DEFILE.getExternalIo();
        if (externalIo != null) {
            List<IOInterface> channels = new ArrayList<>(externalIo.getInOutPort());
            if (isIn)
                channels.addAll(externalIo.getInPort());
            else
                channels.addAll(externalIo.getOutPort());

            for (IOInterface channel : channels) {
                for (IOOperation op : channel.getOperation()) {
                    if (op.getName().equals(operationName) && (preDeterminedChannelName == null || preDeterminedChannelName.isEmpty() || preDeterminedChannelName.equals(channel.getName()))) {
                        ExternalOperationInfo info = new ExternalOperationInfo();
                        info.channel = channel;
                        info.operation = op;
                        result.add(info);
                    }
                }
            }
        }
        return result;
    }
}
