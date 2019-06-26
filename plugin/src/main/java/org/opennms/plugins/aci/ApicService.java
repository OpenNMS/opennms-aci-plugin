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

package org.opennms.plugins.aci;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.dao.southbound.SouthboundConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author metispro
 */
public class ApicService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicService.class);

    static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    public static final String DN_SEP = "/";
    
    public static final String FS_SEP = "~";
    
    final int pollDurationMinutes = 5;

    public final static String APIC_CONFIG_LOCATION_KEY = "Location";
    public final static String APIC_CONFIG_URL_KEY = "URL";
    public final static String APIC_CONFIG_USERNAME_KEY = "Username";
    public final static String APIC_CONFIG_PASSWORD_KEY = "Password";
    public final static String APIC_CONFIG_POLL_DURATION_KEY = "pollDuration";
    public final static String APIC_CONFIG_EVENT_FORWARDER = "EventForwarder";
    public final static String APIC_CONFIG_EVENT_DAO = "EventDao";
    public final static String APIC_CONFIG_NODE_CACHE = "NodeCache";
    public final static String APIC_CONFIG_CLUSTER_MAP = "ClusterMap";
    public final static String APIC_CONFIG_LOCAL_ADDR = "localAddr";
    
    public final static String APIC_CLUSTER_MAP_LAST_PROCESS_TIME = "lastProcessTime";
    
    private EventForwarder eventForwarder;
    
    private NodeDao nodeDao;

    private SouthboundConfigDao southboundConfigDao;
    
    private static ApicServiceManager apicServiceManager;

    public ApicService(SouthboundConfigDao southboundConfigDao, EventForwarder eventForwarder, NodeDao nodeDao) {
        this.southboundConfigDao = southboundConfigDao;
        this.eventForwarder = eventForwarder;
        this.nodeDao = nodeDao;
    }

    public void init() {
        LOG.info("ACI: Initializaing ApicService ...");

        List<SouthCluster> clusters = this.southboundConfigDao.getSouthboundClusters();
        apicServiceManager = new ApicServiceManager(eventForwarder, nodeDao, clusters);
        apicServiceManager.start();
        
        LOG.info("ACI: Finished initializing ApicService");
    }

    public void destroy() {

        LOG.info("ACI: Destorying ApicService ...");

        if (apicServiceManager != null)
            apicServiceManager.shutdown();
        
        long waitStart = System.currentTimeMillis();
        while(apicServiceManager.isAlive()) {
            LOG.info("ACI: Gracefully shutting down ...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            if ((System.currentTimeMillis() - waitStart) >= 30000) {
                //If still running after 30 seconds, forcefully stop
                LOG.warn("ACI: Forcefully stopping ApicServiceManager");
                apicServiceManager.interrupt();
            }
        }

        LOG.info("ACI: Service stopped");
    }
    
    public static Map<String, ApicClusterManager> getClusterManagers() {
        return ApicServiceManager.clusterManagers;
    }

    /**
     * @return the eventForwarder
     */
    public EventForwarder getEventForwarder() {
        return eventForwarder;
    }

    /**
     * @param eventForwarder the eventForwarder to set
     */
    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    /**
     * @return the apicServiceManager
     */
    public static ApicServiceManager getApicServiceManager() {
        return apicServiceManager;
    }

}
