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

package org.opennms.plugins.aci.client;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Base64;
import org.opennms.plugins.aci.config.SouthCluster;
import org.opennms.plugins.aci.config.SouthElement;

/**
 * @author metispro
 */
public class ACIRestConfig {
    private static byte[] sharedvector = { 0x01, 0x02, 0x03, 0x05, 0x07, 0x0B,
            0x0D, 0x11 };

    private String clusterName = "sandboxapicdc";

    private String aciUrl = "https://sandboxapicdc.cisco.com/";

    private String username = "admin";

    public String password = "ciscopsdt";

    public ACIRestConfig() {}

    public ACIRestConfig(SouthCluster southCluster) {
        List<SouthElement> elements = southCluster.getElements();
        aciUrl = "";
        username = "";
        password = "";
        for (SouthElement element : elements ){
            aciUrl += "https://" + element.getHost() + ":"  + element.getPort() + ",";
            username = element.getUserid();
            password = element.getPassword();
        }
    }
                         /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName
     *            the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * @return the aciUrl
     */
    public String getAciUrl() {
        return aciUrl;
    }

    /**
     * @param aciUrl
     *            the aciUrl to set
     */
    public void setAciUrl(String aciUrl) {
        this.aciUrl = aciUrl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     *            the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @param password
     */
    public void setPasswordEncrypt(String password) {
        this.password = this.encryptText(password);
    }

    /**
     * @return
     */
    public String getPasswordClearText() {
        return decryptText(this.password);
    }

    private String encryptText(String RawText) {
        String EncText = "";
        byte[] keyArray = new byte[24];
        byte[] temporaryKey;
        String key = this.getClass().getName();
        byte[] toEncryptArray = null;

        try {

            toEncryptArray = RawText.getBytes("UTF-8");
            MessageDigest m = MessageDigest.getInstance("MD5");
            temporaryKey = m.digest(key.getBytes("UTF-8"));

            if (temporaryKey.length < 24) // DESede require 24 byte length key
            {
                int index = 0;
                for (int i = temporaryKey.length; i < 24; i++) {
                    keyArray[i] = temporaryKey[index];
                }
            }

            Cipher c = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyArray,
                                                          "DESede"), new IvParameterSpec(sharedvector));
            byte[] encrypted = c.doFinal(toEncryptArray);
            EncText = Base64.encodeBase64String(encrypted);

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException NoEx) {
            JOptionPane.showMessageDialog(null, NoEx);
        }

        return EncText;
    }

    private String decryptText(String EncText) {

        String RawText = "";
        byte[] keyArray = new byte[24];
        byte[] temporaryKey;
        String key = this.getClass().getName();
        // String key = "developersnotedotcom";
        byte[] toEncryptArray = null;

        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            temporaryKey = m.digest(key.getBytes("UTF-8"));

            if (temporaryKey.length < 24) // DESede require 24 byte length key
            {
                int index = 0;
                for (int i = temporaryKey.length; i < 24; i++) {
                    keyArray[i] = temporaryKey[index];
                }
            }

            Cipher c = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyArray,
                                                          "DESede"), new IvParameterSpec(sharedvector));
            byte[] decrypted = c.doFinal(Base64.decodeBase64(EncText));

            RawText = new String(decrypted, "UTF-8");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException NoEx) {
            JOptionPane.showMessageDialog(null, NoEx);
        }

        return RawText;

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aciUrl == null) ? 0 : aciUrl.hashCode());
        result = prime * result
                + ((clusterName == null) ? 0 : clusterName.hashCode());
        result = prime * result
                + ((this.getPasswordClearText() == null) ? 0
                                                         : this.getPasswordClearText().hashCode());
        result = prime * result
                + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ACIRestConfig other = (ACIRestConfig) obj;
        if (aciUrl == null) {
            if (other.aciUrl != null)
                return false;
        } else if (!aciUrl.equals(other.aciUrl))
            return false;

        if (clusterName == null) {
            if (other.clusterName != null)
                return false;
        } else if (!clusterName.equals(other.clusterName))
            return false;

        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!this.getPasswordClearText().equals(other.getPasswordClearText()))
            return false;

        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ACIRestConfig [clusterName=" + clusterName + ", aciUrl="
                + aciUrl + ", username=" + username + ", password="
                + this.getPasswordClearText() + "]";
    }
}
