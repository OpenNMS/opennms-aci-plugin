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

import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisition;
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

        requisitionRepository.getDeployedRequisition(DEFAULT_FOREIGN_SOURCE);

        final ImmutableRequisition.Builder requisitionBuilder = ImmutableRequisition.newBuilder()
                .setForeignSource(DEFAULT_FOREIGN_SOURCE);

        ACIRestClient client;
        try {
            client = ACIRestClient.newAciRest(request.getForeignSource(), request.getApicUrl(), request.getUsername(),
                    request.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
