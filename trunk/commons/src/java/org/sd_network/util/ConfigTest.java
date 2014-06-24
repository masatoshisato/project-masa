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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Unit test for {@link Config} class.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class ConfigTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Private class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
        ConfigTest.class.getName());

    /** 
     * Parent directory for test property files. 
     * This is a current working directory.
     */
    private static final String _parentDir =
        System.getProperty("user.dir") +
        System.getProperty("file.separator");

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisation.

    public void setUp()
        throws Exception
    {
        super.setUp();
        Config.reset();
        _log.log(Level.FINE, "--- Run test case [" + getName() + "]");
    }

    //////////////////////////////////////////////////////////// 
    // Test cases for getInstance method.

    /**
     * Test case: Expect call {@link Config#getInstance()} method 
     * after call {@link Config#load(String)} method.
     * After get instance, check whether get contents of properties file is 
     * normally.
     *
     * @throws   IOException
     *          Throws if I/O error occurred when configration file is loaded.
     */
    public void testGetInstance()
        throws IOException
    {
        // load configuration file.
        Config.load(_parentDir + "test/testLoadAndGetInstance.properties");

        // get instance of Config class.
        Config config = Config.getInstance();

        // run test case.
        assertEquals(17, config.size());

        assertEquals("property1",
                config.getProperty("org.sd_network.TestProperty1"));
        assertEquals("", config.getProperty("org.sd_network.TestProperty2"));

        assertEquals("default",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.default.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.default.JDBCDriver"));
        assertEquals("jdbc:h2:test/db/default/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.default.URL"));
        assertEquals("sa",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.default.UserName"));
        assertEquals("",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.default.Password"));

        assertEquals("test1",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test1.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test1.JDBCDriver"));
        assertEquals("jdbc:h2:test/db/test1/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test1.URL"));
        assertEquals("sa",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test1.UserName"));
        assertEquals("",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test1.Password"));

        assertEquals("test2",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test2.ID"));
        assertEquals("org.h2.Driver",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test2.JDBCDriver"));
        assertEquals("jdbc:h2:test/db/test1/db",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test2.URL"));
        assertEquals("test",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test2.UserName"));
        assertEquals("test",
                config.getProperty(
                    "org.sd_network.db.ConnectionParameter.test2.Password"));
    }

    /**
     * Test case: Expect throws IllegalStateException when call 
     * {@link Config#getInstance()} method before call 
     * {@link Config#load(String)} method.
     */
    public void testGetInstanceThrowIllegalStateException() {
        try {
            Config.getInstance();
            fail(
                    "This case expect throws IllegalStateException when call" +
                    " getInstance method before call load method," +
                    " but did not thrown exception.");
        } catch (IllegalStateException e) {
            // This is normally, do nothing.
        }
    }

    //////////////////////////////////////////////////////////// 
    // Test cases for load method.

    /**
     * Test case: Expect load properties from property file specified at
     * parameter of load method.
     */
    public void testLoad() {
        // This test case same contents at testGetInstance() method.
        // Do nothing.
    }

    /**
     * Test case: Expect throws IllegalArgumentException when call load method
     * with illegal parameters as file path string.
     *
     * @throws   IOException
     *          Throws if I/O error occurred when load configuration file.
     *          But this exception is never thrown because of specified 
     *          parameter to {@link Config#load(String)} is null or empty
     *          string, IllegalArgumentExcepiton is thrown before execute
     *          load process.
     */
    public void testLoadThrowIllegalArgumentException()
        throws IOException
    {
        try {
            Config.load((String) null);
            fail(
                    "This case expect throws IllegalArgumentExcepiton" +
                    " when call load method with null parameter," +
                    " but did not thrown exception.");
        } catch (IllegalArgumentException e) {
            // This is normally, do nothing.
        }
        try {
            Config.load("");
            fail(
                    "This case expect throws IllegalArgumentException" +
                    " when call load method with empty string, " +
                    " but did not thrown exception.");
        } catch (IllegalArgumentException e) {
            // This is normally, do nothing.
        }
    }

    /**
     * Test case: Expect throws IOException when call load method with 
     * parameter which is not file or not readable file.
     */
    public void testLoadThrowIOException() {
        try {
            Config.load(_parentDir + "test");
            fail(
                    "This case expect throws IOException" +
                    " when call load method with parameter which is not" +
                    " file path string or is not readable," +
                    " but did not thrown exception.");
        } catch (IOException e) {
            // This is normally, do nothing.
        }
    }
}
