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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfs.sector.SectorDriverManager;
import org.sd_network.vfs.sector.SectorDriver;
import org.sd_network.vfs.sector.SectorException;

/**
 * @author Masatoshi Sato
 */
public class WriteFileSession
    extends FileSession
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            WriteFileSession.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Wrote file size. */
    private long _fileSize;

    /** Instance of output stream. */
    private OutputStream _oStream;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    WriteFileSession(String sessionID, VfsFile vfsFile)
        throws VfsIOException
    {
        super(sessionID, vfsFile, FileSession.Mode.WRITE);
        _fileSize = 0;
        vfsFile.resizeTo(_fileSize);
        try {
            SectorDriver driver = SectorDriverManager.getSectorDriver();
            driver.deleteSectors(vfsFile.getID());
            _oStream = driver.getOutputStream(vfsFile.getID());
        } catch (SectorException e) {
            throw new VfsIOException(e);
        }
    }

    //////////////////////////////////////////////////////////// 
    // Implements to FileSession.

    public void close()
        throws VfsIOException
    {
        checkClosed();

        try {
            _oStream.close();
            SectorDriver driver = SectorDriverManager.getSectorDriver();
            _vfsFile.resizeTo(driver.getFileSize(_vfsFile.getID()));
            _closed = true;
        } catch (IOException e) {
            throw new VfsIOException(e);
        } catch (SectorException e) {
            throw new VfsIOException(e);
        }
    }

    public void destroy()
        throws VfsIOException
    {
        if (_closed)
            return;

        _log.log(Level.WARNING, "Called destroy() without call close()");
        try {
            _oStream.close();
            SectorDriver driver = SectorDriverManager.getSectorDriver();
            driver.deleteSectors(_vfsFile.getID());
            _vfsFile.resizeTo(0);
            _closed = true;
        } catch (IOException e) {
            throw new VfsIOException(e);
        } catch (SectorException e) {
            throw new VfsIOException(e);
        }
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public void write(byte[] data, int size)
        throws VfsIOException
    {
        checkClosed();
        try {
            _oStream.write(data, 0, size);

            // XXX: I wonder file size should write to the db each time...
            _fileSize += size;

        } catch (IOException e) {
            throw new VfsIOException(e);
        }
    }
}
