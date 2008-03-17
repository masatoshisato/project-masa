/*
 * Copyright 2007 Masatoshi sato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sd_network.util;

import junit.framework.TestCase;

/**
 * Unit test for Config class.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class ConfigTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisation.

    //////////////////////////////////////////////////////////// 
    // Test cases.

    public void testInit() {
        Config config = Config.getInstance();
        assertEquals(17, config.size());

        assertEquals("property1",
                config.getProperty("org.sd_network.TestProperty1"));
        assertEquals("", config.getProperty("org.sd_network.TestProperty2"));

        assertEquals("default",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.0.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.0.JDBCDriver"));
        assertEquals("jdbc:h2:test/default/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.0.URL"));
        assertEquals("sa",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.0.UserName"));
        assertEquals("",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.0.Password"));

        assertEquals("test1",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.1.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.1.JDBCDriver"));
        assertEquals("jdbc:h2:test/test1/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.1.URL"));
        assertEquals("sa",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.1.UserName"));
        assertEquals("",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.1.Password"));

        assertEquals("test2",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.2.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.2.JDBCDriver"));
        assertEquals("jdbc:h2:test/test2/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.2.URL"));
        assertEquals("sa",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.2.UserName"));
        assertEquals("",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.2.Password"));
    }
}
