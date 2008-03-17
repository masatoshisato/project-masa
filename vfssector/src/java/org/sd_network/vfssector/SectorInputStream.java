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

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfssector.db.SectorDB;

/**
 * vfssector�p�b�P�[�W�� {@link java.io.InputStream} �C���^�t�F�[�X�̎���
 * �ł��Bvfssector�p�b�P�[�W�ɂ���ĊǗ�����Ă���Z�N�^�[�f�[�^�x�[�X�����
 * �f�[�^�ǂݍ��݂Ɋւ��鏈�����s���܂��B
 *
 * <p> ���̃C���X�^���X�́A
 * {@link org.sd_network.vfs.sector.SectorDriver#getInputStream(String)} 
 * ���\�b�h�̖߂�l�ł��B
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SectorInputStream
    extends InputStream
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            SectorInputStream.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** �f�[�^�ǂݍ��ݑΏۃt�@�C���̎��ʎq */
    private final String _fileID;

    /** �P�Z�N�^�[���̃f�[�^�o�b�t�@ */
    private byte[] _buffer;

    /** �f�[�^�o�b�t�@�̃|�C���^ */
    private int _bufferIdx;
    
    /** �f�[�^�ǂݍ��ݑΏۃt�@�C���ƕR�Â��e�Z�N�^�[�̎��ʎq�̔z�� */
    private String[] _sectorIDs;

    /** �Z�N�^�[���ʎq�z��̃|�C���^ */
    private int _sectorIdx;

    /** Stream�I���t���O */
    private boolean _closed;

    /** SectorDriver�̃C���X�^���X */
    private final VfsSectorDriver _driver;

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisations.

    /**
     * SectorInputStream�̃C���X�^���X�𐶐����܂��B
     * �ȉ��̒l�������l�Ƃ��Đݒ肳��܂��B
     * <ul>
     *  <li> �f�[�^�o�b�t�@�̏�����
     *  <li> �f�[�^�o�b�t�@��index��0�ɐݒ�
     *  <li> �w��t�@�C���̃Z�N�^�[���ʎq�z��̎擾
     *  <li> �Z�N�^�[���ʎq�z��̃|�C���^��0�ɐݒ�
     *  <li> �I���t���O�̃��Z�b�g
     * </ul>
     * �P�Z�N�^�[���̃f�[�^�o�b�t�@�T�C�Y�́A�Z�N�^�[�ɕۑ�����Ă���
     * �f�[�^�o�C�g���Ɉˑ����܂��B
     *
     * @param fileID        �t�@�C�����ʎq
     * @param driver        SectorDriver�̃C���X�^���X
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     */
    SectorInputStream(String fileID, VfsSectorDriver driver) {
        if (fileID == null)
            throw new NullPointerException("fileID");
        if (driver == null)
            throw new NullPointerException("driver");

        _fileID = fileID;
        _buffer = null;
        _bufferIdx = 0;
        _sectorIDs = SectorDB.getSectorIDs(fileID);
        _log.log(Level.INFO, "number of sector = " + _sectorIDs.length);
        StringBuffer sb = new StringBuffer();
        for (int idx = 0; idx < _sectorIDs.length; idx++)
            sb.append(_sectorIDs[idx]).append("\n");
        _log.info(sb.toString());
        _sectorIdx = 0;
        _closed = false;
        _driver = driver;
    }

    //////////////////////////////////////////////////////////// 
    // Implements to InputStream.
    
    public synchronized int read()
        throws IOException
    {
        // check already closed.
        if (_closed)
            throw new IOException("This stream was already closed.");

        // check whether sector exists for specified fileID.
        if (_sectorIDs == null || _sectorIDs.length == 0) {
            _log.log(Level.INFO,
                    "sector not found for fileID=" + _fileID + ".");
            return -1;
        }

        while (_sectorIdx < _sectorIDs.length) {

            // check buffer empty.
            if (_buffer == null || _bufferIdx == _buffer.length) {
                String sectorID = _sectorIDs[_sectorIdx++];
                _buffer = SectorDB.getContent(sectorID);
                _log.log(Level.INFO,
                        "Sector[" + sectorID + "], " +
                        "length=" + _buffer.length + ".");
                _bufferIdx = 0;

                // check whether sector is empty.
                if (_buffer == null || _buffer.length == 0) {
                    SectorDB.deleteSector(sectorID);
                    _log.log(Level.INFO,
                            "Empty sector found [" + sectorID + "].");
                    continue;
                }
            }
            return (int) (_buffer[_bufferIdx++] & 0xff);
        }
        _log.info("input stream terminated.");
        return -1;
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

        _closed = true;
        _driver.releaseReadLock(_fileID);
    }
}
