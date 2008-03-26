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
package org.sd_network.vfs.sector;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A interface that a sector driver must implement.
 *
 * All of the sector driver is must implement this interface.
 *
 * A sector driver, it is to access to a storage such as a local hard disk, 
 * a network storage, or a database for file data whether a text or a binary.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public interface SectorDriver
{
    /**
     * Return instance of implement class of OutputStream.
     * This OutputStream is used for output data to a storage managed by
     * this driver.
     * If the sector related to specified fileID is not found, it is created.
     *
     * <p> The sector is to be write lock mode.
     * While the sectors is write lock mode, {@link getOutputStream(String)},
     * {@link getOutputStream(String, boolean)}, {@link getInputStream(String)}
     * methods is failed. When call {@link OutputStream#close()} method,
     * write lock mode is released.
     *
     * @param fileID    ID of a file that is related some sectors.
     * 
     * @return  The instance of implement class of OutputStream.
     *
     * @throws  SectorException
     *          Throws if status of sector specified by fileID was 
     *          following.
     *          <ul>
     *              <li> sector is already write locked by other thread.
     *              <li> any other error occurred.
     *          </ul>
     */
    public OutputStream getOutputStream(String fileID)
        throws SectorException;

    /**
     * Return instance of implement class of OutputStream.
     * If the sector related to specified fileID not found, it is create.
     *
     * <p> The sector is to be write lock mode.
     * When call {@link OutputStream#close()} method, write lock mode
     * is released.
     *
     * @param fileID    file ID that is related some sector.
     * @param append    If you want to write data as append mode,
     *                  you specify "true".
     *
     * @return  the instance of implement class of InputStream.
     *
     * @throws  SectorException
     *          Throws if status of sector specified by fileID was 
     *          following.
     *          <ul>
     *              <li> sector is already write locked by other thread.
     *              <li> any other error occurred.
     *          </ul>
     */
    public OutputStream getOutputStream(String fileID, boolean append)
        throws SectorException;

    /**
     * Return instance of implement class of InputStream.
     *
     * <p> The all sectors related to fileID is to be read lock mode.
     * When call {@link InputStream#close()} method, read lock mode
     * is released.
     *
     * @param fileID    file ID that is related some sector.
     *
     * @return  The instance of implement class of InputStream.
     *
     * @throws  SectorException
     *          Throws if sector information specified by fileID was 
     *          not found or any other error occurred.
     */
    public InputStream getInputStream(String fileID)
        throws SectorException;

    /**
     * If sector that is specified file ID is already write locked,
     * return true, otherwise, return false.
     *
     * <p> the write lock is representing updating the sector. While that,
     * {@link #getInputStream(String)}, {@link #getOutputStream(String)}, and
     * {@link #getOutputStream(String, boolean)} method call fail.
     *
     * @param fileID    file ID for specify sector.
     *
     * @return  true if sector specified by file ID was already write locked.
     *          otherwise, return false.
     */
    public boolean isWriteLocked(String fileID);

    /**
     * If sector that is specified file ID is already read locked,
     * return true, otherwise, return false.
     *
     * <p> The read lock is representing reading the sector. While that,
     * {@link #getOutputStream(String)} and 
     * {@link #getOutputStream(String, boolean)} method call fail.
     *
     * @param fileID    file ID for specify sector.
     *
     * @return  true if sector specified by file ID was already read locked.
     *          Otherwise, return false.
     */
    public boolean isReadLocked(String fileID);

    /**
     * Delete the all sectors related with the specified file ID.
     * This method must be called when a error occurred with writing data.
     *
     * @param fileID    file ID related with delete sectors.
     *
     * @throws  SectorException
     *          Throws if sector information specified by fileID was 
     *          not found or any other error occurred.
     */
    public void deleteSectors(String fileID)
        throws SectorException;

    /**
     * Initialize this SectorDriver.
     *
     * <p> If you need initialisation process after instantiated your
     * sector driver class, you have to implement this method.
     *
     * @throws  SectorException
     *          Throws if initialization of this driver failed.
     */
    public void initDriver()
        throws SectorException;

    /**
     * Return file size that is specified by fileID.
     * If the sectors that related to the file is not exists, return zero.
     *
     * @param fileID    the file ID.
     *
     * @return  file size (byte).
     *
     * @throws  SectorException
     *          Throws if sector error occurred.
     */
    public long getFileSize(String fileID)
        throws SectorException;

    /**
     * Return available bytes of storage that managed by this driver.
     *
     * @return  available bytes (byte).
     *
     * @throws  SectorException
     *          Throws if it could not access to capacity information.
     */
    public long getAvailableBytes()
        throws SectorException;

    /**
     * Return used bytes of storage that managed by this driver.
     *
     * @return used bytes (byte).
     *
     * @throws  SectorException
     *          Throws if it could not access to capacity information.
     */
    public long getUsedBytes()
        throws SectorException;
}
