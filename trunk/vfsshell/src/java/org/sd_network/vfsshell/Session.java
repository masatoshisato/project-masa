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
package org.sd_network.vfsshell;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfs.db.VfsFile;

/**
 * A session folder.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Session
{
    /** Logger */
    private static final Logger _log = Logger.getLogger(
            Session.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Instance of Session. */
    private static Session _instance = new Session();

    /** Session ID of VFS */
    private String _sessionID;

    /** Current directory. */
    private VfsFile _currentDirectory;

    /** file instance cache. */
    private Map<String, VfsFile> _cachedFileMap =
        new HashMap<String, VfsFile>();

    //////////////////////////////////////////////////////////// 
    // Initializations and Factories.

    /**
     * Default constructor.
     */
    private Session() {
    }

    public static final Session getInstance() {
        return _instance;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public String getSessionID() {
        return _sessionID;
    }

    public void setSessionID(String sessionID) {
        if (_sessionID != null)
            _log.log(Level.WARNING, "Session was overwritten.");
        _sessionID = sessionID;
    }

    public void clearSessionID() {
        _sessionID = null;
    }

    public VfsFile getCurrentDirectory() {
        return _currentDirectory;
    }

    public void setCurrentDirectory(VfsFile directory) {
        _currentDirectory = directory;
    }

    public void cacheVfsFile(VfsFile vfsFile) {
        if (vfsFile == null)
            throw new IllegalArgumentException("vfsFile is null.");
        _cachedFileMap.put(vfsFile.getName(), vfsFile);
    }

    public VfsFile getFileFromCache(String fileName) {
        if (fileName == null)
            throw new IllegalArgumentException("fileName is null.");
        return _cachedFileMap.get(fileName);
    }
}
