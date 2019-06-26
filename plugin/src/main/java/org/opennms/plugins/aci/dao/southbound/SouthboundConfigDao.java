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

package org.opennms.plugins.aci.dao.southbound;

import java.util.List;

import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthboundConfiguration;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Dao interface for all Southbound controller fault collection configurations.
 * @author tf016851
 *
 */
public interface SouthboundConfigDao {
    
    /**
     * <p>getSouthboundConfig</p>
     * 
     * @return a {@link SouthboundConfiguration} object.
     */
    SouthboundConfiguration getSouthboundConfig();
    
    /**
     * <p>getSouthboundCluster</p>
     * 
     * @return a {@link SouthCluster} object.
     */
    List<SouthCluster> getSouthboundClusters();

    /**
     * The underlying XML-based DAO abstraction in the default implementation doesn't provide access to the container so
     * this method is defined so that access to the container doesn't have to be exposed and a reload can still be controlled
     * by the user.
     *
     * Automatically reading in new values if the file changes is a different use case from expecting the services to alter
     * their state based on a configuration change.  This method will most likely be used with event processing and possibly
     * in the ReST API.
     *
     * @throws DataAccessResourceFailureException if any.
     */
    void reloadConfiguration() throws DataAccessResourceFailureException;
}
