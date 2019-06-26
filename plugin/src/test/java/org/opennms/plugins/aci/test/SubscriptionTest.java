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

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
import org.opennms.plugins.aci.client.ACIRestConfig;

/**
 * @author metispro
 *
 */
public class SubscriptionTest {

//    static {
//        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
//            {
//                public boolean verify(String hostname, SSLSession session)
//                {
//                    // ip address of the service URL(like.23.28.244.244)
//                    //if (hostname.equals("23.28.244.244"))
//                        return true;
//                    //return false;
//                }
//            });
//    }

    private boolean connectionOpen = false;

    public SubscriptionTest() {
    }

    public void runTest() {
        System.out.println("Running SubscriptionTest ...");
        ACIRestConfig config = new ACIRestConfig();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        final Calendar startCal = GregorianCalendar.getInstance();

        startCal.add(Calendar.MINUTE, -60);

        String formattedTime = format.format(startCal.getTime());


        long startTime = System.currentTimeMillis();

        try {
            ACIRestClient aciClient = ACIRestClient.newAciRest( config.getClusterName(), config.getAciUrl(), config.getUsername(), config.getPassword() );
//            SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy,
//                                                       SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            SchemeRegistry registry = new SchemeRegistry();
//            registry.register(new Scheme("https", 443, sf));
//            ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
//            httpClient = new DefaultHttpClient(ccm);
//
//            authHeader = "Basic " + Base64.encodeBase64String((username + ':'
//                    + password).getBytes("UTF-8"));
//            httpContext = new BasicHttpContext();
//            cookieStore = new BasicCookieStore();
//            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = ClientManager.createClient();
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostVerificationEnabled(false);
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
//            client.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, 60000);
//            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);
//            client.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 60000);

//            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
//            sslEngineConfigurator.setHostnameVerifier(new HostnameVerifier() {
//                @Override
//                public boolean verify(String host, SSLSession sslSession) {
////                    Certificate certificate = sslSession.getPeerCertificates()[0];
//                    // validate the host in the certificate
//                    return true;
//                }
//            });
           client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
           client.setAsyncSendTimeout(60000);
           client.setDefaultMaxSessionIdleTimeout(60000);

           System.out.println(new Date() + " Creating websocket connection with settings:");
           System.out.println("\tasyncSendTimeout: " + client.getDefaultAsyncSendTimeout());
           System.out.println("\tmaxSessionIdleTimeout: " + client.getDefaultMaxSessionIdleTimeout());
           System.out.println("\t" + ClientProperties.HANDSHAKE_TIMEOUT + ": " + client.getProperties().get(ClientProperties.HANDSHAKE_TIMEOUT));

           client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {

                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: "+message);
                            }

                        });
                        //session.getBasicRemote().sendText(SENT_MESSAGE);
                        connectionOpen = true;
                    } catch (Exception e) {
                        System.out.println(new Date() + " WebSocket failed with:");
                        e.printStackTrace();
                    }
                }

               @Override
               public void onClose(Session session, CloseReason closeReason) {
                   System.out.println("ACI: Websocket connection closed: " + closeReason.getReasonPhrase());
                   try {
                       session.close();
                   } catch (IOException e) {
                       //Don't care we are forcibly restarting
                   } finally {
                       connectionOpen = false;
                   }

                   Thread.currentThread().interrupt();
               }

               @Override
               public void onError(Session session, Throwable thr) {
                   System.out.println("ACI: Websocket connection error: " + thr.getLocalizedMessage());
                   thr.printStackTrace();
                   try {
                       session.close();
                   } catch (IOException e) {
                       //Don't care we are forcibly restarting
                   } finally {
                       connectionOpen = false;
                   }
                   Thread.currentThread().interrupt();
               }
           }, cec, new URI("wss://"+ aciClient.getHost() + "/socket" + aciClient.getToken()));
//        }, cec, new URI("wss://"+ aciClient.getHost() + ":1000/socket" + aciClient.getToken()));

            //Next, query subscription and store subscriptionId
//            String query = "/api/node/class/faultInfo.json?subscription=yes";
            String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + formattedTime + "\")&subscription=yes";

            System.out.println("Subscribing to query: " + query);
            long now = System.currentTimeMillis();
            JSONObject result = (JSONObject) aciClient.runQueryNoAuth(query);
            String subscriptionId = (String)result.get("subscriptionId");

            int count=0;
            while (connectionOpen && subscriptionId != null) {
                //Currently both Subscription and Token expire every 60 seconds
                if ((System.currentTimeMillis() - now) > 30000) {
                    System.out.println(new Date() + " Refreshing token");
                    //Do refresh on client session
                    aciClient.runQueryNoAuth("/api/aaaRefresh.json");
                    //Do refresh on subscription
                    aciClient.runQueryNoAuth("/api/subscriptionRefresh.json?id=" + subscriptionId);
                    count = 0;
                    now = System.currentTimeMillis();
                }
                count++;
                System.out.println(count + ": Waiting ...");
                Thread.sleep(1000);
            }

            client.shutdown();
        } catch (Exception e) {
            System.out.println(new Date() + " WebSocket failed with:");
            e.printStackTrace();
        }

        System.out.println("SubscriptionTest finished in " + (System.currentTimeMillis() - startTime) + " millis");

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SubscriptionTest test = new SubscriptionTest();
        test.runTest();
    }
}
