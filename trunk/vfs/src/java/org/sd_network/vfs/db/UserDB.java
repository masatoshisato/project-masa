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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sd_network.db.ConnectionPool;
import org.sd_network.db.DBException;
import org.sd_network.db.DBUtil;

/**
 * The class to create {@link User} instance.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class UserDB
{
    /** Logger */
    private static final Logger _log = Logger.getLogger(
            UserDB.class.getName());
    
    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return instance of the User class.
     *
     * @param loginName     login name.
     * @param password      password.
     *
     * @return  Instance of User entity. If User entity not found in
     *          the database, return null.
     *
     * @throws  DBException
     *          Throws when database error is occured.
     */
    public static final User get(String loginName, String password) {
        if (loginName == null || loginName.length() == 0)
            throw new IllegalArgumentException("loginName is empty.");
        if (password == null || password.length() == 0)
            throw new IllegalArgumentException("password is empty.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT user_id, login_name, is_admin FROM user " +
                    " WHERE login_name = ? AND password = ?");
            stmt.setString(1, loginName);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            User user = null;
            if (rs.next()) {
                user = new User(
                        rs.getString("user_id"),
                        rs.getString("login_name"),
                        rs.getBoolean("is_admin"));
            }
            return user;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.",e);
            }
        }
    }
    
    /**
     * Create new User as generic user to Database, and return the instance
     * of it.
     * This method commit transaction automatically.
     *
     * @param loginName     login name.
     * @param password      password.
     *
     * @return  Instance of User entity.
     *
     * @throws DBException
     *          Throws when database error occured.
     *
     * @throws IllegalArgumentExcepiton
     *          Throws when each argument is specified null or emtpy.
     */
    public static final User create(String loginName, String password) {
        return create(loginName, password, false);
    }

    /**
     * Create a new User to Database, and return the instance of it.
     * This method commit transaction automatically.
     *
     * @param loginName     login name.
     * @param password      password.
     * @param isAdmin       if a new User is Administrator, set true.
     *
     * @return  The instance of the new User entry.
     *
     * @throws DBException
     *          Throws when database error occurred.
     *
     * @throws IllegalArgumentException
     *          Throws when each argument is specified null or empty.
     */
    public static final User create(String loginName, String password, 
            boolean isAdmin)
    {
        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            con.setAutoCommit(true);
            return create(con, loginName, password, isAdmin);
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.",e);
            }
        }
    }

    /**
     * Create a new User to Database, and return the instance of it.
     * This method do not commit or rollback transaction automatically,
     * these function depend on the autocommit mode of specified connection.
     *
     * @param con           Database connection object.
     * @param loginName     login name.
     * @param password      password.
     * @param isAdmin       if a new User is Administrator, set true.
     *
     * @return  The instance of the new User entry.
     *
     * @throws DBException
     *          Throws when database error occurred.
     *
     * @throws IllegalArgumentException
     *          Throws when each argument is specified null or empty.
     */
    public static final User create(Connection con, String loginName,
            String password, boolean isAdmin)
    {
        if (con == null)
            throw new IllegalArgumentException("con is null.");
        if (loginName == null || loginName.length() == 0)
            throw new IllegalArgumentException("loginName is empty.");
        if (password == null || password.length() == 0)
            throw new IllegalArgumentException("password is empty.");

        HashMap<String, Object> columnMap = new HashMap<String, Object>();
        String userID = UUID.randomUUID().toString();
        columnMap.put("user_id", userID);
        columnMap.put("login_name", loginName);
        columnMap.put("password", password);
        columnMap.put("is_admin", new Boolean(isAdmin));
        if (DBUtil.insert(con, "user", columnMap) != 1)
            throw new DBException("new user could not insert to db.");
        return new User(userID, loginName, isAdmin);
    }
}
