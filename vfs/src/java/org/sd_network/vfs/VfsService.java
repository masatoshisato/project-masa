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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.db.DBException;
import org.sd_network.db.ConnectionPool;
import org.sd_network.vfs.db.User;
import org.sd_network.vfs.db.UserDB;
import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfs.db.VfsFileDB;
import org.sd_network.vfs.sector.SectorDriver;
import org.sd_network.vfs.sector.SectorDriverManager;
import org.sd_network.vfs.sector.SectorException;
import org.sd_network.util.Config;

/**
 * A facade class for all of vfs interface.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class VfsService
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            VfsService.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    //////////////////////////////////////////////////////////// 
    // File system APIs.

    /**
     * Login to this system by specified user account.
     * The user account was registered to this system first.
     * In order to register user account, you can call {@link #addUser} method.
     *
     * @param loginName Login name of user.
     * @param password password of user.
     * 
     * @return  the session ID if login was successful.
     *
     * @throws  NullPointerException
     *          Throws if null was specified to these parameters.
     *
     * @throws  AuthenticationException
     *          Throws if login failed.
     */
    public String login(String loginName, String password)
        throws AuthenticationException
    {
        if (loginName == null)
            throw new NullPointerException("loginName");
        if (password == null)
            throw new NullPointerException("password");

        UserSessionManager usm = UserSessionManager.getInstance();
        return usm.authenticate(loginName, password);
    }

    /**
     * Logout from this system.
     *
     * @param sessionID the session ID that is logged in.
     *
     * @throws  NullPointerException
     *          Throws if null was specified to the parameter.
     *
     * @throws  VfsIOException
     *          Throws if any error occurred when execute destroy.
     */
    public void logout(String sessionID)
        throws VfsIOException
    {
        if (sessionID == null)
            throw new NullPointerException("sessionID");

        UserSessionManager usm = UserSessionManager.getInstance();
        usm.destroy(sessionID);
    }

    /**
     * Return array of instances that is child of specified parent file.
     * You can get home directory if you specifiy "-1" to <tt>parentFileID</tt>.
     *
     * @param sessionID     user session ID that is logged in.
     * @param parentFileID  parent file ID.
     *
     * @return  Array of instances that is child of specified parent file.
     *          This value ensure that to be set a instance of array.
     *          The array is contained one or more instances of VfsFile,
     *          or is empty.
     *
     * @throws   NullPointerException
     *          Throws if null was specified to these parameters.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public VfsFile[] getVfsFiles(String sessionID, String parentFileID)
        throws SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (parentFileID == null)
            throw new NullPointerException("parentFileID");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User owner = userSession.getUser();

        List<VfsFile> childList =
            VfsFileDB.getChildList(parentFileID, owner.getID());

        VfsFile[] children = new VfsFile[childList.size()];
        for (int idx = 0; idx < childList.size(); idx++) {
            children[idx] = childList.get(idx);
        }
        return children;
    }

    /**
     * Return the instance of VfsFile specified by name.
     * You can get home directory if you specifiy "-1" to
     * <tt>parentFileID</tt> and "Home" to <tt>fileName</tt>.
     *
     * @param sessionID     user session ID that is logged in.
     * @param parentFileID  parent file ID.
     * @param fileName      file name of target file.
     *
     * @return  the instance of file.
     *          If the file not found, return null.
     *
     * @throws  NullPointerException
     *          throws if null was specified to these parameters.
     *
     * @throws  SessionException
     *          throws if the sesion is invalidated or timeout.
     */
    public VfsFile getVfsFile(String sessionID, String parentFileID,
            String fileName)
        throws SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (parentFileID == null)
            throw new NullPointerException("parentFileID");
        if (fileName == null)
            throw new NullPointerException("fileName");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User owner = userSession.getUser();

        // retrive target.
        return VfsFileDB.get(parentFileID, fileName, owner.getID());
    }

    /**
     * Return the instance of parent of file specified by <tt>targetFileID</tt>.
     *
     * @param sessionID     user session ID that is logged in.
     * @param targetFileID  target file ID.
     *
     * @return  the instance of parent file.
     *          If the file not found, return null.
     *
     * @throws  NullPointerException
     *          Throws if null was specified to these parameters.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public VfsFile getParent(String sessionID, String targetFileID)
        throws SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (targetFileID == null)
            throw new NullPointerException("targetFileID");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();

        // retrive target.
        VfsFile target = VfsFileDB.get(targetFileID, user.getID());
        if (target == null)
            return null;

        // retrive parent.
        return VfsFileDB.get(target.getParentID(), user.getID());
    }

    /**
     * Move object to other parent.
     *
     * @param sessionID         user session ID that is logged in.
     * @param targetFileID      the fileID of target file.
     * @param newParentFileID   the fileID of new parent to move.
     * @param newFileName       new file name.
     *
     * @return  new instance of moved VfsFile.
     *
     * @throws  VfsIOException
     *          Throws if specified file was not found, specified file name
     *          was too long or including invalid character, or other error
     *          occurred.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public VfsFile moveObject(String sessionID, String targetFileID,
            String newParentFileID, String newFileName)
        throws VfsIOException, SessionException
    {
        throw new UnsupportedOperationException("Please implement.");
    }

    /**
     * Delete database entry of specified VfsFile.
     *
     * @param sessionID     user session ID that is logged in.
     * @param fileID        ID of delete file.
     *
     * @throws  VfsIOException
     *          Throws if specified file was not found, has some child
     *          objects, or other error occurred.
     * 
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public void deleteObject(String sessionID, String fileID)
        throws VfsIOException, SessionException
    {
        // check parameters.
        if (sessionID == null || sessionID.trim().length() == 0)
            throw new IllegalArgumentException("sessionID is empty.");
        if (fileID == null || fileID.trim().length() == 0)
            throw new IllegalArgumentException("fileID is empty.");

        // retrive user from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession session = usm.getUserSession(sessionID);
        if (session == null)
            throw new SessionException("Invalid session.");
        User user = session.getUser();

        // check whether file exists.
        VfsFile target = VfsFileDB.get(fileID, user.getID());
        if (target == null)
            throw new VfsIOException("The object not found.");

        // check whether target file has child.
        List<VfsFile> childList =
            VfsFileDB.getChildList(target.getID(), user.getID());
        if (childList.size() > 0)
            throw new VfsIOException("The object has child object.");

        // delete sectors related to specified fileID.
        try {
            SectorDriver sectorDriver = SectorDriverManager.getSectorDriver();
            sectorDriver.deleteSectors(fileID);
        } catch (SectorException e) {
            throw new VfsIOException(e.getMessage(), e);
        }

        // delete file entry.
        VfsFileDB.delete(fileID, user.getID());
    }

    /**
     * Create database entry of VfsFile as Directory, then return the
     * instance of VfsFile.
     *
     * @param sessionID     user session ID thati s logged in.
     * @param parentFileID  parent file ID.
     * @param name          the name of new directory.
     *
     * @throws  VfsIOException
     *          Throws if could not create by an error or system limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public VfsFile createDirectory(String sessionID, String parentFileID,
            String name)
        throws VfsIOException, SessionException
    {
        // check parameters.
        if (sessionID == null || sessionID.trim().length() ==0)
            throw new IllegalArgumentException("sessionID is empty.");
        if (parentFileID == null || parentFileID.trim().length() == 0)
            throw new IllegalArgumentException("parentFileID is empty.");

        // check unavailable character.

        // check length of filename.
        SystemInfo systemInfo = getSystemInfo();
        if (name.length() > systemInfo.getFileNameLength())
            throw new VfsIOException("The name is too long.");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();

        // check already exists.
        if (VfsFileDB.get(parentFileID, name, user.getID()) != null)
            throw new VfsIOException("The name is already used.");

        // retrive parent file.
        VfsFile parent = VfsFileDB.get(parentFileID, user.getID());
        if (parent == null)
            throw new VfsIOException("The parent not found.");

        // check child count.
        int childCount = VfsFileDB.countChild(parent.getID(), user.getID());
        if (childCount >= systemInfo.getChildObjectsPerParent())
            throw new VfsIOException("Could not create object any more.");

        return VfsFileDB.createDirectory(name, parent.getID(), user.getID());
    }

    /**
     * Create file, and return the instance of VfsFile.
     * This method only create File entry to database, the contents may
     * not exists.  In order to write the contents, you should call
     * {@link #createFileSession(String, String, FileSession.Mode)}
     * method for get file session first.
     * And then, you should call
     * {@link #writeData(String, String, byte[], int)}
     * method for write content.
     *
     * <p> If number of child object that exists under the parent object
     * already max, throws VfsIOException.
     *
     * @param sessionID     user session ID that is logged in.
     * @param parentFileID  parent file ID.
     * @param name          the name of new file.
     *
     * @throws  VfsIOException
     *          Throws if could not create the file by an error or
     *          system limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public VfsFile createFile(String sessionID, String parentFileID,
            String name)
        throws VfsIOException, SessionException
    {
        if (sessionID == null || sessionID.trim().length() == 0)
            throw new IllegalArgumentException("sessionID is empty.");
        if (parentFileID == null || parentFileID.trim().length() == 0)
            throw new IllegalArgumentException("parentFileID is empty.");
        if (name == null || name.trim().length() == 0)
            throw new IllegalArgumentException("name is empty.");

        // check unavailable character
        
        // check length of filename.
        SystemInfo systemInfo = getSystemInfo();
        if (name.length() > systemInfo.getFileNameLength())
            throw new VfsIOException("The name is too long.");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();

        // retrive parent object.
        VfsFile parent = VfsFileDB.get(parentFileID, user.getID());
        if (parent == null)
            throw new VfsIOException("The parent not found.");

        // check already exists same name.
        if (VfsFileDB.get(parent.getID(), name, user.getID()) != null)
            throw new VfsIOException("The name is already used.");

        // check number of child object.
        int childCount = VfsFileDB.countChild(parent.getID(), user.getID());
        if (childCount >= systemInfo.getChildObjectsPerParent())
            throw new VfsIOException("Could not create object any more.");

        // create object as a file.
        return VfsFileDB.createFile(name, parent.getID(), 0, user.getID());
    }

    /**
     * Create file session specified <code>fileID</code> for specified
     * <code>mode</code>.
     * If specified file already create file session by write or append mode,
     * You can not create file session whatever you sepcified any mode.
     * If specified file already create file session by read mode, you can
     * create file session by only read mode.
     *
     * @param sessionID user session ID that is logged in.
     * @param fileID    target file ID.
     * @param mode      session mode.
     *
     * @return  created file session ID.
     *
     * @throws  NullPointerException
     *          Throws if these parameters was null.
     *
     * @throws  VfsIOException
     *          Throws if could not create by an error or system limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public String createFileSession(String sessionID, String fileID,
            FileSession.Mode mode)
        throws VfsIOException, SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (fileID == null)
            throw new NullPointerException("fileID");
        if (mode == null)
            throw new NullPointerException("mode is null.");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();

        // check exists of the file.
        VfsFile targetFile = VfsFileDB.get(fileID, user.getID());
        if (targetFile == null)
            throw new VfsIOException("Invalid fileID.");

        return userSession.newFileSession(targetFile, mode);
    }

    /**
     * Read data from the file that is specified by fileSessionID.
     *
     * @param sessionID     user session ID.
     * @param fileSessionID file session ID for read.
     * @param data          buffer for read data.
     * @param count         number of read bytes.
     *
     * @return  number of actual read bytes.
     *
     * @throws  NullPointerException
     *          Throws if these parameters was null.
     *
     * @throws  ArrayIndexOutOfBoundsException
     *          Throws if the count parameter larger than data.length, or
     *          the count parameter smaller than zero.
     *
     * @throws  VfsIOException
     *          Throws if could not read any errors or system limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public int readData(String sessionID, String fileSessionID, byte[] data,
            int count)
        throws VfsIOException, SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (fileSessionID == null)
            throw new NullPointerException("fileSessionID");
        if (data == null)
            throw new NullPointerException("data");
        if (count > data.length || count < 0)
            throw new ArrayIndexOutOfBoundsException("count");

        // check number of read bytes whether within the system limitation.
        SystemInfo systemInfo = getSystemInfo();
        if (systemInfo.getBytesPerRead() < count)
            throw new VfsIOException("count is beyond the system limitation.");

        // retrieve session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");

        // retrieve read file session.
        FileSession fileSession = userSession.getFileSession(fileSessionID);
        if (fileSession == null)
            throw new VfsIOException("The read file session not found.");
        if (!(fileSession instanceof ReadFileSession))
            throw new VfsIOException("The file session is not for read.");

        // read data.
        return ((ReadFileSession) fileSession).read(data, count);
    }

    /**
     * Write data to the file that is specified by fileSessionID.
     *
     * @param sessionID     user session ID.
     * @param fileSessionID file session ID for write or append.
     * @param data          byte array of data.
     * @param size          number of bytes.
     *
     * @return  number of actual wrote bytes.
     *
     * @throws  NullPointerException
     *          Throws if these parameters was null.
     *
     * @throws  ArrayIndexOutOfBoundsException
     *          Throws if the size parameter larger than data.length, or
     *          the size parameter smaller than zero.
     *
     * @throws  VfsIOException
     *          Throws if could not write by an error or system limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public int writeData(String sessionID, String fileSessionID, byte[] data,
            int size)
        throws VfsIOException, SessionException
    {
        // check parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (fileSessionID == null)
            throw new NullPointerException("fileSessionID");
        if (data == null)
            throw new NullPointerException("data");
        if (size > data.length)
            throw new ArrayIndexOutOfBoundsException("size > data.length");
        if (size < 0)
            throw new ArrayIndexOutOfBoundsException("size < 0");

        // check number of bytes whether within the system limitation.
        SystemInfo systemInfo = getSystemInfo();
        if (systemInfo.getBytesPerWrite() < size)
            throw new VfsIOException("size is beyond the system limitation.");

        // retrieve session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");

        // retrieve write file session.
        FileSession fileSession = userSession.getFileSession(fileSessionID);
        if (fileSession == null)
            throw new VfsIOException("The write file session not found.");
        if (!(fileSession instanceof WriteFileSession))
            throw new VfsIOException("The file session is not for write.");

        ((WriteFileSession) fileSession).write(data, size);

        return size;
    }

    /**
     * Close specified file session related to specified user session.
     *
     * @param sessionID     user session ID that is logged in.
     * @param fileSessionID target file session ID.
     *
     * @throws  NullPointerException
     *          Throws if these parameters was null.
     *
     * @throws  VfsIOException
     *          Throws if any error occurred.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public void closeFileSession(String sessionID, String fileSessionID)
        throws VfsIOException, SessionException
    {
        // check parameters
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (fileSessionID == null)
            throw new NullPointerException("fileSessionID");

        // retrieve user session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");

        // close file session.
        userSession.closeFileSession(fileSessionID);
    }

    /**
     * Check specified file name already exists.
     *
     * @param sessionID     user session ID that is logged in.
     * @param parentFileID  parent file ID.
     * @param fileName      the file name you want to check.
     *
     * @return  If the file name already exists in the parent, return true.
     *          Other wise return false.
     *
     * @throws  VfsIOException
     *          Throws if could not check by an error, or over the system
     *          limitation.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public boolean isExistsFileName(String sessionID, String parentFileID,
            String fileName)
        throws VfsIOException, SessionException
    {
        // check these parameters.
        if (sessionID == null)
            throw new NullPointerException("sessionID");
        if (parentFileID == null)
            throw new NullPointerException("parentFileID");
        if (fileName == null)
            throw new NullPointerException("fileName");

        // check unavailable character.

        // check length of filename whether the length over the maximum.
        SystemInfo systemInfo = getSystemInfo();
        if (fileName.length() > systemInfo.getFileNameLength())
            throw new VfsIOException("The file name is too long.");

        // retrive owner from session.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();

        // retrive parent object.
        VfsFile parent = VfsFileDB.get(parentFileID, user.getID());
        if (parent == null)
            throw new VfsIOException("The parent not found.");

        // check already exists same name.
        return (VfsFileDB.get(parent.getID(), fileName, user.getID()) != null);
    }

    /**
     * Return system information.
     *
     * @return  Instance of SystemInfo.
     */
    public SystemInfo getSystemInfo()
        throws VfsIOException
    {
        Config config = Config.getInstance();

        long bytesPerRead = 0;
        try {
            bytesPerRead = Long.parseLong(
                    config.getProperty("org.sd_network.vfs.BytesPerRead"));
        } catch (NumberFormatException e) {
            throw new VfsIOException(
                "Invalid property [org.sd_network.vfs.BytesPerRead].");
        }

        long bytesPerWrite = 0;
        try {
            bytesPerWrite = Long.parseLong(
                config.getProperty("org.sd_network.vfs.BytesPerWrite"));
        } catch (NumberFormatException e) {
            throw new VfsIOException(
                "Invalid property [org.sd_network.vfs.BytesPerWrite].");
        }

        int childObjectsPerParent = 0;
        try {
            childObjectsPerParent = Integer.parseInt(
                config.getProperty("org.sd_network.vfs.ChildObjectsPerParent"));
        } catch (NumberFormatException e) {
            throw new VfsIOException(
                "Invalid property [org.sd_network.vfs.ChildObjectsPerParent].");
        }

        int hierarchicalDepth = 0;
        try {
            hierarchicalDepth = Integer.parseInt(
                config.getProperty("org.sd_network.vfs.HierarchicalDepth"));
        } catch (NumberFormatException e) {
            throw new VfsIOException(
                "Invalid property [org.sd_network.vfs.HierarchicalDepth].");
        }

        int fileNameLength = 0;
        try {
            fileNameLength = Integer.parseInt(
                config.getProperty("org.sd_network.vfs.FileNameLength"));
        } catch (NumberFormatException e) {
            throw new VfsIOException(
                "Invalid property [org.sd_network.vfs.FileNameLength].");
        }

        long availableBytes = 0;
        long usedBytes = 0;
        try {
            SectorDriver sectorDriver = SectorDriverManager.getSectorDriver();
            availableBytes = sectorDriver.getAvailableBytes();
            usedBytes = sectorDriver.getUsedBytes();
        } catch (SectorException e) {
            throw new VfsIOException(e.getMessage(), e);
        }
        return new SystemInfo(bytesPerRead, bytesPerWrite,
            childObjectsPerParent, hierarchicalDepth, fileNameLength,
            availableBytes, usedBytes);
    }

    //////////////////////////////////////////////////////////// 
    // System management APIs.

    /**
     * Add new user.
     *
     * <p> If <tt>isAdmin</tt> set true, the new user create 
     * as an Administrator of VFS. Administrator is for only management
     * of VFS, has not even one file and or one directory.
     * 
     * <p> When create user as an Administrator, You must have permission
     * as an Administrator.
     *
     * @param sessionID user session ID that is logged in.
     * @param loginName Login name of new User.
     * @param password Password of new User.
     * @param isAdmin Flag whether new User is Administrator.
     *
     * @throws  PermissionException
     *          Throws if logged in user is not administrator.
     *
     * @throws  SessionException
     *          Throws if the session is invalidated or timeout.
     */
    public void addUser(String sessionID, String loginName,
            String password, boolean isAdmin)
        throws PermissionException, SessionException
    {
        // check whether logged in user has permission.
        UserSessionManager usm = UserSessionManager.getInstance();
        UserSession userSession = usm.getUserSession(sessionID);
        if (userSession == null)
            throw new SessionException("Invalid session.");
        User user = userSession.getUser();
        if (!user.isAdmin())
            throw new PermissionException(
                    "You have not permission as an Administrator.");

        ConnectionPool pool = ConnectionPool.getInstance("vfs");
        Connection con = pool.engageConnection(10);
        try {
            con.setAutoCommit(false);
            User newUser = UserDB.create(con, loginName, password, isAdmin);

            // If new user is not administrator, create HOME directory of user.
            if (!newUser.isAdmin())
                VfsFileDB.createDirectory(con, "Home", "-1", newUser.getID());
            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
                _log.log(Level.SEVERE, "Connection could not rollback.",e1);
            }
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e1) {
                _log.log(Level.WARNING, "Connection could not close.",e1);
            }
        }
    }
}
