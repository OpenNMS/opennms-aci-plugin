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

package org.opennms.plugins.aci.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.plugins.aci.client.ACIRestClient;


/**
 * @author tf016851
 */
@Command(scope = "aci", name = "get-faults", description="Gets faults from ACI")
@Service
public class GetFaultsCommand implements Action {
    
    @Option(name = "-l", aliases = "--location", description = "Location", required=true, multiValued=false)
    private String location;

    @Option(name = "-a", aliases = "--aci-url", description = "ACI URL", required=true, multiValued=false)
    private String aciUrl;

    @Option(name = "-u", aliases = "--username", description = "Username", required=true, multiValued=false)
    private String username;

    @Option(name = "-p", aliases = "--password", description = "Password", required=true, multiValued=false)
    public String password;
    
    @Option(name = "-d", aliases = "--duration", description = "Fault Polling Duration in minutes.", required=false, multiValued=false)
    public int pollDuration = 5;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.karaf.shell.console.AbstractAction#doExecute()
     */
    @Override
    public Object execute() throws Exception {
        try
        {
            ACIRestClient client = ACIRestClient.newAciRest( location, aciUrl, username, password );
            
            JSONArray results = client.getCurrentFaults(client.getTimeStamp(pollDuration * 60));

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

                    System.out.println(attributes.toJSONString());
                }
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return null;
    }

}
