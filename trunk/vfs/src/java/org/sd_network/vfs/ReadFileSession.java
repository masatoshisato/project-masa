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

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfs.sector.SectorDriver;
import org.sd_network.vfs.sector.SectorDriverManager;
import org.sd_network.vfs.sector.SectorException;

/**
 * @author Masatoshi Sato
 */
public class ReadFileSession
    extends FileSession
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ReadFileSession.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Instance of input stream. */
    private InputStream _iStream;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    ReadFileSession(String sessionID, VfsFile vfsFile)
        throws VfsIOException
    {
        super(sessionID, vfsFile, FileSession.Mode.READ);
        try {
            SectorDriver driver = SectorDriverManager.getSectorDriver();
            _iStream = driver.getInputStream(vfsFile.getID());
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
            _iStream.close();
            _closed = true;
        } catch (IOException e) {
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
            _iStream.close();
            _closed = true;
        } catch (IOException e) {
            throw new VfsIOException(e);
        }
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public int read(byte[] buff, int size)
        throws VfsIOException
    {
        checkClosed();
        try {
            return _iStream.read(buff, 0, size);
        } catch (IOException e) {
            throw new VfsIOException(e);
        }
    }
}
