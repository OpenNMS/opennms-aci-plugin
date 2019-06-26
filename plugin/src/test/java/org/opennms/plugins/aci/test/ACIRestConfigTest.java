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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.plugins.aci.client.ACIRestConfig;

/**
 * @author metispro
 *
 */
public class ACIRestConfigTest
{

    /**
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    /**
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void test()
    {
        ACIRestConfig config = new ACIRestConfig();
        
        config.setAciUrl( "URL" );
        config.setClusterName( "TEST" );
        config.setUsername( "TEST" );
        config.setPasswordEncrypt( "TEST" );
        
        assertEquals( "URL", config.getAciUrl() );
        assertEquals( "TEST", config.getClusterName() );
        assertEquals( "TEST", config.getPasswordClearText() );
        assertNotEquals( "TEST", config.getPassword() );
    }

}
