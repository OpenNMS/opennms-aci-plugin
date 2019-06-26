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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.IpInterface;
import org.opennms.integration.api.v1.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author metispro
 *
 */
public class NodeCache {
    private static final Logger LOG = LoggerFactory.getLogger(NodeCache.class);
    
    private long MAX_SIZE = 10000;
    private long MAX_TTL  = 3; // Minutes

    private volatile NodeDao nodeDao;

    private LoadingCache<String, String> cache = null;
    
    public NodeCache() {}
    
    @SuppressWarnings("unchecked")
    public void init() {
        LOG.info("initializing node data cache (TTL="+MAX_TTL+"m, MAX_SIZE="+MAX_SIZE+")");
         @SuppressWarnings("rawtypes")
        CacheBuilder cacheBuilder =  CacheBuilder.newBuilder();
         if(MAX_TTL>0) {
             cacheBuilder.expireAfterWrite(MAX_TTL, TimeUnit.MINUTES);
         }
         if(MAX_SIZE>0) {
             cacheBuilder.maximumSize(MAX_SIZE);
         }

         cache=cacheBuilder.build(new CacheLoader<String, String>() {
             @Override
             public String load(String key) throws Exception {
                 return lookupNodeKey(key);
             }
         }
);
    }
    
    private String lookupNodeKey(String key) throws Exception {
        if (key == null)
            return null;
        
        String[] keyParts = key.split(ApicService.FS_SEP);
        Node node = null;
        if (keyParts.length == 2) {
            String foreignSource = keyParts[0];
            String foreignId = keyParts[1];

            node = nodeDao.getNodeByForeignSourceAndForeignId(foreignSource, foreignId);
        } else if (keyParts.length == 1) {
            InetAddress address = InetAddress.getByName(key);
//            List<Node> nodes = nodeDao.findByIpAddressAndService(address, "ICMP");
            // TODO - Temporarily changing to pull back all nodes until OIA NodeDAO supports findByIpAddressAndService
            List<Node> nodes = nodeDao.getNodes();
            if (nodes.size() > 0) {
//                node = nodes.get(0);
                NODELOOP: for (Node n: nodes) {
                    for (IpInterface ipInterface : n.getIpInterfaces()) {
                        if (ipInterface != null && ipInterface.equals(address)) {
                            node = n;
                            break NODELOOP;
                        }
                    }
                }
            }
        } else {
            throw new NodeCacheInvalidKeyException("Incorrect key format key="
                    + key);
        }

        if (node == null)
            throw new NodeCacheKeyNotFoundException("ACI Key not found, key="
                    + key);

        LOG.debug("Found Node for key = " + key);

        return node.getId().toString();
    }
    
    public Long getNodeId(String key) {
        try {
            String nodeKey = this.cache.get(key);
            if (nodeKey.contains(ApicService.FS_SEP)) {
                String[] nodeKeyParts = nodeKey.split(ApicService.FS_SEP);
                return Long.parseLong(nodeKeyParts[0]);
            } else {
                return Long.parseLong(nodeKey);
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
    
    public String getInterfaceId(String key) {
        try {
            String nodeKey = this.cache.get(key);
            String[] nodeKeyParts = nodeKey.split(ApicService.FS_SEP);
            if (nodeKeyParts.length == 2)
                return nodeKeyParts[1];
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }
    
}
