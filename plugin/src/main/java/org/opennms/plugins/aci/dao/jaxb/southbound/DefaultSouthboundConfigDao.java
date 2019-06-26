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

package org.opennms.plugins.aci.dao.jaxb.southbound;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;

import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthboundConfiguration;
import org.opennms.plugins.aci.dao.southbound.SouthboundConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessResourceFailureException;

/**
* DefaultSouthboundConfigDao
*
* @author metispro
*
*/
public class DefaultSouthboundConfigDao extends AbstractJaxbConfigDao<SouthboundConfiguration, SouthboundConfiguration> implements SouthboundConfigDao {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSouthboundConfigDao.class);

    /**
     * @throws MalformedURLException
     */
    public DefaultSouthboundConfigDao() {
        super(SouthboundConfiguration.class, "Southbound Controlller Config");
        Resource configResource = null;
        try {
            configResource = new UrlResource(Paths.get(System.getProperty("opennms.home"), "etc", "southbound-configuration.xml").toUri());
            LOG.debug("Setting configResource: " + configResource.getFilename());
            this.setConfigResource(configResource);
            this.afterPropertiesSet();
        } catch (Exception e) {
            LOG.warn("Error loading resource: " + configResource.toString(), e);
        }
    }

    public DefaultSouthboundConfigDao(Resource configResource) {
        super(SouthboundConfiguration.class, "Southbound Controlller Config");
        try {
            LOG.debug("Setting configResource: " + configResource.getFilename());
            this.setConfigResource(configResource);
            this.afterPropertiesSet();
        } catch (Exception e) {
            LOG.warn("Error loading resource: " + configResource.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.southbound.api.SouthboundConfigDao#getSouthboundConfig()
     */
    public SouthboundConfiguration getSouthboundConfig() {
        return getContainer().getObject();
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.southbound.api.SouthboundConfigDao#getSouthboundCluster()
     */
    public List<SouthCluster> getSouthboundClusters() {
        return getContainer().getObject().getSouthClusters();
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.southbound.api.SouthboundConfigDao#reloadConfiguration()
     */
    @Override
    public void reloadConfiguration()
            throws DataAccessResourceFailureException {
        getContainer().reload();
    }

    @Override
    protected SouthboundConfiguration translateConfig(
            SouthboundConfiguration config) {
        return config;
    }
}
