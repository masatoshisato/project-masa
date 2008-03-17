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
package org.sd_network.vfs.db;

/**
 * A class represent a User.
 * There is not password in this instance.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class User
{
    //////////////////////////////////////////////////////////// 
    // Private fields.

    private final String _userID;
    private final String _loginName;
    private final boolean _isAdmin;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * Constructor for same package.
     * This method is generally used by UserDB.
     *
     * @param userID    user id.
     * @param loginName login name.
     */
    User(String userID, String loginName, boolean isAdmin) {
        _userID = userID;
        _loginName = loginName;
        _isAdmin = isAdmin;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return the userID.
     */
    public String getID() {
        return _userID;
    }

    /**
     * Return the login name.
     */
    public String getLoginName() {
        return _loginName;
    }

    /**
     * Return true if this user has permission as an Administrator.
     */
    public boolean isAdmin() {
        return _isAdmin;
    }
}
