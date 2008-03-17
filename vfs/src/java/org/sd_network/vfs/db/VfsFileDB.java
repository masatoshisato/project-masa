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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.db.DBUtil;
import org.sd_network.db.ConnectionPool;
import org.sd_network.db.DBException;

/**
 * The class for create a {@link VfsFile} instance.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class VfsFileDB 
{
    /** Logger */
    private static final Logger _log = Logger.getLogger(
            VfsFileDB.class.getName());

    //////////////////////////////////////////////////////////// 
    // Public fields.

    public static final int OBJECT_ENTRY_SIZE = 487;

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return number of child object of target file.
     *
     * @param fileID    ID of target file.
     * @param ownerID   ID of owner user of target file.
     *
     * @return  Number of child object of target file.
     */
    public static final int countChild(String fileID, String ownerID) {
        if (fileID == null || fileID.trim().length() == 0)
            throw new IllegalArgumentException("fileID is empty.");
        if (ownerID == null || ownerID.trim().length() == 0)
            throw new IllegalArgumentException("ownerID is empty.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT count(*) as number_of_child " +
                    "FROM vfs_file " +
                    "WHERE parent_file_id=? AND owner_id=?");
            stmt.setString(1, fileID);
            stmt.setString(2, ownerID);
            ResultSet rs = stmt.executeQuery();
            VfsFile obj = null;
            rs.next();
            return rs.getInt("number_of_child");
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
     * Return the instance of VfsFile specified by fileID.
     *
     * @param fileID    ID of target file.
     * @param ownerID   ID of owner user of target file.
     *
     * @return  VfsFile intance of target file.
     */
    public static final VfsFile get(String fileID, String ownerID) {
        if (fileID == null || fileID.trim().length() == 0)
            throw new IllegalArgumentException("fileID is empty.");
        if (ownerID == null || ownerID.trim().length() == 0)
            throw new IllegalArgumentException("ownerID is empty.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT file_id, name, type_id, parent_file_id, size, " +
                    " owner_id, created " +
                    "FROM vfs_file " +
                    "WHERE file_id=? AND owner_id=?");
            stmt.setString(1, fileID);
            stmt.setString(2, ownerID);
            ResultSet rs = stmt.executeQuery();
            VfsFile obj = null;
            if (rs.next() ) {
                obj = getInstance(rs);
            }
            return obj;
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
     * Return the instance of VfsFile that is matched with specified arguments.
     *
     * @param parentFileID  File ID of parent of target file.
     * @param fileName      Name of target file.
     * @param ownerID       owner user ID of target file.
     *
     * @return  VfsFile instance of target file.
     */
    public static final VfsFile get(String parentFileID, String fileName,
            String ownerID)
    {
        if (parentFileID == null || parentFileID.trim().length() == 0)
            throw new IllegalArgumentException("parentFileID is empty.");
        if (fileName == null || fileName.trim().length() == 0)
            throw new IllegalArgumentException("fileName is empty.");
        if (ownerID == null || ownerID.trim().length() == 0)
            throw new IllegalArgumentException("ownerID is empty.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT file_id, name, type_id, parent_file_id, size, " +
                    " owner_id, created " +
                    "FROM vfs_file " +
                    "WHERE parent_file_id=? AND name=? AND owner_id=?");
            stmt.setString(1, parentFileID);
            stmt.setString(2, fileName);
            stmt.setString(3, ownerID);
            ResultSet rs = stmt.executeQuery();
            VfsFile obj = null;
            if (rs.next() ) {
                obj = getInstance(rs);
            }
            return obj;
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
     * Return list of instances of VfsFile at underlayer of the parent.
     *
     * @param parentFileID  parent file ID.
     * @param ownerID       owner user ID.
     *
     * @return  list of instances of VfsFile at underlayer of the 
     *          parent object.
     */
    public static final List<VfsFile> getChildList(String parentFileID,
            String ownerID)
    {
        // check parameters
        if (parentFileID == null || parentFileID.trim().length() == 0)
            throw new IllegalArgumentException("parentFileID is empty.");
        if (ownerID == null || ownerID.trim().length() == 0)
            throw new IllegalArgumentException("ownerID is empty.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT file_id, name, type_id, parent_file_id, size, " +
                    " owner_id, created " +
                    "FROM vfs_file " +
                    "WHERE parent_file_id=? AND owner_id=?");
            stmt.setString(1, parentFileID);
            stmt.setString(2, ownerID);
            ResultSet rs = stmt.executeQuery();
            List<VfsFile> objList = new ArrayList<VfsFile>();
            while (rs.next()) {
                objList.add(getInstance(rs));
            }
            return objList;
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
     * Create a directory object.
     * This object has some fixed values as following.
     * <ul>
     *  <li> object type is "1" (directory).
     *  <li> size is "0".
     * </ul>
     *
     * <p> This method commit transaction automatically.
     *
     * @param name object name.
     * @param parentID parent object ID of this object.
     * @param ownerID owner user ID of this object.
     *
     * @return created object.
     */
    public static final VfsFile createDirectory(String name, String parentID,
            String ownerID)
    {
        return create(name, 1, parentID, 0, ownerID);
    }

    /**
     * Create a directory object.
     * This object has some fixed values as following.
     * <ul>
     *  <li> object type is "1" (directory).
     *  <li> size is "0".
     * </ul>
     *
     * <p> This method do not commit or rollback transaction automatically,
     * these function depend on the autocommit mode of specified connection.
     *
     * @param con       Database connection object.
     * @param name      Object name.
     * @param parentID  Parent object ID of this object.
     * @param ownerID   Owner user ID of this object.
     *
     * @return created object.
     */
    public static final VfsFile createDirectory(Connection con, String name,
            String parentID, String ownerID)
    {
        return create(con, name, 1, parentID, 0, ownerID);
    }

    /**
     * Create a file object.
     * This object has some fixed values as following.
     * <ul>
     *  <li> object type is "2" (file).
     * </ul>
     *
     * <p> This method commit transaction automatically.
     *
     * @param name  object name.
     * @param parentID  parent object ID of this object.
     * @param size content size of this object. (byte)
     * @param ownerID owner user ID of this object.
     *
     * @return create object.
     */
    public static final VfsFile createFile(String name, String parentID,
            long size, String ownerID)
    {
        return create(name, 2, parentID, size, ownerID);
    }

    /**
     * Create a file object.
     * This object has some fixed values as following.
     * <ul>
     *  <li> object type is "2" (file).
     * </ul>
     *
     * <p> This method do not commit or rollback transaction automatically,
     * these functions depend on the autocommit mode of specified connection.
     *
     * @param con       Database connection object.
     * @param name      Object name.
     * @param parentID  Parent object ID of this object.
     * @param size      Content size of this object. (byte)
     * @param ownerID   Owner user ID of this object.
     *
     * @return create object.
     */
    public static final VfsFile createFile(Connection con, String name,
            String parentID, long size, String ownerID)
    {
        return create(con, name, 2, parentID, size, ownerID);
    }


    public static final void delete(String fileID, String ownerID) {
        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            con.setAutoCommit(true);
            delete(con, fileID, ownerID);
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

    public static final void delete(Connection con, String fileID,
            String ownerID)
    {
        if (con == null)
            throw new IllegalArgumentException("con is null.");
        if (fileID == null)
            throw new IllegalArgumentException("fileID is empty.");
        if (ownerID == null)
            throw new IllegalArgumentException("ownerID is empty.");

        DBUtil.update(
                con,
                "DELETE FROM vfs_file WHERE file_id=? AND owner_id=?",
                new Object[] {fileID, ownerID});
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    private static final VfsFile create(String name, int typeID,
            String parentID, long size, String ownerID)
    {
        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            con.setAutoCommit(true);
            return create(con, name, typeID, parentID, size, ownerID);
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

    private static final VfsFile create(Connection con, String name,
            int typeID, String parentID, long size, String ownerID)
    {
        if (con == null)
            throw new IllegalArgumentException("con is null.");
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name is empty.");

        HashMap<String, Object> columnMap = new HashMap<String, Object>();
        String objectID = UUID.randomUUID().toString();
        Date now = new Date();
        columnMap.put("file_id", objectID);
        columnMap.put("name", name);
        columnMap.put("type_id", new Integer(typeID));
        columnMap.put("parent_file_id", parentID);
        columnMap.put("size", new Long(size));
        columnMap.put("owner_id", ownerID);
        columnMap.put("created", now);
        if (DBUtil.insert(con, "vfs_file", columnMap) != 1)
            throw new DBException("new object could not insert to db.");
        return new VfsFile(
                objectID, name, typeID, parentID, size, ownerID, now);
    }

    private static final VfsFile getInstance(ResultSet rs)
        throws SQLException
    {
        return new VfsFile(
                rs.getString("file_id"),
                rs.getString("name"),
                rs.getInt("type_id"),
                rs.getString("parent_file_id"),
                rs.getLong("size"),
                rs.getString("owner_id"),
                rs.getTimestamp("created"));
    }
}
