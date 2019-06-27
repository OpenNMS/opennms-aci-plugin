/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.plugins.aci.commands;

import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.plugins.aci.ApicClusterManager;
import org.opennms.plugins.aci.ApicService;

/**
 * @author tf016851
 *
 */
@Command(scope = "aci", name = "cluster-info", description="Displays ACI Cluster configuration information.")
@Service
public class ClusterInfoCommand implements Action {
    
    @Argument(required=false, name="cluster-name", description="The name of the cluster to show the information for.")
    private String clusterName = null;

    /* (non-Javadoc)
     * @see org.apache.karaf.shell.api.action.Action#execute()
     */
    @Override
    public Object execute() throws Exception {
        System.out.println("ApicService Info:");

        Map<String, ApicClusterManager> cms = ApicService.getClusterManagers();

        if (cms == null || cms.size() < 1)
            System.out.println("\tNo APIC clusters running.");
        else {
            for (String clusterName : cms.keySet()) {
                ApicClusterManager apicClusterManager = cms.get(clusterName);
                if ( clusterName == null || clusterName.equals(apicClusterManager.clusterName)) {
                    apicClusterManager.printConfig();
                    System.out.println();
                }
            }
        }
        return null;
    }

}
