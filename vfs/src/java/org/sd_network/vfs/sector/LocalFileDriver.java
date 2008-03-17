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

/**
 * A reference implementation of {@link SectorDriver}.
 * This class implement SectorDriver used by Local file system.
 * The file name of each files is represented by <code>fileID</code>.
 * All of the limitation of this driver depend on the local file system.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class LocalFileDriver
    implements SectorDriver
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            LocalFileDriver.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Read locked fileID collection. */
    private Set<String> _readLockedSet = new HashSet<String>();
    
    /** Write locekd fileID collection. */
    private Set<String> _writeLockedSet = new HashSet<String>();

    //////////////////////////////////////////////////////////// 
    // Implements SectorDriver.
    
    public OutputStream getOutputStream(String fileID)
        throws SectorException
    {
        return getOutputStream(fileID, false);
    }

    public OutputStream getOutputStream(String fileID, boolean append)
        throws SectorException
    {
        try {
            OutputStream os = new FileOutputStream(fileID, append);
            _writeLockedSet.add(fileID);
            return os;
        } catch (Exception e) {
            throw new SectorException(
                    "OutputStream could not create.", e);
        }
    }

    public InputStream getInputStream(String fileID)
        throws SectorException
    {
        try {
            InputStream is = new FileInputStream(fileID);
            _readLockedSet.add(fileID);
            return is;
        } catch (Exception e) {
            throw new SectorException(
                    "InputStream could not create.", e);
        }
    }

    public boolean isWriteLocked(String fileID) {
        return _writeLockedSet.contains(fileID);
    }

    public boolean isReadLocked(String fileID) {
        return _readLockedSet.contains(fileID);
    }

    public void deleteSectors(String fileID)
        throws SectorException
    {
        try {
            File target = new File(fileID);
            if (target.exists())
                target.delete();
        } catch (Exception e) {
            throw new SectorException(fileID + " could not delete.", e);
        }
    }

    public void initDriver() {
        // do nothing.
    }

    public long getFileSize(String fileID)
        throws SectorException
    {
        try {
            File target = new File(fileID);
            return target.length();
        } catch (Exception e) {
            throw new SectorExeption(e);
        }
    }

    public long getAvailableBytes()
        throws SectorException
    {
    }

    public long getUsedBytes()
        throws SectorException
    {
    }

    //////////////////////////////////////////////////////////// 
    // private methods.

    private void checkReadLocked(String fileID)
        throws SectorException
    {
        if (_readLockedSet.contains(fileID))
            throw new SectorException(
                    "Specified file have been read locked.");
    }

    private void checkWriteLocked(String fileID)
        throws SectorException
    {
        if (_writeLockedSet.contains(fileID))
            throw new SectorException(
                    "Specified file have been write locked.");
    }
}
