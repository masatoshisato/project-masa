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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.sd_network.util.Config;
import org.sd_network.util.InvalidConfigException;
import org.sd_network.vfs.db.User;
import org.sd_network.vfs.db.UserDB;

/**
 * Manage user sessions.
 * This class is made by singleton pattern. When use this class, call
 * <@link #getInstance()> method as follow.
 *
 * <p><pre>UserSessionManager usm = UserSessionManager.getInstance();</pre>
 *
 * <p> 
 * This class refer to property "org.sd_network.vfs.UserSession.Max". 
 * This property represent Maximum number of user session that is stored in 
 * this instance. The user session is counted when call 
 * <@link #authenticate(String, String)> method and logged in was success. 
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class UserSessionManager
{
    /** Logger */
    private static final Logger _log = Logger.getLogger(
            UserSessionManager.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** The instance of this class. */
    private static UserSessionManager _instance = null;

    /** Maximum number of user session. */
    private final int _maxUserSession;

    /** The session buffer. */
    private Map<String, UserSession> _userSessionMap =
        new HashMap<String, UserSession>();

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * A default constructor
     * Set maximum of session number.
     */
    private UserSessionManager() {
        Config config = Config.getInstance();
        try {
            _maxUserSession = Integer.parseInt(
                    config.getProperty("org.sd_network.vfs.UserSession.Max"));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(
                    "org.sd_network.vfs.UserSession.Max", e);
        }
    }

    /**
     * Return instance of this class.
     * the instance is created if it is not created, and it is a singleton
     * instance.
     *
     * @return  The instance of this class.
     */
    public static final UserSessionManager getInstance() {
        if (_instance == null)
            _instance = new UserSessionManager();
        return _instance;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Execute authenticate specified User.
     * If authentication faild, throw AuthenticationException
     *
     * @param loginName     Login name of user.
     * @param password      Password of user.
     *
     * @return  created new sessionID.
     *
     * @throws AuthentictionExcepiton
     *          Throw when authenticate failed.
     */
    public String authenticate(String loginName, String password)
        throws AuthenticationException
    {
        // check maximum of session number.
        if (_userSessionMap.size() >= _maxUserSession)
            throw new AuthenticationException(
                    "Session full. Please try again after a wait few minutes.");

        // check whether User exists.
        User user = UserDB.get(loginName, password);
        if (user == null)
            throw new AuthenticationException("User not found.");

        // create session.
        String userSessionID = UUID.randomUUID().toString();
        _userSessionMap.put(
                userSessionID, new UserSession(userSessionID, user));
        return userSessionID;
    }

    /**
     * Return instance of UserSession that is related to <code>sessionID</code>
     * from session buffer.
     *
     * @param userSessionID     The user session ID before published.
     *
     * @return  The user instance. If session which specified by sessionID is
     *          not exists, return null.
     */
    public UserSession getUserSession(String userSessionID) {
        return _userSessionMap.get(userSessionID);
    }

    /**
     * Destroy the session related to <code>sessionID</code> and remove it
     * from session buffer.
     *
     * @param userSessionID     User session id which destroy.
     *
     * @throws  VfsIOException
     *          Throws if any error occurred.
     */
    public void destroy(String userSessionID)
        throws VfsIOException
    {
        UserSession userSession = _userSessionMap.remove(userSessionID);
        userSession.destroy();
    }
}
