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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author metispro
 */
public class ApicClusterJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterJob.class);

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final EventForwarder eventForwarder;

    private final NodeDao nodeDao;

    private ACIRestClient client;

    private String lastProcessTime = null;
    
    private NodeCache nodeCache;
    
    private String localAddr;

    public ApicClusterJob(EventForwarder eventForwarder, NodeDao nodeDao) {
        LOG.debug("Initializing ApicClusterJob ...");
        this.eventForwarder = eventForwarder;
        this.nodeDao = nodeDao;
    }

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        
        LOG.debug("Running execution for Job: " + context.getJobDetail().getKey());

        String location = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_LOCATION_KEY);
        String apicUrl = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_URL_KEY);
        String username = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_USERNAME_KEY);
        String password = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_PASSWORD_KEY);

        if (client == null) {
            try {
                LOG.info("Initializing ACIRestClient with: \n"
                        + ApicService.APIC_CONFIG_LOCATION_KEY + ": "
                        + location + "\n" + ApicService.APIC_CONFIG_URL_KEY
                        + ": " + apicUrl + "\n"
                        + ApicService.APIC_CONFIG_USERNAME_KEY + ": "
                        + username);
                client = ACIRestClient.newAciRest(location, apicUrl, username, password);
            } catch (Exception e) {
                // e.printStackTrace();
                String msg = "Failed to initialize ACIRestClient";
                LOG.debug(msg, e);
                throw new JobExecutionException(msg, e, false);
            }

        }

        if (client == null)
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.", false);

        SchedulerContext schedulerContext = null;
        try {
            schedulerContext = context.getScheduler().getContext();
        } catch (SchedulerException e1) {
//            e1.printStackTrace();
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.", e1, false);
        }

        EventForwarder eventForwarder = (EventForwarder) schedulerContext.get(ApicService.APIC_CONFIG_EVENT_FORWARDER);
        nodeCache = (NodeCache) schedulerContext.get(ApicService.APIC_CONFIG_NODE_CACHE);
        localAddr = (String) schedulerContext.get(ApicService.APIC_CONFIG_LOCAL_ADDR);

        int pollDuration = (int) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_POLL_DURATION_KEY);

        if (schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP) == null
                || ((Map<String, Map<String, Object>>) schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString()) == null) {

            String msg = context.getJobDetail().getKey()
                    + ": Failed to initialize "
                    + ApicService.APIC_CONFIG_CLUSTER_MAP;

            LOG.error(msg);
            throw new JobExecutionException(msg, false);
        }

        Map<String, Object> clusterJobMap = (Map<String, Object>) ((Map<String, Map<String, Object>>) schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString());
        try {
            this.setLastProcessTime(clusterJobMap, location, pollDuration);

            LOG.debug("Querying for faults after: " + lastProcessTime);
            JSONArray results = client.getCurrentFaults(lastProcessTime);

            if (results == null || results.isEmpty())
                return;

            this.sendEvents(clusterJobMap, eventForwarder, results, location, client.getHost());
        } catch (Exception e) {
//            e.printStackTrace();
            String msg = "ApicClusterJob failed for cluster: LS6 at "
                    + lastProcessTime;
            LOG.error(msg, e);
            throw new JobExecutionException(msg, e, false);
        }

    }

    private void setLastProcessTime(Map<String, Object> clusterJobMap, String location, int pollDuration)
            throws Exception {

        // If last execution time is null (first time after restart),
        // then set from last event
        if (clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME) == null) {
            //TODO - Need to figure out how to get the time from the last event
            //For now, just get all events from last 60 minutes
            final java.util.Calendar startCal = GregorianCalendar.getInstance();
            startCal.add(GregorianCalendar.MINUTE, -60);
            lastProcessTime = format.format(startCal.getTime());
        } else {
            lastProcessTime = (String) clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME);
        }
        LOG.debug("Setting lastProcessTime = " + lastProcessTime);

    }

    private void sendEvents(Map<String, Object> clusterJobMap,
            EventForwarder eventForwarder, JSONArray results, String location, String apicHost)
            throws ParseException {

        Date lastProcessDate = null;

        for (Object object : results) {
            JSONObject objectData = (JSONObject) object;
            if (objectData == null)
                continue;
            for (Object object2 : objectData.keySet()) {
                String key = (String) object2;
                JSONObject classData = (JSONObject) objectData.get(key);
                JSONObject attributes = (JSONObject) classData.get("attributes");
                if (attributes == null)
                    continue;

                if (attributes.get("created") != null) {
                    String created = (String) attributes.get("created");
                    String[] startTimeparts = created.split("T");
                    String onlydate = startTimeparts[0];
                    String onlytimewtz = startTimeparts[1];
                    String onlytime = onlytimewtz.substring(0, onlytimewtz.length()
                            - 6);
                    String onlytz = onlytimewtz.substring(onlytimewtz.length()
                            - 6);
                    String tz = onlytz.replace(":", "");

                    Date createDate = ApicService.format.parse(onlydate + "T"
                            + onlytime + tz);
                    if (lastProcessDate == null
                            || createDate.after(lastProcessDate)) {
                        lastProcessDate = createDate;
                        lastProcessTime = ApicService.format.format(lastProcessDate);
                    }

                    LOG.debug(created + " --- " + attributes.toJSONString());
                    InMemoryEvent event = ConvertToEvent.toEventBuilder(this.nodeCache, location, createDate, attributes, apicHost);

                    if (event != null) {
                        eventForwarder.sendAsync(event);
                    }
                }
            }
        }

        clusterJobMap.put(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME, lastProcessTime);
        LOG.debug("Last Process Date: " + lastProcessTime);
    }

//    private void sendNewSuspectEvent(EventForwarder eventForwarder, String localAddr, String eventInterface, String distPoller) {
//        EventBuilder bldr = new EventBuilder(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI, "syslogd");
//        bldr.setInterface(addr(eventInterface));
//        bldr.setHost(localAddr);
////        bldr.setDistPoller(distPoller);
//        eventForwarder.sendNow(bldr.getEvent());
//    }
}

