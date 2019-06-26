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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.requisition.RequisitionRequest;

public class AciRequistionRequest implements RequisitionRequest {

    public static final List<String> DEFAULT_SERVICES = Arrays.asList("ICMP",
            "SSH");

    private String hostname = null;

    private String username = null;

    private String password = null;

    private String clusterName = null;

    private String location = null;

    // unique cluster name
    private String foreignSource = null;

    private String apicUrl = null;

    private List<String> services;

    private Requisition existingRequisition;

    public AciRequistionRequest(Map<String, String> parameters) {
        if (parameters.get("hostname") != null)
            setHostname(parameters.get("hostname"));

        if (parameters.get("username") != null)
            setUsername(parameters.get("username"));

        if (parameters.get("password") != null)
            setPassword(parameters.get("password"));

        if (parameters.get("apic-url") != null)
            setApicUrl(parameters.get("apic-url"));

        if (parameters.get("location") != null)
            setLocation(parameters.get("location"));

        setClusterName(parameters.get("cluster-name"));

        if (clusterName == null) {
            throw new IllegalArgumentException("cluster-name is required");
        }

        //Use the clusterName as the foreign source
        if (!clusterName.equals(foreignSource))
            setForeignSource(clusterName);

    }

    public AciRequistionRequest() {}

    public void setApicUrl(String apicUrl) {
        this.apicUrl = apicUrl;
    }

    public AciRequistionRequest apicUrl(String apicUrl) {
        this.apicUrl = apicUrl;
        return this;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public AciRequistionRequest clusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AciRequistionRequest password(String password) {
        this.password = password;
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AciRequistionRequest username(String username) {
        this.username = username;
        return this;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;

    }

    public AciRequistionRequest hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setForeignSource(String foreignSource) {
        this.foreignSource = foreignSource;

    }

    public String getForeignSource() {
        return foreignSource;
    }

    public String getApicUrl() {
        return apicUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setExistingRequisition(Requisition existingRequisition) {
        this.existingRequisition = existingRequisition;
    }

    /**
     * @return the existingRequisition
     */
    public Requisition getExistingRequisition() {
        return existingRequisition;
    }

    public List<String> getServices() {
        return services != null ? services : DEFAULT_SERVICES;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
