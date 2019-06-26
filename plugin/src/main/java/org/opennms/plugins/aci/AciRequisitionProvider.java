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
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.config.requisition.SnmpPrimaryType;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionInterface;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionMetaData;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionNode;
import org.opennms.integration.api.v1.requisition.RequisitionProvider;
import org.opennms.integration.api.v1.requisition.RequisitionRepository;
import org.opennms.integration.api.v1.requisition.RequisitionRequest;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AciRequisitionProvider implements RequisitionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AciRequisitionProvider.class);
    private static final String TYPE = "aci";
    public static final String DEFAULT_FOREIGN_SOURCE = "ACI";
    public static final String METADATA_CONTEXT_ID = "ACI";

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

    private RequisitionRepository requisitionRepository;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequisitionRequest getRequest(Map<String, String> parameters) {

        return new AciRequistionRequest(parameters);
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

        final ImmutableRequisition.Builder requisitionBuilder = ImmutableRequisition.newBuilder()
                .setForeignSource(DEFAULT_FOREIGN_SOURCE);

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
                InetAddress inetAddress = NON_RESPONSIVE_IP_ADDRESS;

                try {
                    //If we have an IP Address String from JSON, then try and create InetAddress object
                    if (ipAddr != null)
                        inetAddress = InetAddress.getByAddress(ipAddr.getBytes());
                } catch (UnknownHostException e) {
                    LOG.warn("ACI: Invalid InetAddress for: {}", ipAddr, e);
                }

                requisitionBuilder.addNode(ImmutableRequisitionNode.newBuilder()
                        .setForeignId(dn)
                        .setNodeLabel(nodeId)
                        .addCategory(locationCategory)
                        .addCategory(METADATA_CONTEXT_ID)
                        .addCategory(role)
                        .addMetaData(ImmutableRequisitionMetaData.newBuilder()
                                .setContext(METADATA_CONTEXT_ID)
                                .setKey("nodeId")
                                .setValue(nodeId)
                                .build())
                        .addMetaData(ImmutableRequisitionMetaData.newBuilder()
                                .setContext(METADATA_CONTEXT_ID)
                                .setKey("topologyId")
                                .setValue(dn)
                                .build())
                        .addInterface(ImmutableRequisitionInterface.newBuilder()
                                .setIpAddress(inetAddress)
                                .setDescription("ACI-" + (String) attributes.get("dn"))
                                .setSnmpPrimary(SnmpPrimaryType.PRIMARY)
                                .addMonitoredService("ICMP")
                                .addMonitoredService("SSH")
                                .build())
                        .addAsset("latitude", "45.340561")
                        .addAsset("longitude", "-75.910005")
                        .addAsset("building", building)
                        .build());

//                iface.setManaged(Boolean.TRUE);
//                iface.setStatus(Integer.valueOf(1));
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
