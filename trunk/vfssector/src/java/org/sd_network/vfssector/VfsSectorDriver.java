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
package org.sd_network.vfssector;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.db.DBException;
import org.sd_network.db.ConnectionPool;
import org.sd_network.db.ConnectionPoolException;
import org.sd_network.util.Config;
import org.sd_network.vfs.sector.SectorDriver;
import org.sd_network.vfs.sector.SectorException;
import org.sd_network.vfssector.db.Schema;
import org.sd_network.vfssector.db.Sector;
import org.sd_network.vfssector.db.SectorDB;

/**
 * A {@link org.sd_network.vfs.sector.SectorDriver} implementation for
 * vfssector.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class VfsSectorDriver
    implements SectorDriver
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            VfsSectorDriver.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Property name of available bytes. */
    private static final String _PROP_AVAILABLEBYTES =
        "org.sd_network.vfssector.AvailableBytes";

    /** initialized flag. */
    private boolean _initialized = false;

    /** Read locked fileID collection. */
    private Set<String> _readLockedSet = new HashSet<String>();

    /** Write locked fileID collection. */
    private Set<String> _writeLockedSet = new HashSet<String>();

    /** available bytes in this Driver. */
    private long _availableBytes;

    /**
     * Used bytes.
     * This value is cleared when getOutputStream method call.
     */
    private long _usedBytes;

    //////////////////////////////////////////////////////////// 
    // Implements to SectorDriver.
    
    public synchronized OutputStream getOutputStream(String fileID)
        throws SectorException
    {
        checkInitialized();
        checkReadLocked(fileID);
        checkWriteLocked(fileID);

        _writeLockedSet.add(fileID);
        clearUsedBytes();
        try {
            return new SectorOutputStream(fileID, this);
        } catch (Exception e) {
            throw new SectorException(e);
        }
    }

    public synchronized OutputStream getOutputStream(String fileID,
            boolean append)
        throws SectorException
    {
        if (!append)
            return getOutputStream(fileID);

        checkInitialized();
        checkReadLocked(fileID);
        checkWriteLocked(fileID);

        _writeLockedSet.add(fileID);
        clearUsedBytes();
        try {
            Sector lastSector = SectorDB.getLastSector(fileID);
            return new SectorOutputStream(lastSector, this);
        } catch (Exception e) {
            throw new SectorException(e);
        }
    }

    public synchronized InputStream getInputStream(String fileID)
        throws SectorException
    {
        checkInitialized();
        checkWriteLocked(fileID);

        _readLockedSet.add(fileID);
        try {
            return new SectorInputStream(fileID, this);
        } catch (Exception e) {
            throw new SectorException(e);
        }
    }

    public boolean isWriteLocked(String fileID) {
        return _writeLockedSet.contains(fileID);
    }

    public boolean isReadLocked(String fileID) {
        return _readLockedSet.contains(fileID);
    }

    public synchronized void deleteSectors(String fileID)
        throws SectorException
    {
        checkInitialized();
        checkReadLocked(fileID);
        checkWriteLocked(fileID);

        SectorDB.deleteSectors(fileID);
        clearUsedBytes();
    }

    public void initDriver()
        throws SectorException
    {
        if (_initialized)
            return;

        // setup database connection informations.
        try {
            ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        } catch (ConnectionPoolException e) {
            throw new SectorException(e.getMessage());
        }

        try {
            // setup database scheme.
            Schema.setup();
        } catch (DBException e) {
            throw new SectorException(
                    "Database error occurred. " + e.getMessage(), e);
        }

        // setup etc.
        Config config = Config.getInstance();
        try {
            _availableBytes = Long.parseLong(
                    config.getProperty(_PROP_AVAILABLEBYTES));
        } catch (NumberFormatException e) {
            throw new SectorException(
                    "Invalid property " + _PROP_AVAILABLEBYTES + "]. " +
                    e.getMessage(), e);
        }
        clearUsedBytes();
        _initialized = true;
    }

    public long getFileSize(String fileID)
        throws SectorException
    {
        checkInitialized();
        checkWriteLocked(fileID);

        try {
            return SectorDB.getFileSize(fileID);
        } catch (Exception e) {
            throw new SectorException(e);
        }
    }

    public long getAvailableBytes()
        throws SectorException
    {
        checkInitialized();
        return _availableBytes;
    }

    public long getUsedBytes()
        throws SectorException
    {
        checkInitialized();
        if (isUsedBytesClear())
            _usedBytes = SectorDB.getUsedBytes();
        return _usedBytes;
    }

    //////////////////////////////////////////////////////////// 
    // Package methods.

    void releaseWriteLock(String fileID) {
        _writeLockedSet.remove(fileID);
    }

    void releaseReadLock(String fileID) {
        _readLockedSet.remove(fileID);
    }

    int getDefaultSectorSize() {
        return 1048576;
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    private void checkInitialized()
        throws SectorException
    {
        if (!_initialized)
            throw new SectorException("SectorDriver not initialized.");
    }

    private void checkReadLocked(String fileID)
        throws SectorException
    {
        if (_readLockedSet.contains(fileID))
            throw new SectorException(
                    "Speicified file have been read locked.");
    }

    private void checkWriteLocked(String fileID)
        throws SectorException
    {
        if (_writeLockedSet.contains(fileID))
            throw new SectorException(
                    "Specified file have been write locked.");
    }

    /**
     * Clear used bytes, i.e. set to -1.
     */
    private void clearUsedBytes() {
        _usedBytes = -1;
    }

    /**
     * Test whether used bytes is cleared.
     */
    private boolean isUsedBytesClear() {
        return _usedBytes == -1;
    }
}
