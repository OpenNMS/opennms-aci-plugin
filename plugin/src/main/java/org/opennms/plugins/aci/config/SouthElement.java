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

package org.opennms.plugins.aci.config;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.integration.api.xml.ConfigUtils;

/**
 * @author tf016851
 *
 */
@XmlRootElement(name = "south-element")
@XmlAccessorType(XmlAccessType.FIELD)
public class SouthElement implements Serializable {
    
    private static final long serialVersionUID = 2L;
    
    public static final String DEFAULT_USERID = "opennms";
    public static final String DEFAULT_PASSWORD = "opennms";
    public static final String DEFAULT_SOUTH_CLIENT_API = "org.opennms.netmgt.southd";
    public static final String DEFAULT_SOUTH_MESSAGE_PARSER = "org.opennms.netmgt.southd";

    @XmlAttribute(name = "host", required = true)
    private String m_host;

    @XmlAttribute(name = "port")
    private Integer m_port;

    @XmlAttribute(name = "userid")
    private String m_userid;

    @XmlAttribute(name = "password")
    private String m_password;

    @XmlAttribute(name = "southbound-api")
    private String m_southboundApi;

    @XmlAttribute(name = "southbound-message-parser")
    private String m_southboundMessageParser;

    @XmlAttribute(name = "reconnect-delay")
    private Long m_reconnectDelay;

    @XmlAttribute(name = "wss-port")
    private String m_wssPort;

    public String getHost() {
        return m_host;
    }

    public void setHost(final String host) {
        m_host = Objects.requireNonNull(host);
    }

    public Integer getPort() {
        return m_port != null ? m_port : 443;
    }

    public void setPort(final Integer port) {
        m_port = port;
    }

    public String getUserid() {
        return m_userid != null ? m_userid : DEFAULT_USERID;
    }

    public void setUserid(final String userid) {
        m_userid = ConfigUtils.normalizeAndTrimString(userid);
    }

    public String getPassword() {
        return m_password != null ? m_password : DEFAULT_PASSWORD;
    }

    public void setPassword(final String password) {
        m_password = ConfigUtils.normalizeString(password);
    }

    public String getSouthboundApi() {
        return m_southboundApi != null ? m_southboundApi : DEFAULT_SOUTH_CLIENT_API;
    }

    public void setSouthboundApi(final String southboundApi) {
        this.m_southboundApi = ConfigUtils.normalizeString(southboundApi);
    }

    public String getSouthboundMessageParser() {
        return m_southboundMessageParser != null ? m_southboundMessageParser : DEFAULT_SOUTH_MESSAGE_PARSER;
    }

    public void setSouthboundMessageParser(final String southboundMessageParser) {
        m_southboundMessageParser = ConfigUtils.normalizeString(southboundMessageParser);
    }

    public Long getReconnectDelay() {
        return m_reconnectDelay != null ? m_reconnectDelay : 30000L;
    }

    public void setReconnectDelay(final Long reconnectDelay) {
        m_reconnectDelay = reconnectDelay;
    }

    public String getM_wssPort() {
        return m_wssPort;
    }

    public void setM_wssPort(String m_wssPort) {
        this.m_wssPort = m_wssPort;
    }

    @Override
    public String toString() {
        return "SouthElement{" +
                "m_host='" + m_host + '\'' +
                ", m_port=" + m_port +
                ", m_userid='" + m_userid + '\'' +
                ", m_password='" + m_password + '\'' +
                ", m_southboundApi='" + m_southboundApi + '\'' +
                ", m_southboundMessageParser='" + m_southboundMessageParser + '\'' +
                ", m_reconnectDelay=" + m_reconnectDelay +
                ", m_wssPort='" + m_wssPort + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SouthElement that = (SouthElement) o;
        return m_host.equals(that.m_host) &&
                Objects.equals(m_port, that.m_port) &&
                m_userid.equals(that.m_userid) &&
                m_password.equals(that.m_password) &&
                Objects.equals(m_southboundApi, that.m_southboundApi) &&
                Objects.equals(m_southboundMessageParser, that.m_southboundMessageParser) &&
                Objects.equals(m_reconnectDelay, that.m_reconnectDelay) &&
                Objects.equals(m_wssPort, that.m_wssPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_host, m_port, m_userid, m_password, m_southboundApi, m_southboundMessageParser, m_reconnectDelay, m_wssPort);
    }
}
