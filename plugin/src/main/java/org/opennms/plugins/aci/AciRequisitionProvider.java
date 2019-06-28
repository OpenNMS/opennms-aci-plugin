/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.aci;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionInterface;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionMetaData;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionNode;
import org.opennms.integration.api.v1.requisition.RequisitionProvider;
import org.opennms.integration.api.v1.requisition.RequisitionRepository;
import org.opennms.integration.api.v1.requisition.RequisitionRequest;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthElement;
import org.opennms.plugins.aci.dao.southbound.SouthboundConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AciRequisitionProvider implements RequisitionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AciRequisitionProvider.class);
    private static final String TYPE = "aci";
    public static final String DEFAULT_FOREIGN_SOURCE = "ACI";
    public static final String METADATA_CONTEXT_ID = "ACI";
    public static final String ACI_CLUSTER_TYPE = "CISCO-ACI";

    // TODO: Make this configurable
    public static final InetAddress NON_RESPONSIVE_IP_ADDRESS;
    static {
        try {
            // Addresses in the 192.0.2.0/24 block are reserved for documentation and should not respond to anything
            NON_RESPONSIVE_IP_ADDRESS = InetAddress.getByName("192.0.2.0");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private final RequisitionRepository requisitionRepository;

    private final SouthboundConfigDao southboundConfigDao;

    public AciRequisitionProvider(SouthboundConfigDao southboundConfigDao, RequisitionRepository requisitionRepository) {
        this.southboundConfigDao = southboundConfigDao;
        this.requisitionRepository = requisitionRepository;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequisitionRequest getRequest(Map<String, String> parameters) {

        AciRequistionRequest request = new AciRequistionRequest(parameters);

        List<SouthCluster> clusters = this.southboundConfigDao.getSouthboundClusters();
        for (SouthCluster southCluster : clusters) {
            if (southCluster.getClusterType().equals(ACI_CLUSTER_TYPE) &&
                    request.getForeignSource().equals(southCluster.getClusterName())) {
                //Build URL
                String url = "";
                String username = "";
                String password = "";
                String location = southCluster.getLocation();

                if (location != null)
                    request.setLocation(location);

                List<SouthElement> elements = southCluster.getElements();
                for (SouthElement element : elements ){
                    url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
                    username = element.getUserid();
                    password = element.getPassword();
                }
                // We found a corresponding entry - copy the credentials to the request
                request.setApicUrl(url);
                request.setUsername(username);
                request.setPassword(password);
                request.setClusterName(southCluster.getClusterName());
            }
        }

        return request;
    }

    @Override
    public Requisition getRequisition(RequisitionRequest requisitionRequest) {
        final AciRequistionRequest request = (AciRequistionRequest) requisitionRequest;

        Requisition curReq = requisitionRepository.getDeployedRequisition(DEFAULT_FOREIGN_SOURCE);

        ACIRestClient client = null;
        try {
            client = ACIRestClient.newAciRest(request.getForeignSource(), request.getApicUrl(), request.getUsername(),
                    request.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Initialize RequisitionBuilder with foreignSource
        String foreignSource = DEFAULT_FOREIGN_SOURCE;
        if (((AciRequistionRequest) requisitionRequest).getClusterName() != null)
            foreignSource = ((AciRequistionRequest) requisitionRequest).getClusterName();
        final ImmutableRequisition.Builder requisitionBuilder = ImmutableRequisition.newBuilder()
                .setForeignSource(foreignSource);

        //First add all the apic nodes in this cluster.
        List<SouthCluster> clusters = this.southboundConfigDao.getSouthboundClusters();
        for (SouthCluster southCluster : clusters) {
            if (southCluster.getClusterType().equals(ACI_CLUSTER_TYPE) &&
                    request.getForeignSource().equals(southCluster.getClusterName())) {
                String building = request.getForeignSource();
                for (SouthElement southElement : southCluster.getElements()) {
                    InetAddress inetAddress = NON_RESPONSIVE_IP_ADDRESS;

                    try {
                        //If we have an IP Address String from JSON, then try and create InetAddress object
                        inetAddress = InetAddress.getByName(southElement.getHost());
                    } catch (UnknownHostException e) {
                        LOG.warn("ACI: Invalid InetAddress for: " + southElement.getHost(), e);
                    }
                    ImmutableRequisitionNode.Builder builder = ImmutableRequisitionNode.newBuilder();

                    builder.setForeignId(southElement.getHost())
                            .setNodeLabel(southElement.getHost());

                    ImmutableRequisitionInterface ipv4InterfaceBuilder = ImmutableRequisitionInterface.newBuilder()
                            .setIpAddress(inetAddress)
                            .setDescription("ACI-" + southElement.getHost())
                            .build();

                    builder.addInterface(ipv4InterfaceBuilder)
                            .addAsset("latitude", "45.340561")
                            .addAsset("longitude", "-75.910005")
                            .addAsset("building", building);

                    requisitionBuilder.addNode(builder.build());
                }
            }
        }

        //Next, query all topology systems and add
        LOG.debug("sending get for top system");
        JSONArray results = null;
        try {
            results = client.getClassInfo( "topSystem" );
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Object object : results) {
            JSONObject objectData = (JSONObject) object;
            if (objectData == null)
                continue;
            LOG.debug("total count " + (String) objectData.get("totalCount"));
            for (Object object2 : objectData.keySet()) {
                String key = (String) object2;
                JSONObject classData = (JSONObject) objectData.get(key);
                JSONObject attributes = (JSONObject) classData.get("attributes");
                if (attributes == null)
                    continue;


                String dn = (String) attributes.get("dn");
                String[] dnParts = dn.split("/");
                dn = dnParts[0] + "_" + dnParts[1] + "_" + dnParts[2];

                String nodeId = (String) attributes.get("name");

                String building = request.getForeignSource();
                String locationCategory = null;

                if (request.getLocation() != null) {
                    building = request.getLocation();
                    locationCategory = request.getLocation();
                }

                String role = (String) attributes.get("role");

                String ipAddr = (String) attributes.get("oobMgmtAddr");
                String ipAddr6 = (String) attributes.get("oobMgmtAddr6");
                InetAddress inetAddress = NON_RESPONSIVE_IP_ADDRESS;
                InetAddress inetAddress6 = null;

                try {
                    //If we have an IP Address String from JSON, then try and create InetAddress object
                    if (ipAddr != null)
                        inetAddress = InetAddress.getByName(ipAddr);
                } catch (UnknownHostException e) {
                    LOG.warn("ACI: Invalid InetAddress for: " + ipAddr, e);
                }

                try {
                    //If we have an IP Address String from JSON, then try and create InetAddress object
                    if (ipAddr6 != null)
                        inetAddress6 = InetAddress.getByName(ipAddr6);
                } catch (UnknownHostException e) {
                    LOG.warn("ACI: Invalid InetAddress for: " + ipAddr6, e);
                }

                ImmutableRequisitionNode.Builder builder = ImmutableRequisitionNode.newBuilder();

                builder.setForeignId(dn)
                        .setNodeLabel(dn)
                        .addCategory(locationCategory)
                        .addCategory(METADATA_CONTEXT_ID)
                        .addCategory(role);

                for (Object k:  attributes.keySet()) {
                    String attributeKey = (String)k;

                    if (attributes.get(k) instanceof String) {
                        ImmutableRequisitionMetaData nodeId1 = ImmutableRequisitionMetaData.newBuilder()
                                .setContext(METADATA_CONTEXT_ID)
                                .setKey(attributeKey)
                                .setValue((String)attributes.get(k))
                                .build();

                        builder.addMetaData(nodeId1);
                    }
                }

                ImmutableRequisitionInterface ipv4InterfaceBuilder = ImmutableRequisitionInterface.newBuilder()
                        .setIpAddress(inetAddress)
                        .setDescription("ACI-" + (String) attributes.get("dn"))
                        .build();

                if (inetAddress6 != null) {

                    ImmutableRequisitionInterface ipv6InterfaceBuilder = ImmutableRequisitionInterface.newBuilder()
                        .setIpAddress(inetAddress6)
                        .setDescription("ACI-" + (String) attributes.get("dn"))
                        .build();

                    builder.addInterface(ipv6InterfaceBuilder);
                 }

                builder.addInterface(ipv4InterfaceBuilder)
                        .addAsset("latitude", "45.340561")
                        .addAsset("longitude", "-75.910005")
                        .addAsset("building", building);

                requisitionBuilder.addNode(builder.build());

            }
        }

       return requisitionBuilder.build();
    }

    @Override
    public byte[] marshalRequest(RequisitionRequest requisitionRequest) {
        return new byte[0];
    }

    @Override
    public RequisitionRequest unmarshalRequest(byte[] bytes) {
        return new AciRequistionRequest();
    }

}
