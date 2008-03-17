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

import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfs.db.VfsFile;

/**
 * An abstract file session.
 *
 * @author Masatoshi Sato
 */
public abstract class FileSession
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            FileSession.class.getName());

    //////////////////////////////////////////////////////////// 
    // Public fields.

    /** Session mode. */
    public enum Mode {
        READ(1),
        WRITE(2),
        APPEND(3);

        private final int _value;

        Mode(int value) {
            _value = value;
        }

        public int getValue() {
            return _value;
        }
    };

    //////////////////////////////////////////////////////////// 
    // Protected fields.

    /** Session ID. */
    protected final String _sessionID;

    /** Instance of VfsFile. */
    protected VfsFile _vfsFile;

    /** FileSession mode. */
    protected final Mode _mode;

    /** Flag for this session closed. */
    protected boolean _closed;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    protected FileSession(String sessionID, VfsFile vfsFile, Mode mode) {
        _sessionID = sessionID;
        _vfsFile = vfsFile;
        _mode = mode;
        _closed = false;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public String getID() {
        return _sessionID;
    }

    public VfsFile getFile() {
        return _vfsFile;
    }

    public Mode getMode() {
        return _mode;
    }

    public void finalize()
        throws Throwable
    {
        super.finalize();
        destroy();
    }

    //////////////////////////////////////////////////////////// 
    // Protected methods.

    protected void checkClosed() {
        if (_closed)
            throw new IllegalStateException(
                    "The file session is already closed.");
    }

    //////////////////////////////////////////////////////////// 
    // Abstract methods.

    public abstract void close()
        throws VfsIOException;

    public abstract void destroy()
        throws VfsIOException;
}
