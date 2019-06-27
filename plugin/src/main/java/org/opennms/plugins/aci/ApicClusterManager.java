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

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.json.simple.JSONObject;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author metispro
 *
 */
public class ApicClusterManager extends Thread {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterManager.class);
    
    private final ApicEventForwader apicEventForwader;
    private final SouthCluster southCluster;
    public final String clusterUrl;
    private final ClientManager client;
    public final boolean hostVerficationEnabled;
    public final String clusterName;
    
    private ACIRestClient aciClient;
    private NodeCache nodeCache;
    
    private boolean shutdown = false;
    
    private boolean connectionOpen = false;
    private String subscriptionId =  null;
    
    private Session session = null;
    
    private Date connectionStart = null;
    

    /**
     * Default Constructor
     * @param cluster
     * @throws Exception
     */
    public ApicClusterManager(ApicEventForwader apicEventForwader, SouthCluster cluster) throws Exception {
        this(apicEventForwader, cluster, false);
    }

    /**
     * Constructor with SSL HostVerification flag.
     * @param cluster
     * @param hostVerificationEnabled
     * @throws Exception
     */
    public ApicClusterManager(ApicEventForwader apicEventForwader, SouthCluster cluster, boolean hostVerificationEnabled) throws Exception {
        this.southCluster = cluster;
        this.apicEventForwader = apicEventForwader;
        this.hostVerficationEnabled = hostVerificationEnabled;
        this.clusterName = cluster.getClusterName();
        this.client = ClientManager.createClient();

        client.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, 60000);
        client.setAsyncSendTimeout(60000);
        client.setDefaultMaxSessionIdleTimeout(60000);

        if (!this.hostVerficationEnabled) {
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostVerificationEnabled(false);
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR,
                                       sslEngineConfigurator);
        }
        
//        Logging.putPrefix("aci");
        
        List<SouthElement> elements = southCluster.getElements();
        String url = "";
        String username = "";
        String password = "";
        for (SouthElement element : elements ){
        	
            url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
            username = element.getUserid();
            password = element.getPassword();
        }
        this.clusterUrl = url;
        
        nodeCache = new NodeCache();
        
        this.aciClient = ACIRestClient.newAciRest( cluster.getClusterName(), clusterUrl, username, password );

    }
    
    public boolean isRunning() {
        return !this.shutdown && this.connectionOpen && this.session != null && this.session.isOpen();
    }

    public String apicHost() {
        if (this.aciClient == null)
            return null;

        return this.aciClient.getHost();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (this.shutdown)
            return;

        LOG.info("ACI: Starting ApicClusterManager for: " + clusterName);

        try {
            long now = System.currentTimeMillis();
            subscriptionId = this.connectAndSubscribeToFaults();

            while (isRunning()) {
                //Currently both Subscription and Token expire every 60 seconds
                if ((System.currentTimeMillis() - now) > 30000 && this.subscriptionId != null) {
                    LOG.debug("ACI: Refresh client session and subscription token for id: {}", subscriptionId);
                    //Do refresh on client session
                    aciClient.runQueryNoAuth("/api/aaaRefresh.json");
                    //Do refresh on subscription
                    aciClient.runQueryNoAuth("/api/subscriptionRefresh.json?id=" + subscriptionId);
                    now = System.currentTimeMillis();
                }
                Thread.sleep(1000);
            }
            
            LOG.info("ACI: Stopping websocket client for " + this.clusterName);
            client.shutdown();
        } catch (Throwable e) {
            LOG.error("APIC websocket exception", e);
//            e.printStackTrace();
            this.connectionOpen = false;
        }
        LOG.debug("ACI: Exiting thread ApicClusterManager for APIC: " + this.clusterName);
    }
    
    public void shutdown() {
        LOG.debug("ACI: Shutting down " + this.clusterName);
        this.shutdown = true;
    }
    
    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
    
    public boolean isShutdown() {
        return this.shutdown;
    }

    private String connectAndSubscribeToFaults() throws Exception {
        LOG.debug("ACI: Starting websocket client for: {}", clusterName);

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        session = client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(Session session, EndpointConfig config) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        ExecutorService execService = Executors.newFixedThreadPool(25);

                        @Override
                        public void onMessage(String message) {
                            if (message == null)
                                return;
                            LOG.trace("ACI: Received message: {}", message);
                            Runnable runnableTask = () -> {
                                apicEventForwader.sendEvent(clusterName, aciClient.getHost(), message);
                            };
                            
                            execService.execute(runnableTask);
                        }

                    });
                    connectionOpen = true;
                } catch (Exception e) {
                    LOG.error("Failed to connect to server.", e);
//                    e.printStackTrace();
                }
            }
            
            @Override
            public void onClose(Session session, CloseReason closeReason) {
                LOG.info("ACI: Websocket connection closed: " + closeReason.getReasonPhrase());
                try {
                    session.close();
                } catch (IOException e) {
                    //Don't care we are forcibly restarting
                }
                connectionOpen = false;
                subscriptionId = null;
                Thread.currentThread().interrupt();
            }
            
            @Override
            public void onError(Session session, Throwable thr) {
                LOG.error("ACI: Websocket connection error: ", thr);
                thr.printStackTrace();
                try {
                    session.close();
                } catch (IOException e) {
                    //Don't care we are forcibly restarting
                }
                connectionOpen = false;
                subscriptionId = null;
                Thread.currentThread().interrupt();
            }

        }, cec, new URI("wss://"+ this.aciClient.getHost() + "/socket" + this.aciClient.getToken()));
        
//        session = fs.get();
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        final Calendar startCal = GregorianCalendar.getInstance();
        startCal.add(Calendar.SECOND, -30);
        String formattedTime = format.format(startCal.getTime());

//        String query = "/api/node/class/faultInfo.json?subscription=yes";
        String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + formattedTime + "\")&subscription=yes";
        LOG.debug("ACI: Subscribing to query: " + query);
        JSONObject result = (JSONObject) aciClient.runQueryNoAuth(query);
        this.connectionStart = startCal.getTime();
        return (String)result.get("subscriptionId");
    }
    
    public void printStatus() {
        System.out.println("\t" + this.southCluster.getClusterName());
        System.out.println("\t--- subscriptionId: " + this.subscriptionId);
        System.out.println("\t--- threadAlive: " + this.isAlive());

        if (this.isAlive())
            System.out.println("\t--- isRunning: " + this.isRunning());
        else
            System.out.println("\t--- isRunning: " + this.isAlive());

        if (this.isRunning()) {
            System.out.println("\t--- apicHost: " + this.apicHost());
            System.out.println("\t--- Running Since: " + this.connectionStart);
        } else {
            System.out.println("\t--- Not connected to apic: " + this.clusterUrl);
        }
    }

    public void printConfig() {
        List<SouthElement> elements = southCluster.getElements();
        String url = "";
        String username = "";
        String password = "";
        for (SouthElement element : elements ){
            url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
            username = element.getUserid();
            password = element.getPassword();
        }
        url = url.replaceAll(",$", "");
        System.out.println("\t" + southCluster.getClusterName());
        System.out.println("\t--- cluster-type: " + southCluster.getClusterType());
        System.out.println("\t--- cron-schedule: " + southCluster.getCronSchedule());
        System.out.println("\t--- poll-duration-minutes: " + southCluster.getPollDurationMinutes());
        System.out.println("\t--- location: " + southCluster.getLocation());
        System.out.println("\t--- url: " + url);
        System.out.println("\t--- username: " + username);

    }
}
