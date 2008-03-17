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
package org.sd_network.vfssector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.util.Config;
import org.sd_network.vfs.sector.SectorDriver;
import org.sd_network.vfssector.db.Sector;
import org.sd_network.vfssector.db.SectorDB;

/**
 * A {@link java.io.OutputStream} implementation for vfssector.
 * This class is to write binary data to database that is managed by vfssector.
 * The number of byte a sector must be specified by property 
 * [org.sd_network.vfssector.SectorSize]. Default of the value is 1048576 byte
 * (1MByte).  If write data more than the sector size, the data is written at
 * multiple sectors.
 * 
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SectorOutputStream
    extends OutputStream
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            SectorOutputStream.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Stream buffer. */
    private byte[] _streamBuffer;
    
    /** Sequence number of sectors. */
    private int _sequenceNumber;

    /** Target file ID. */
    private final String _fileID;

    /** Buffer pointer. */
    private int _pointer;

    /** Closed flag. */
    private boolean _closed;

    /** Instance of SectorDriver. */
    private final VfsSectorDriver _driver;

    /** Append mode flag. */
    private boolean _appendMode;

    /** Sector ID (for append mode) */
    private String _sectorID;

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisations.
    
    /**
     * Initialisation of OutputStream as following.
     * <ul>
     *  <li> Set number of sector sequence to 0.
     *  <li> Set number of byte a sector to be specified by property.
     *  <li> Initialize stream buffer.
     *  <li> Set buffer pointer to 0.
     * </ul>
     * Generally, this constructor is used for new file.
     *
     * @param fileID    file ID of the write data.
     * @param driver    Instance of VfsSectorDriver.
     */
    SectorOutputStream(String fileID, VfsSectorDriver driver) {
        if (fileID == null || fileID.trim().length() == 0)
            throw new IllegalArgumentException("fileID is empty.");

        _fileID = fileID;
        _sequenceNumber = 0;
        _streamBuffer = new byte[driver.getDefaultSectorSize()];
        _pointer = 0;
        _closed = false;
        _driver = driver;
        _appendMode = false;
    }

    /**
     * Create this instance that is initialized by Sector table.
     * Generally, this constructor is used for open exists file as append mode.
     *
     * @param sector    The instance of Sector that is last sector related to
     *                  file.
     * @param driver    Instance of VfsSectorDriver.
     */
    SectorOutputStream(Sector sector, VfsSectorDriver driver) {
        if (sector == null)
            throw new IllegalArgumentException("sector is null.");

        _fileID = sector.getFileID();
        _sequenceNumber = sector.getSeqNum();
        _streamBuffer = new byte[driver.getDefaultSectorSize()];
        System.arraycopy(
                sector.getContent(), 0,
                _streamBuffer, 0,
                sector.getContentSize());
        _pointer = sector.getContentSize();
        _closed = false;
        _driver = driver;
        _appendMode = true;
        _sectorID = sector.getSectorID();
    }

    //////////////////////////////////////////////////////////// 
    // Impements to OutputStream.

    public synchronized void write(int b)
        throws IOException
    {
        if (_closed)
            throw new IOException("This stream was already closed.");

        _streamBuffer[_pointer] = (byte) b;
        _pointer++;
        if (_pointer < _streamBuffer.length)
            return;
        writeToSector();
    }

    public synchronized void flush()
        throws IOException
    {
        if (_closed)
            throw new IOException("This stream was already closed.");

        return;
    }

    public synchronized void close()
        throws IOException
    {
        if (_closed)
            return;

        try {
            writeToSector();
            _closed = true;
        } finally {
            _driver.releaseWriteLock(_fileID);
        }
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Write bufferring current data to a sector.
     * After the process, this method do following.
     * <ul>
     *  <li> Increment _sequenceNumber.
     *  <li> Zero clear _pointer.
     *  <li> Renew _streamBuffer.
     * </ul>
     *
     * @throws  IOException
     *          Throws if a database error occurred.
     */
    private void writeToSector()
        throws IOException
    {
        try {
            // write to db.
            String sectorID = null;
            if (_appendMode) {
                sectorID = SectorDB.update(_sectorID, _pointer, _streamBuffer);
                _appendMode = false;
            } else {
                sectorID = SectorDB.create(
                        _fileID, _sequenceNumber, _pointer, _streamBuffer);
            }

            // update
            _sequenceNumber++;
            _pointer = 0;

            _log.log(Level.FINE, 
                    "Data wrote to sector. " +
                    "file_id=" + _fileID + ", " +
                    "sector_id=" + sectorID + ", " +
                    "sequence_number=" + _sequenceNumber + ", " +
                    "number of bytes=" + _pointer + ".");
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Sector could not write.", e);
            throw new IOException(e.getMessage());
        }
    }
}
