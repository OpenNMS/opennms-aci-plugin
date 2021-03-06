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
 *
 */
@XmlRootElement(name = "southbound-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("southbound-configuration.xsd")
public class SouthboundConfiguration implements Serializable {

    private static final long serialVersionUID = 2L;
    
    @XmlElement(name = "south-cluster")
    private List<SouthCluster> m_southCluster = new ArrayList<SouthCluster>();

    public List<SouthCluster> getSouthClusters() {
        return m_southCluster;
    }

    public void setSouthCluster(List<SouthCluster> southCluster) {
        this.m_southCluster = southCluster;
    }
    
    public void addSouthCluster(SouthCluster southCluster) {
        this.m_southCluster.add(southCluster);
    }
    
    public void removeSouthCluster(SouthCluster southCluster) {
        this.m_southCluster.remove(southCluster);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.m_southCluster);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SouthboundConfiguration) {
            SouthboundConfiguration other = (SouthboundConfiguration) obj;
            return Objects.equals(this.m_southCluster, other.m_southCluster);
        }
        return false;
    }

}
