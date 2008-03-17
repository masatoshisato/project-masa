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
package org.sd_network.vfs;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.sd_network.db.ConnectionPool;
import org.sd_network.util.Config;
import org.sd_network.vfs.db.Schema;
import org.sd_network.vfs.sector.SectorDriverManager;

/**
 * The context for VFS service.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class VfsContext
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            VfsContext.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** System service interface. */
    private static VfsService _service;

    //////////////////////////////////////////////////////////// 
    // Public methods.
    
    /**
     * Initialize the context.
     * this method contains following things.
     * <ul>
     *  <li> check required properties whether there are exist.
     *  <li> Database initialization.
     * </ul>
     */
    public static final void init(String propertyFilePath)
        throws VfsIOException
    {

        // Tests whether already initialized.
        if (_service != null) {
            _log.log(Level.WARNING, "VfsContext already initialized.");
            return;
        }

        try {
            // setup configurations.
            Config config = Config.load(propertyFilePath);

            // setup database connection information.
            ConnectionPool pool = ConnectionPool.getInstance("vfs");
            pool.setJDBCDriver(
                    config.getProperty("org.sd_network.vfs.db.JDBCDriver"));
            pool.setDatabaseURL(
                    config.getProperty("org.sd_network.vfs.db.URL"));
            pool.setDatabaseUserName(
                    config.getProperty("org.sd_network.vfs.db.UserName"));
            pool.setDatabasePassword(
                    config.getProperty("org.sd_network.vfs.db.Password"));

            // setup database schema.
            Schema.setup();

            // SectorDriverManager initialization.
            SectorDriverManager.setSectorDriver(
                    config.getProperty("org.sd_network.vfs.SectorDriver"));

            _service = new VfsService();

        } catch (Throwable t) {
            _log.log(Level.SEVERE, t.getMessage(), t);
            throw new VfsIOException(
                    "System initialization error: " + t.getMessage());
        }
    }

    /**
     * Create new service instance.
     * If return null, System is not initialized.
     */
    public static final VfsService getService() {
        return _service;
    }
}
