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

import java.util.Date;

/**
 * This class provide various system information.
 * The various system information must defined to property file that is used
 * initialization of this system.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SystemInfo
{
    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Maximum number of byte per call of read method. */
    private final long _bytesPerRead;

    /** Maximum number of acceptable byte per call of write method. */
    private final long _bytesPerWrite;

    /** Maximum number of registerable child object per one object. */
    private final int _childObjectsPerParent;

    /** Maximum number of hierarchical depth. */
    private final int _hierarchicalDepth;

    /** Number of byte that available as file name per one file. */
    private final int _fileNameLength;

    /** Number of byte that available at whole of this system. */
    private final long _availableBytes;

    /** Number of byte that is used at whole of this system. */
    private final long _usedBytes;

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * Constructor for same package.
     * This method is generally used by SystemInfoDB.
     *
     */
    SystemInfo(long bytesPerRead, long bytesPerWrite,
            int childObjectsPerParent, int hierarchicalDepth,
            int fileNameLength, long availableBytes, long usedBytes)
    {
        _bytesPerRead = bytesPerRead;
        _bytesPerWrite = bytesPerWrite;
        _childObjectsPerParent = childObjectsPerParent;
        _hierarchicalDepth = hierarchicalDepth;
        _fileNameLength = fileNameLength;
        _availableBytes = availableBytes;
        _usedBytes = usedBytes;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Return maximum number of byte per call of read method.
     */
    public long getBytesPerRead() {
        return _bytesPerRead;
    }

    /** 
     * Return maximum number of acceptable byte per call of write method.
     */
    public long getBytesPerWrite() {
        return _bytesPerWrite;
    }

    /** 
     * Return maximum number of registerable child object per one object.
     */
    public int getChildObjectsPerParent() {
        return _childObjectsPerParent;
    }

    /** 
     * Return maximum number of hierarchical depth.
     */
    public int getHierarchicalDepth() {
        return _hierarchicalDepth;
    }

    /** 
     * Return number of byte that available as file name per one file.
     */
    public int getFileNameLength() {
        return _fileNameLength;
    }

    /** 
     * Return number of byte that available at whole of this system.
     */
    public long getAvailableBytes() {
        return _availableBytes;
    }

    /** 
     * Return number of byte that is used at whole of this system.
     */
    public long getUsedBytes() {
        return _usedBytes;
    }
}
