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

package org.opennms.plugins.aci.test;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.json.simple.JSONArray;
import org.opennms.plugins.aci.client.ACIRestClient;
import org.opennms.plugins.aci.client.ACIRestConfig;


/**
 * @author metispro
 *
 */
public class ACIRestClientTest
{
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    /**
     * @param args
     */
    public static void main( String[] args )
    {
        ACIRestConfig config = new ACIRestConfig();
//        final String userName = args[0];
//        final String pw = args[1];

        try
        {
            final java.util.Calendar startCal = GregorianCalendar.getInstance();
            startCal.add(GregorianCalendar.MINUTE, -100);
            
            ACIRestClient client = ACIRestClient.newAciRest( config.getClusterName(), config.getAciUrl(), config.getUsername(), config.getPassword() );
            
            String formattedTime = format.format(startCal.getTime());
            String[] formattedTimeParts = formattedTime.split("T");
            String fDate = formattedTimeParts[0];
            String fTime = formattedTimeParts[1];
            String[] fTimeParts = fTime.split("-");
            String justTime = fTimeParts[0];
            String tz = fTimeParts[1];
            String tzhours = tz.substring(0, 2);
            String tzmins = tz.substring(2);
            
            String goodtime = fDate + "T" + justTime + "-" + tzhours + ":" + tzmins;
            
            System.out.println("Formatted Time: " + goodtime);
            
            String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + formattedTime + "\")";
//            System.out.println("Running query: " + query);
//            String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + fDate + "T" + justTime + "\")";
//            JSONArray jsonAray = client.getFaults(query);

//            client.getFaults("/api/class/faultInst.json?query-target-filter=gt(faultInst.lastTransition, \"" + goodtime + "\")");
//            client.getFaults("/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + goodtime + "\")");
//            client.getCurrentFaults(formattedTime);
//            client.getClassInfo(  "faultRecord" );
//            client.getClassInfo(  "faultRecord", "eventRecord" );
//            JSONArray jsonArray = client.getClassInfo( "topSystem" );
            JSONArray jsonArray = client.getClassInfo( "bogus" );
//            client.getClassInfo( "ethpmPhysIf" );
//            client.getManagedObject( "topology/pod-1/node-155/sys/phys-[eth1/18]/phys" );
//            client.getManagedObject( "topology/pod-1/node-155/sys" );
//            client.getManagedObjectSubtree( "topology/pod-1/node-155/sys", "ethpmPhysIf" );
//            client.getManagedObject( "dbgs/ac/path-121-to-122" );
//            client.getManagedObjectSubtree( "dbgs/ac/path-121-to-122", "topSystem" );
            
//            client.getHealth( "fvAp", "fvTenant", "fabricNode" );
//            client.getStats( "fvAp" );
//            System.out.println("Completed query: " + query);
            System.out.println(jsonArray.toJSONString());

        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
