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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthElement;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author metispro
 *
 */
public class ApicServiceManager extends Thread {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicServiceManager.class);
    
    private static final int restartDuration = 14400000; // 6 hours in millis
    
    private final EventForwarder eventForwarder;
    
    private final NodeDao nodeDao;
    
    private final List<SouthCluster> clusters;

    private Scheduler scheduler = null;
    
    private Map<String, Map<String, Object>> clusterMap;
    
    public static Map<String, ApicClusterManager> clusterManagers;
    
    private String localAddr;
    
    private boolean shutdown = false;
    
    public ApicServiceManager(EventForwarder eventForwarder, NodeDao nodeDao, List<SouthCluster> clusters) {
        super();
        this.eventForwarder = eventForwarder;
        this.nodeDao = nodeDao;
        this.clusters = clusters;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (clusters == null || clusters.size() < 1) {
            LOG.warn("No Southbound Clusters configured.");
            return;
        }

        long start = System.currentTimeMillis();
        
        //Initialize service
        init();

        boolean restarted = false;
        while (!shutdown) {
            
            for (SouthCluster southCluster : clusters) {
                if (southCluster.getPollDurationMinutes() == 0) {
                    if (System.currentTimeMillis() - start >= restartDuration) {
                        restarted = true;
                        try {
                            LOG.info("ACI: Restarting Cluster Mangaer for APIC: " + southCluster.getClusterName());
                            this.restartClusterManager(southCluster.getClusterName());
                        } catch (ApicClusterNotFoundException e) {
                            LOG.info("ACI: No Cluster Mangaer found for APIC: " + southCluster.getClusterName());
                        }
                    } else {
                        // Websocket, check thread
                        this.checkClusterManager(southCluster);
                    }

                }
            }
            
            if (restarted) {
                start = System.currentTimeMillis();
                restarted = false;
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        //Destroy Service
        destroy();

    }
    
    /**
     * Helper method for gracefully stopping a Apic Cluster Manager for given cluster name.
     * @param clusterName
     * @throws ApicClusterNotFoundException
     */
    public void stopClusterManager(String clusterName) throws ApicClusterNotFoundException {
        if (clusterName == null)
            return;
        
        ApicClusterManager clusterManagerThread = clusterManagers.get(clusterName);
        
        if (clusterManagerThread == null) {
            throw new ApicClusterNotFoundException("ACI: APIC cluster manageer thread not found for: " + clusterName);
        }
        
        LOG.info("ACI: Stopping clusterManager: {}", clusterName);
        clusterManagerThread.shutdown();
    }
    
    /**
     * Helper method for starting an APIC cluster manager for given cluster name.
     * @param clusterName
     * @throws ApicClusterNotFoundException
     */
    public void startClusterManager(String clusterName) throws ApicClusterNotFoundException {
        if (clusterName == null)
            return;
        
        SouthCluster southCluster = null;
        for (SouthCluster sc : clusters) {
            if (clusterName.equals(sc.getClusterName())) {
                southCluster = sc;
                break;
            }
        }
        
        if (southCluster == null)
            throw new ApicClusterNotFoundException("ACI: APIC cluster configuration not found for: " + clusterName);
        
        LOG.info("ACI: Starting clusterManager: {}", clusterName);
        ApicClusterManager apicClusterManager = clusterManagers.get(clusterName);

        //We may have an instance that was shutdown, so re-enable it
        if (apicClusterManager != null)
            apicClusterManager.setShutdown(false);

        this.checkClusterManager(southCluster);
    }
    
    /**
     * Helper method for restarting an APIC cluster manager for given cluster name.
     * @param clusterName
     * @throws ApicClusterNotFoundException
     */
    public void restartClusterManager(String clusterName) throws ApicClusterNotFoundException {
        if (clusterName == null)
            return;
       
        this.stopClusterManager(clusterName);
        //Sleep for 5 seconds to give everything time to shutdown
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // DOn't care
        }
        
        //Check and start
        this.startClusterManager(clusterName);
    }
    
    private void checkClusterManager(SouthCluster southCluster) {
        ApicClusterManager clusterManagerThread = null;
        
        clusterManagerThread  = clusterManagers.get(southCluster.getClusterName());
        
        //If we have a cluster manager thread, but was shutdown, then return
        if (clusterManagerThread != null && clusterManagerThread.isShutdown())
            return;
        
        if (clusterManagerThread == null || 
                !clusterManagerThread.isAlive() || !clusterManagerThread.isRunning()) {
            if (clusterManagerThread != null && !clusterManagerThread.isShutdown()) {
                LOG.info("ACI: APIC cluster thread {} is dead ... restarting", southCluster.getClusterName());
                //Let's try cleaning up
                clusterManagerThread.interrupt();
            } else {
                LOG.info("ACI: Starting APIC cluster thread: {}", southCluster.getClusterName());
            }
            NodeCache nodeCache = new NodeCache();
            nodeCache.setNodeDao(nodeDao);
            nodeCache.init();
            ApicEventForwader apicEventForwarder = new ApicEventForwader(eventForwarder, nodeCache);
            try {
                ApicClusterManager apicClusterManager = new ApicClusterManager(apicEventForwarder, southCluster);
                //Create and start thread
                apicClusterManager.start();
                clusterManagers.put(southCluster.getClusterName(), apicClusterManager);
            } catch (Exception e) {
                LOG.error("Error starting ApicClusterManager for cluster: " + southCluster.getClusterName(), e);
                e.printStackTrace();
            }
        } else {
            if (this.shutdown)
                LOG.trace("ACI: Thread for APIC clsuter: {} is shutdown", southCluster.getClusterName());
            else
                LOG.trace("ACI: Thread for APIC clsuter: {} is running", southCluster.getClusterName());
        }
    }
    
    private void init() {
        LOG.debug("ACI: Initializing ApicServiceManager ...");
        try {
            localAddr = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            localAddr = "localhost";
        }
        clusterMap = new HashMap<String, Map<String, Object>>();
        clusterManagers = new HashMap<String, ApicClusterManager>();
        
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
            } catch (SchedulerException e) {
                LOG.debug("Error stopping scheduler.", e);
                e.printStackTrace();
            }
            scheduler = null;
        }
        
        for (SouthCluster southCluster : clusters) {
            if (southCluster.getClusterType().equals("CISCO-ACI")) {
                String location = southCluster.getClusterName();
                LOG.debug("Found ACI Cluster configuration for: " + location);
                String url = "";
                String username = "";
                String password = "";
                List<SouthElement> elements = southCluster.getElements();
                for (SouthElement element : elements) {
                    url += "https://" + element.getHost() + ":"
                            + element.getPort() + ",";
                    username = element.getUserid();
                    password = element.getPassword();
                }
                if (southCluster.getPollDurationMinutes() == 0) {
                    LOG.debug("polldDurationMinutes == 0 ... starting websockets.");
                    this.checkClusterManager(southCluster);
                } else {
                    // Start scheduled data collection.
                    LOG.debug("pollDurationMinutes == {} ... create and schedule job",
                            southCluster.getPollDurationMinutes() );
                    this.createAndScheduleJob(location,
                                              org.apache.commons.lang.StringUtils.chomp(url,
                                                                                        ","),
                                              username, password,
                                              southCluster.getPollDurationMinutes());
                }
            }
        }
        //Let's wait and allow all threads to finish starting up.
        try {
            Thread.sleep(clusterManagers.size() * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void destroy() {
        LOG.debug("ACI: Destroying ApicServiceManager ...");
        try {
            if (scheduler != null)
                scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOG.debug("Error shutting down scheduler", e);
            e.printStackTrace();
        }

        for (String key : clusterManagers.keySet()) {
            LOG.debug("Stopping clusterManager: {}", key);
            clusterManagers.get(key).stop();
            clusterManagers.remove(key);
        }

        LOG.info("ACI: Service stopped");

    }

    public void createAndScheduleJob(String location, String apicUrl, String username, String password, int pollDuration) {
        String jobIdentity = ApicClusterJob.class.getSimpleName() + "-" + location;
        LOG.info("Creating job: " + jobIdentity);
        
        JobDetail job = JobBuilder.newJob(ApicClusterJob.class).withIdentity(jobIdentity, ApicClusterJob.class.getSimpleName())
                    .usingJobData(ApicService.APIC_CONFIG_LOCATION_KEY, location)
                    .usingJobData(ApicService.APIC_CONFIG_URL_KEY, apicUrl)
                    .usingJobData(ApicService.APIC_CONFIG_USERNAME_KEY, username)
                    .usingJobData(ApicService.APIC_CONFIG_PASSWORD_KEY, password)
                    .usingJobData(ApicService.APIC_CONFIG_POLL_DURATION_KEY, pollDuration)
                    .usingJobData(ApicService.APIC_CONFIG_LOCAL_ADDR, localAddr)
                    .storeDurably()
                    .build();
        
        NodeCache nodeCache = new NodeCache();
        nodeCache.setNodeDao(nodeDao);
        nodeCache.init();
        
        Map<String, Object> clusterJobMap = new HashMap<String, Object>();
        
        clusterMap.put(job.getKey().toString(), clusterJobMap);

        // Trigger the job to run on the next round minute
        String triggerIdentity = ApicService.class.getSimpleName() + "-Trigger-" + location;
        Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(triggerIdentity, "org.opennms.aci")
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(pollDuration)
                            .withMisfireHandlingInstructionFireNow()
                            .repeatForever())
                            .build();

        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.getContext().put(ApicService.APIC_CONFIG_EVENT_FORWARDER, eventForwarder);
            scheduler.getContext().put(ApicService.APIC_CONFIG_NODE_CACHE, nodeCache);
            scheduler.getContext().put(ApicService.APIC_CONFIG_CLUSTER_MAP, clusterMap);
            scheduler.start();
            
            if (!scheduler.checkExists(job.getKey()))
                scheduler.scheduleJob(job, trigger);
            
        } catch (SchedulerException e) {
            LOG.error("Error executing job.", e);
        }

    }

    /**
     * @return the eventForwarder
     */
    public EventForwarder getEventForwarder() {
        return eventForwarder;
    }

    /**
     * @return the nodeDao
     */
    public NodeDao getNodeDao() {
        return nodeDao;
    }

    /**
     * @return the shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Initiate shutdown of service.
     */
    public void shutdown() {
        this.shutdown = true;
    }

}
