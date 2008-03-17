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
package org.sd_network.vfssector.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.db.ConnectionPool;
import org.sd_network.db.DBException;

/**
 * �f�[�^�x�[�X�e�[�u�� <tt>sector</tt> �̂P���R�[�h�̃G���e�B�e�B��\���܂��B
 * ���̃G���e�B�e�B�N���X�͓ǂݏo����p�ƂȂ��Ă���A�Z�N�^�[�̓o�^�^�X�V
 * �ɂ� {@link SectorDB} ���g�p���܂��B
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Sector
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Sector.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** �Z�N�^�[���ʎq */
    private final String _sectorID;

    /** �t�@�C�����ʎq */
    private final String _fileID;

    /** �f�[�^�̃o�C�g�� */
    private int _size;

    /** �Z�N�^�[�̏��� */
    private int _seqNum;

    /** �f�[�^ */
    private byte[] _content;

    //////////////////////////////////////////////////////////// 
    // Initialisations.

    Sector(String sectorID, String fileID, int seqNum, int size) {
        _sectorID = sectorID;
        _fileID = fileID;
        _seqNum = seqNum;
        _size = size;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * ���̃Z�N�^�[�̃Z�N�^�[���ʎq��Ԃ��܂��B
     *
     * @return  �Z�N�^�[���ʎq
     */
    public String getSectorID() {
        return _sectorID;
    }

    /**
     * ���̃Z�N�^�[�̃t�@�C�����ʎq��Ԃ��܂��B
     *
     * @return  �t�@�C�����ʎq
     */
    public String getFileID() {
        return _fileID;
    }

    /**
     * ���̃Z�N�^�[�̏��Ԃ�Ԃ��܂��B
     *
     * @return  ����
     */
    public int getSeqNum() {
        return _seqNum;
    }

    /**
     * ���̃Z�N�^�[���ێ�����f�[�^�̃o�C�g����Ԃ��܂��B
     *
     * @return  �f�[�^�̃o�C�g��
     */
    public int getContentSize() {
        return _size;
    }

    /**
     * ���̃Z�N�^�[���ێ�����f�[�^���o�C�g�z��Ƃ��ĕԂ��܂��B
     *
     * @return  �f�[�^�̃o�C�g�z��
     */
    public byte[] getContent() {
        if (_content != null)
            return _content;

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT content FROM sector WHERE sector_id=?");
            stmt.setString(1, _sectorID);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new IllegalStateException(
                        "Sector not found when retrive content. sectorID = " +
                        _sectorID);
            _content = rs.getBytes("content");
            return _content;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING,
                        "Could not close statement or connection.", e);
            }
        }
    }
}
