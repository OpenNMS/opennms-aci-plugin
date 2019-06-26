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

package org.opennms.plugins.aci.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.plugins.aci.dao.jaxb.southbound.ValidateUsing;

/**
 * @author tf016851
 */
@XmlRootElement(name = "south-cluster")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("southbound-configuration.xsd")
public class SouthCluster implements Serializable {
    
    private static final long serialVersionUID = 2L;

    @XmlElement(name = "cluster-name", required = true)
    private String m_clusterName;
    
    @XmlElement(name = "cluster-type", required = true)
    private String m_clusterType;
    
    @XmlElement(name = "cron-schedule", required = false)
    private String m_cronSchedule;
    
    @XmlElement(name = "location", required = false)
    private String location;

    @XmlElement(name = "poll-duration-minutes", required = false)
    private int pollDurationMinutes;
   
    @XmlElement(name = "south-element")
    private List<SouthElement> m_elements = new ArrayList<SouthElement>();

    public String getClusterName() {
        return m_clusterName;
    }

    public void setClusterName(String clusterName) {
        this.m_clusterName = clusterName;
    }

    public String getClusterType() {
        return m_clusterType;
    }

    public void setClusterType(String clusterType) {
        this.m_clusterType = clusterType;
    }

    public List<SouthElement> getElements() {
        return m_elements;
    }

    public void setElements(List<SouthElement> elements) {
        this.m_elements = elements;
    }
    
    public void addElement(SouthElement element) {
        this.m_elements.add(element);
    }
    
    public void removeElement(SouthElement element) {
        this.m_elements.remove(element);
    }

    public String getCronSchedule() {
        return m_cronSchedule;
    }

    public void setCronSchedule(String cronSchedule) {
        this.m_cronSchedule = cronSchedule;
    }

    public int getPollDurationMinutes() {
        return pollDurationMinutes;
    }

    public void setPollDurationMinutes(int pollDurationMinutes) {
        this.pollDurationMinutes = pollDurationMinutes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.m_clusterName, this.m_elements, this.m_cronSchedule, this.pollDurationMinutes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SouthCluster) {
            SouthCluster other = (SouthCluster) obj;
            return Objects.equals(this.m_clusterName, other.m_clusterName)
                    && Objects.equals(this.m_elements, other.m_elements)
                    && Objects.equals(this.m_cronSchedule, other.m_cronSchedule)
                    && Objects.equals(this.pollDurationMinutes, other.pollDurationMinutes);
        }
        return false;
    }

}
