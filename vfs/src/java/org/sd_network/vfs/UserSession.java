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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.util.Config;
import org.sd_network.util.InvalidConfigException;
import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfs.db.User;

/**
 * This class represent a user session.
 * The instance of this class store following informations.
 * <ul>
 *  <li> The session ID.
 *  <li> Collection of a file session.
 *  <li> The instance of the User class that is logged in.
 * </ul>
 *
 * <p> Maximum number of file session that can stored is specified by
 * property "org.sd_network.vfs.FileSession.Max" in the property file.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class UserSession
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            UserSession.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Maximum number of file session. */
    private final int _maxFileSession;

    /** The file session buffer. */
    private Map<String, FileSession> _fileSessionMap =
        new HashMap<String, FileSession>();

    /** The user session ID. */
    private final String _userSessionID;

    /** The logged in user. */
    private final User _user;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    UserSession(String userSessionID, User user) {
        _userSessionID = userSessionID;
        _user = user;
        Config config = Config.getInstance();
        try {
            _maxFileSession = Integer.parseInt(
                    config.getProperty("org.sd_network.vfs.FileSession.Max"));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(
                    "org.sd_network.vfs.FileSession.Max", e);
        }
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return logged in user.
     *
     * @return Instance of user.
     */
    public User getUser() {
        return _user;
    }

    /**
     * Return user session ID of this session.
     *
     * @return session ID.
     */
    public String getSessionID() {
        return _userSessionID;
    }

    /**
     * Create file session and return it.
     * If number of file session that is created is already max, throws
     * VfsIOException.
     *
     * <p> This method is synchronized.
     *
     * @param targetFile    The target file.
     * @param mode          Session mode.
     *
     * @return  Created file session iD.
     *
     * @throws  VfsIOException
     *          Throws if created file session already max.
     */
    public synchronized String newFileSession(VfsFile targetFile,
            FileSession.Mode mode)
        throws VfsIOException
    {
        if (targetFile == null)
            throw new IllegalArgumentException("targetFile is null.");
        if (mode == null)
            throw new IllegalArgumentException("mode is null.");

        // check maximum number of file session.
        if (_maxFileSession == _fileSessionMap.size())
            throw new VfsIOException(
                    "You can not create file session any more.");

        // check whether file session specified file already exists.
        if (hasFileSession(targetFile))
            throw new VfsIOException("The file session already exists.");

        // create file session.
        String fileSessionID = UUID.randomUUID().toString();
        FileSession fileSession = null;
        if (mode == FileSession.Mode.READ)
            fileSession = new ReadFileSession(fileSessionID, targetFile);
        else if (mode == FileSession.Mode.WRITE)
            fileSession = new WriteFileSession(fileSessionID, targetFile);
        else if (mode == FileSession.Mode.APPEND)
            throw new UnsupportedOperationException("Fix this.");
        else
            throw new UnsupportedOperationException("Fix this.");
        _fileSessionMap.put(fileSessionID, fileSession);

        return fileSessionID;
    }

    public void closeFileSession(String fileSessionID)
        throws VfsIOException
    {
        if (fileSessionID == null)
            throw new IllegalArgumentException("fileSessionID is null.");

        FileSession fileSession = _fileSessionMap.remove(fileSessionID);
        if (fileSession == null) {
            _log.log(Level.INFO,
                    "File session [" + fileSessionID + "] is not found.");
            return;
        }
        fileSession.close();
    }

    public FileSession getFileSession(String fileSessionID)
        throws VfsIOException
    {
        if (fileSessionID == null)
            throw new NullPointerException("fileSessionID");

        return _fileSessionMap.get(fileSessionID);
    }

    public boolean hasFileSession(VfsFile targetFile) {
        if (targetFile == null)
            throw new IllegalArgumentException("targetFile is null.");

        return hasFileSession(targetFile.getID());
    }

    public boolean hasFileSession(String fileID) {
        if (fileID == null || fileID.trim().length() == 0)
            throw new IllegalArgumentException("fileID is empty.");

        Iterator<FileSession> fileSessions =
            _fileSessionMap.values().iterator();
        for (; fileSessions.hasNext(); ) {
            FileSession fileSession = fileSessions.next();
            if (fileSession.getFile().getID().equals(fileID))
                return true;
        }
        return false;
    }

    public synchronized void destroy()
        throws VfsIOException
    {
        Iterator<FileSession> fileSessions =
            _fileSessionMap.values().iterator();
        for (; fileSessions.hasNext(); ) {
            FileSession fileSession = fileSessions.next();
            fileSession.destroy();
        }
        _fileSessionMap.clear();
    }
}
