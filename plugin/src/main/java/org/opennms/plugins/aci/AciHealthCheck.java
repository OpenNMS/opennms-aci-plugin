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

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.integration.api.v1.health.Context;
import org.opennms.integration.api.v1.health.HealthCheck;
import org.opennms.integration.api.v1.health.Response;
import org.opennms.integration.api.v1.health.Status;
import org.opennms.integration.api.v1.health.immutables.ImmutableResponse;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.opennms.plugins.aci.client.ACIRestConfig;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.dao.southbound.SouthboundConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AciHealthCheck implements HealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(AciHealthCheck.class);

    private static final String DESCRIPTION = "Check APIC Controller connectivity";

    private final SouthboundConfigDao configDao;

    public AciHealthCheck(SouthboundConfigDao configDao) {
        this.configDao = configDao;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Response perform(Context context) {
        // Loop on all configured clusters and test connectivity
        List<SouthCluster> clusters = configDao.getSouthboundClusters();
        List<String> successClusters = new ArrayList<>();
        List<String> failureClusters = new ArrayList<>();
        int topCount = 0;
        for (SouthCluster cluster : clusters) {
            ACIRestConfig config = new ACIRestConfig(cluster);
            try {
                ACIRestClient client = ACIRestClient.newAciRest(config.getClusterName(), config.getAciUrl(),
                        config.getUsername(), config.getPassword());
                JSONArray results = null;
                try {
                    results = client.getClassInfo( "topSystem" );
                    if (results == null || hasError(results)) {
                        failureClusters.add(cluster.getClusterName());
                    } else {
                        successClusters.add(cluster.getClusterName());
                        topCount += results.size();
                    }
                } catch (Exception e) {
                    failureClusters.add(cluster.getClusterName());
                }
            } catch (Exception e) {
                return ImmutableResponse.newInstance(e);
            }
        }

        if (failureClusters.size() > 0)
            return ImmutableResponse.newInstance(Status.Failure, "Failed to connect and retrieve topSystems");

        return ImmutableResponse.newInstance(Status.Success, String.format("Found %d topSystem(s).", topCount));
    }

    private boolean hasError(JSONArray results) {
        for (Object object : results) {
            JSONObject objectData = (JSONObject) object;
            if (objectData == null)
                continue;

            if (objectData.get("error") != null)
                return true;
        }
        return false;
    }
}
