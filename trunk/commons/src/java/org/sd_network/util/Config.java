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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;

/**
 * This class represent configuration of an application.
 *
 * <p> This class is applied Singleton pattern, you can not create instance
 * twice in an application. How to use this class for generally is, 
 * <ul>
 *  <li> call {@link #load(String)} method in initialization of an 
 *       application to load properties from property file and create 
 *       instance. (Only once)
 *  <li> call {@link #getInstance()} method from anywhere after call
 *       {@link #load(String)} method if necessary.
 * </ul>
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Config
    extends Properties
{
    /** Default Logger. */
    private static final Logger _log = Logger.getLogger(
            Config.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** The instance of this class. */
    private static Config _instance = null;

    //////////////////////////////////////////////////////////// 
    // Initializations.

    /**
     * Default instructor for Config class.
     *
     * <p> NOTE: This method restrict to private access because of this class
     * have applied singleton pattern. Thus other class can not create 
     * instance of this class.
     */
    private Config() {
        super();
    }

    /**
     * Instructor with parameters for Config class.
     *
     * <p> NOTE: This method restrict to private access because of this class
     * have applied singleton pattern. Thus other class can not create 
     * instance of this class.
     */
    private Config(Properties defaults) {
        super(defaults);
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return the instance of this class.
     * The instance is included properties that is loaded from property file.
     *
     * <p> Before call this method, you must be call {@link #load(String)} 
     * method that is load properties from property file which is specified by
     * parameter.
     *
     * @return  Instance of this class.
     *
     * @throws  IllegalStateException
     *          Throws this exception when call this method before call 
     *          {@link #load(String)} method.
     */
    public static final Config getInstance()
        throws IllegalStateException
    {
        if (_instance == null)
            throw new IllegalStateException(
                    "You must call load(propertyFile) method " +
                    "before get instance.");
        return _instance;
    }

    /**
     * Load properties from file which is specified file path by 
     * <tt>propertyFilePath</tt> parameter, and create instance of this class.
     *
     * @param propertyFilePath  Path to property file that is defined various
     *                          properties.
     *
     * @return  Config instance included new properties.
     *
     * @throws  IllegalArgumentException
     *          Throws if specified argument <tt>propertyFilePath</tt> is null
     *          or zero length string.
     *
     * @throws  IOException
     *          Throws if access error occured when load properties from 
     *          property file specified by argument <tt>propertyFilePath</tt>.
     *          Or is not file.
     */
    public static final Config load(String propertyFilePath)
        throws IllegalArgumentException, IOException
    {
        // Check argument.
        if (StringUtil.isEmpty(propertyFilePath, true))
            throw new IllegalArgumentException("propertyFilePath is empty.");
        if (!(new File(propertyFilePath).isFile()))
            throw new IOException(
                    "propertyFilePath [" + propertyFilePath + "] is not file.");

        // Create instance if still it.
        if (_instance == null)
            _instance = new Config();

        // Load properties.
        _instance.loadProperty(propertyFilePath);
        _log.info("Property loaded from [" + propertyFilePath + "]");
        for (Object name: Collections.list(_instance.propertyNames())) {
            String value = _instance.getProperty((String) name);
            _log.info("Name=[" + name + "], Value=[" + value + "]");
        }
        return _instance;
    }

    //////////////////////////////////////////////////////////// 
    // Package methods.

    /**
     * Null reset the instance.
     *
     * <p><b>This is for UnitTest only.</b>
     * This method for UnitTest only. When execute Main method of TestRunner
     * class, set system property "UnitTest" = "TRUE". If call this method
     * when not executed Main method of TestRunner, throws 
     * {@link java.lang.UnsupportedOperationException}.
     *
     * @throws  UnsupportedOperationException
     *          Throws if not set system property "UnitTest".
     */
    static final void reset()
        throws UnsupportedOperationException
    {
        if (!System.getProperty("UnitTest").equals("TRUE"))
            throw new UnsupportedOperationException(
                    "This method for UnitTest only!");

        _instance = null;
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Load property from specified file.
     *
     * @param filePath  file path of property file.
     *
     * @throws  IllegalArgumentException
     *          Throws if argument specified by <tt>filePath</tt> is null or 
     *          zero length string.
     *
     * @throws  IOException
     *          Throws if access error occured when load property from
     *          file specified argument <tt>filePath</tt>.
     */
    private final void loadProperty(String filePath)
        throws IllegalArgumentException, IOException
    {
        if (StringUtil.isEmpty(filePath, true))
            throw new IllegalArgumentException("filePath is empty.");

        InputStream in = new BufferedInputStream(new FileInputStream(filePath));
        load(in);
    }
}
