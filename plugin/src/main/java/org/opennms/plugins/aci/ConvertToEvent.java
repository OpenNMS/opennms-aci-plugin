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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.opennms.integration.api.v1.model.EventParameter;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.integration.api.v1.model.Severity;
import org.opennms.integration.api.v1.model.immutables.ImmutableInMemoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author metispro
 *
 */
public class ConvertToEvent {

	private static final Logger LOG = LoggerFactory.getLogger(ConvertToEvent.class);

	private static final String ACI_UEI_PART = "uei.opennms.org/cisco/aci/";

	public static int MAX_SYSLOG_DROP_THRESHOLD_MIN = 5;

	public static int MAX_SYSLOG_INGEST_THRESHOLD_MIN = 43200;

	private static Map<String, String> syslodTimestampProperties;
	
    private static Date sysTime;

	public static Date getcurrentSystemTime() {

		return sysTime;

	}

	public static void setcurrentSystemTime(Date systemTime) {
		
		if(systemTime==null)
			sysTime=Calendar.getInstance().getTime();
		 sysTime=systemTime;
	}
	
	static {
        try {
                loadSyslodTimestampProperties();
                if (syslodTimestampProperties != null) {
                	MAX_SYSLOG_DROP_THRESHOLD_MIN = Integer.parseInt(syslodTimestampProperties.get("MAX_SYSLOG_DROP_THRESHOLD_MIN"));
                	MAX_SYSLOG_INGEST_THRESHOLD_MIN = Integer.parseInt(syslodTimestampProperties.get("MAX_SYSLOG_INGEST_THRESHOLD_MIN"));	
                }
        	}   catch (Exception e) {
        	LOG.debug("Not able to read syslog timestamp property file values" +e);
        	}
		   }

	private static void loadSyslodTimestampProperties() {
//		try {
//			syslodTimestampProperties = readPropertiesInOrderFromToMap(
//					ConfigFileConstants.getFile(ConfigFileConstants.SYSLOGD_TIME_PROPERITES));
//			} catch (Exception e) {
//			LOG.error("Failed to load syslogd timestamp properties and properties file !" + e.getMessage());
//			}
	}

	private static final Map<String, Severity> SEVERITY_MAP;

	static {
		SEVERITY_MAP = new HashMap<String, Severity>();
		SEVERITY_MAP.put("critical", Severity.CRITICAL);
		SEVERITY_MAP.put("major", Severity.MAJOR);
		SEVERITY_MAP.put("minor", Severity.MINOR);
		SEVERITY_MAP.put("warning", Severity.WARNING);
		SEVERITY_MAP.put("info", Severity.NORMAL);
		SEVERITY_MAP.put("cleared", Severity.CLEARED);
	}

	public static final InMemoryEvent toEventBuilder(NodeCache nodeCache, String location, Date createDate,
													 JSONObject attributes, String apicHost) throws ParseException {

		if (attributes == null || attributes.size() == 0)
			return null;

		ImmutableInMemoryEvent.Builder bldr = ImmutableInMemoryEvent.newBuilder();

		Long nodeId = (long) 0;
		LOG.trace("Building Event for " + location + " message: " + attributes.toJSONString());
		// First, let's add all Fault attributes as parameters.
		for (Object obj : attributes.keySet()) {
			EventParameter param = new EventParameter() {
				@Override
				public String getName() {
					return (String) obj;
				}

				@Override
				public String getValue() {
					return (String) attributes.get(obj);
				}
			};
			bldr.addParameter(param);
		}
		String dn = (String) attributes.get("affected");
		if (dn == null)
			dn = (String) attributes.get("dn");
		String[] dnParts = dn.split(ApicService.DN_SEP);

		if (dnParts[0].equals("topology") && dnParts.length >= 3) {
			// Device Fault
			String key = new StringBuffer().append(location).append(ApicService.FS_SEP).append(dnParts[0]).append(dnParts[1]).append(dnParts[2]).toString();
			nodeId = nodeCache.getNodeId(key);
			if (nodeId != null)
				bldr.setNodeId(nodeId.intValue());
		}

		// If we still do not have a nodeId, default to apichost
		if (nodeId == null || nodeId.longValue() == 0) {
			nodeId = nodeCache.getNodeId(apicHost);
			if (nodeId != null)
				bldr.setNodeId(nodeId.intValue());
		}
		if ("cleared".equals(attributes.get("severity")))
			bldr.setUei(ACI_UEI_PART + attributes.get("severity"));
		else
			bldr.setUei(ACI_UEI_PART + attributes.get("code"));
		bldr.setSeverity(SEVERITY_MAP.get(attributes.get("severity")));
		bldr.setSource(ApicService.class.getSimpleName());

		EventParameter apicHostsParam = new EventParameter() {
			@Override
			public String getName() {
				return "apicHost";
			}

			@Override
			public String getValue() {
				return apicHost;
			}
		};
		bldr.addParameter(apicHostsParam);
		return bldr.build();
	}

}
