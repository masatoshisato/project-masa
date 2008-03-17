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
package org.sd_network.vfs.sector;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.sd_network.util.Config;

/**
 * The service class for managing a set of SectorDrivers.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SectorDriverManager
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            SectorDriverManager.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Error Message for instantiation error of SectorDriver. */
    private static final String _INSTANTIATION_ERROR =
        "SectorDriver could not create instance. ";

    /** The instance of SectorDriver. */
    private static SectorDriver _driver;

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public static final SectorDriver getSectorDriver()
        throws SectorException
    {
        if (_driver == null)
            throw new IllegalStateException(
                    "SectorDriverManager is not initialized.");
        return _driver;
    }

    /**
     * Register a SectorDriver implementation class specified by 
     * driverClassName.
     * The class must be implemented {@link SectorDriver}.
     *
     * @param driverClassName   The class name that is implemented
     *                          {@link SectorDriver}.
     *
     * @throws  SectorException
     */
    public static synchronized void setSectorDriver(String driverClassName)
        throws SectorException
    {
        if (driverClassName == null || driverClassName.trim().length() == 0)
            throw new IllegalArgumentException(
                    _INSTANTIATION_ERROR + 
                    "You must sepcify SectorDirver class name.");

        if (_driver != null)
            _log.log(Level.WARNING,
                    "SectorDriver class [" + _driver.getClass().getName() +
                    "] is already regsitered. " +
                    "Restore [" + driverClassName + "].");

        try {
            Class driverClass = Class.forName(driverClassName); 
            if (!SectorDriver.class.isAssignableFrom(driverClass)) {
                throw new IllegalStateException(
                        _INSTANTIATION_ERROR + 
                        "The class specified as SectorDriver does not " +
                        "implement org.sd_network.vfs.sector.SectorDriver " +
                        "interface.");
            }
            _driver = (SectorDriver) driverClass.newInstance();
            _driver.initDriver();
        } catch (Exception e) {
            _log.log(Level.SEVERE, _INSTANTIATION_ERROR, e);
            throw new IllegalStateException(_INSTANTIATION_ERROR);
        }
    }
}
