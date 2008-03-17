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

import java.util.Date;

import org.sd_network.db.DBUtil;

/**
 * A class represent a VfsFile.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class VfsFile
{
    //////////////////////////////////////////////////////////// 
    // Public fields.

    /** File type. */
    public enum FileType {
        DIRECTORY(1),
        FILE(0);

        private final int _value;

        FileType(int value) {
            _value = value;
        }

        public int getValue() {
            return _value;
        }
    };

    //////////////////////////////////////////////////////////// 
    // Private fields.

    private final String _fileID;
    private String _name;
    private final FileType _type;
    private final String _parentID;
    private long _size;
    private final String _ownerID;
    private final Date _created;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * Constructor for same package.
     * This method is generally used by VfsFileDB.
     *
     */
    VfsFile(String fileID, String name, int typeID, String parentID,
            long size, String ownerID, Date created)
    {
        _fileID = fileID;
        _name = name;
        if (typeID == FileType.DIRECTORY.getValue())
            _type = FileType.DIRECTORY;
        else
            _type = FileType.FILE;
        _parentID = parentID;
        _size = size;
        _ownerID = ownerID;
        _created = created;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return the fileID.
     */
    public String getID() {
        return _fileID;
    }

    /**
     * Return name.
     */
    public String getName() {
        return _name;
    }

    /** 
     * Change the file name.
     */
    public String renameTo(String name) {
        DBUtil.update(
                "vfs",
                "UPDATE vfs_file SET name=? WHERE file_id=?",
                new Object[] {name, _fileID});
        _name = name;
        return name;
    }

    /**
     * Set file size.
     */
    public long resizeTo(long size) {
        if (!isFile())
            throw new IllegalStateException("This object is not a file.");

        DBUtil.update(
                "vfs",
                "UPDATE vfs_file SET size=? WHERE file_id=?",
                new Object[] {new Long(size), _fileID});
        _size = size;
        return size;
    }

    public FileType getType() {
        return _type;
    }

    public boolean isDirectory() {
        return _type == FileType.DIRECTORY;
    }

    public boolean isFile() {
        return _type == FileType.FILE;
    }

    public String getParentID() {
        return _parentID;
    }

    public long getSize() {
        return _size;
    }

    public String getOwnerID() {
        return _ownerID;
    }

    public Date getCreated() {
        return _created;
    }
}
