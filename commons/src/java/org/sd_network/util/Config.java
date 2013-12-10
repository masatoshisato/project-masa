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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;

/**
 * A Configuration of an application.
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
    // Public methods.

    /**
     * Return the instance of this class.
     * You should be call init method before call this method.
     */
    public static final Config getInstance() {
        if (_instance == null)
            throw new IllegalStateException(
                    "You must call load(propertyFile) method " +
                    "before get instance.");
        return _instance;
    }

    /**
     * Load property from the specified file path by <tt>propertyFilePath</tt>.
     *
     * @param propertyFilePath  Path to property file that is defined various
     *                          properties.
     *
     * @return  Instance of Config it was included new properties.
     */
    public static final Config load(String propertyFilePath) {
        if (propertyFilePath == null)
            throw new NullPointerException("propertyFilePath");

        if (_instance == null)
            _instance = new Config();
        _instance.loadProperty(propertyFilePath);
        _log.info("Property loaded from [" + propertyFilePath + "]");
        for (Object name: Collections.list(_instance.propertyNames())) {
            String value = _instance.getProperty((String) name);
            _log.info("Name=[" + name + "], Value=[" + value + "]");
        }
        return _instance;
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Load property from specified file.
     */
    private final void loadProperty(String filePath) {
        if (filePath == null)
            throw new NullPointerException("filePath");

        try {
            InputStream in =
                new BufferedInputStream(new FileInputStream(filePath));
            load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "File[" + filePath + "] is not accessible. " +
                    e.getMessage());
        }
    }
}
