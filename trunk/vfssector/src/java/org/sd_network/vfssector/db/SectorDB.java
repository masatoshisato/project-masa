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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.db.ConnectionPool;
import org.sd_network.db.DBException;
import org.sd_network.db.DBUtil;

/**
 * This class includes service methods of Sector table entities.
 *
 * <p> $Id$
 *
 * @author MAsatoshi Sato
 */
public class SectorDB
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            SectorDB.class.getName());

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * �w��t�@�C���ɕR�Â��Z�N�^�[��o�^���܂��B
     * �ʏ�P�̃t�@�C���ɂ��A�����̃Z�N�^�[���֘A�t���鎖���ł��܂��B
     * �e�Z�N�^�[�ɂ́A���̃t�@�C�����ɂ�����f�[�^�̏��Ԃ������V�[�P���X�ԍ�
     * ���K�v�ł��B
     *
     * @param fileID    �t�@�C�����ʎq�B
     * @param seqNum    �t�@�C�����ɂ�����Z�N�^�[�̃V�[�P���X�ԍ��B
     * @param content   �Z�N�^�[�Ɋi�[����f�[�^��byte�z��B
     * @param size      <tt>content</tt> ������o���o�C�g���B
     *
     * @return  �o�^���ɐ��������Z�N�^�[�̈�ӂȎ��ʎq�B
     *
     * @throws  NullPointerException
     *          ������Null���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          �ȉ��̏ꍇ�ɃX���[���܂��B
     *          <ul>
     *              <li> <tt>seqNum</tt> ��0�����̒l���w�肳�ꂽ
     *              <li> <tt>content</tt> ����̔z�񂾂���
     *              <li> <tt>size</tt> �� <tt>content</tt> �̔z��T�C�Y�ȏ��
     *                   �����w�肳�ꂽ
     *              <li> <code>size</code> ��0�ȉ��̒l���w�肳�ꂽ
     *          </ul>
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final String create(String fileID, int seqNum, int size,
            byte[] content)
    {
        if (fileID == null)
            throw new NullPointerException("fileID.");
        if (seqNum < 0)
            throw new IllegalArgumentException("seqNum too small.");
        if (content == null)
            throw new NullPointerException("content.");
        if (content.length == 0)
            throw new IllegalArgumentException("content was empty.");
        if (content.length < size || size <= 0)
            throw new IllegalArgumentException("invalid size.");

        String sectorID = UUID.randomUUID().toString();
        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "INSERT INTO sector " +
                    " (sector_id, file_id, seq_num, size, content) " +
                    "VALUES (?,?,?,?,?)");
            stmt.setString(1, sectorID);
            stmt.setString(2, fileID);
            stmt.setInt(3, seqNum);
            stmt.setInt(4, size);
            stmt.setBinaryStream(
                    5, new ByteArrayInputStream(content), size);
            stmt.executeUpdate();
            _log.info("Sector created : " + sectorID);
            return sectorID;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, 
                        "Could not close statemet or connection", e);
            }
        }
    }

    /**
     * �w��Z�N�^�[�ɕۑ�����Ă���f�[�^��byte�z��ŕԂ��܂��B
     *
     * @param sectorID  �Z�N�^�[���ʎq�B
     *
     * @return  �f�[�^��byte�z��B
     *
     * @throws  NullPointerExeption
     *          <tt>sectorID</tt> ��null���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalStateException
     *          <tt>sectorID</tt> �ɕR�Â��Z�N�^�[��񂪑��݂��Ȃ��ꍇ��
     *          �X���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final byte[] getContent(String sectorID) {
        if (sectorID == null)
            throw new NullPointerException("sectorID");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT content FROM sector WHERE sector_id=?");
            stmt.setString(1, sectorID);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new IllegalStateException(
                        "Sector not found. sectorID = " + sectorID);
            return rs.getBytes("content");
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

    /**
     * �w��Z�N�^�[�ɕۑ�����Ă���f�[�^���A�w��� <tt>content</tt>
     * �ŏ㏑�����܂��B<tt>size</tt> �� <tt>content</tt> �̔z��T�C�Y��菬����
     * ���A1�ȏ�łȂ���΂Ȃ�܂���B<BR>
     * �����w�肳�ꂽ <tt>sectorID</tt> �ɕR�Â��Z�N�^�[��񂪑��݂��Ȃ��ꍇ��
     * �����s���܂���B
     *
     * @param sectorID  �㏑������Z�N�^�[��ID�B
     * @param size      <tt>content</tt> �̏������݃o�C�g���B
     * @param content   �㏑������f�[�^�̃o�C�g�z��B
     *
     * @return  �㏑�������Z�N�^�[��ID�B
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          �ȉ��̏ꍇ�ɃX���[���܂��B
     *          <ul>
     *              <li> <tt>content</tt> ����̔z�񂾂���
     *              <li> <tt>size</tt> �� <tt>content</tt> �̔z��T�C�Y�ȏ��
     *                   �����w�肳�ꂽ
     *              <li> <code>size</code> ��0�ȉ��̒l���w�肳�ꂽ
     *              <li> <tt>sectorID</tt> �ɕR�Â��Z�N�^�[��񂪑��݂��Ȃ�
     *          </ul>
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final String update(String sectorID, int size,
            byte[] content)
    {
        if (sectorID == null)
            throw new NullPointerException("sectorID.");
        if (!exists(sectorID))
            throw new IllegalArgumentException("The sector was not found.");
        if (content == null)
            throw new NullPointerException("content.");
        if (content.length == 0)
            throw new IllegalArgumentException("content was empty.");
        if (content.length < size || size <= 0)
            throw new IllegalArgumentException("invalid size.");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "UPDATE sector SET size=?, content=? " +
                    "WHERE sector_id=?");
            stmt.setInt(1, size);
            stmt.setBinaryStream(
                    2, new ByteArrayInputStream(content), size);
            stmt.setString(3, sectorID);
            stmt.executeUpdate();
            return sectorID;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, 
                        "Could not close statemet or connection", e);
            }
        }
    }

    /**
     * �w��t�@�C���ɕR�Â��Z�N�^�[�̂����A�ŏI�Z�N�^�[�̃C���X�^���X��
     * �Ԃ��܂��B
     * �����w��t�@�C���ɕR�Â��Z�N�^�[�����݂��Ȃ��ꍇ�� <tt>null<tt> ��
     * �Ԃ��܂��B
     *
     * @param fileID    �t�@�C�����ʎq�B
     *
     * @return  <tt>fileID</tt> �ɕR�Â��Ō�̃Z�N�^�[���� {@link Sector}
     *          �̃C���X�^���X�B�����A�Z�N�^�[��񂪑��݂��Ȃ��ꍇ�� 
     *          <tt>null</tt> ��Ԃ��܂��B
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final Sector getLastSector(String fileID) {
        if (fileID == null)
            throw new NullPointerException("fileID.");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT sector_id, file_id, seq_num, size " +
                    "FROM " +
                    " sector, " +
                    " (SELECT max(seq_num) as max_seq_num FROM sector) t1 " +
                    "WHERE " +
                    " sector.seq_num = t1.max_seq_num AND " +
                    " sector.file_id = ?");
            stmt.setString(1, fileID);
            ResultSet rs = stmt.executeQuery();
            Sector lastSector = null;
            if (rs.next())
                lastSector = getInstance(rs);
            return lastSector;
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

    /**
     * �w��t�@�C���ɕR�Â��S�Z�N�^�[���ێ����Ă���f�[�^�̑��o�C�g����
     * �Ԃ��܂��B
     *
     * @param fileID    �t�@�C�����ʎq
     *
     * @return  <tt>fileID</tt> �ɕR�Â��e�Z�N�^�[���ێ�����f�[�^�̑��o�C�g��
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final long getFileSize(String fileID) {
        if (fileID == null)
            throw new NullPointerException("fileID.");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT sum(size) as total_bytes " +
                    "FROM sector " +
                    "WHERE file_id=?");
            stmt.setString(1, fileID);
            ResultSet rs = stmt.executeQuery();
            long size = 0;
            if (rs.next())
                size = rs.getLong("total_bytes");
            return size;
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

    /**
     * �w��t�@�C���ɕR�Â��Z�N�^�[����Ԃ��܂��B
     *
     * @param fileID    �t�@�C�����ʎq
     *
     * @return  <tt>fileID</tt> �ɕR�Â��Z�N�^�[���B
     *          �Z�N�^�[�����݂��Ȃ��ꍇ�� <tt>0</tt> ��Ԃ��܂��B
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final long getTotalSectorNumber(String fileID) {
        if (fileID == null)
            throw new NullPointerException("fileID");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT count(*) as total_sector_number " +
                    "FROM sector " +
                    "WHERE file_id=?");
            stmt.setString(1, fileID);
            ResultSet rs = stmt.executeQuery();
            long totalSectorNumber = 0;
            if (rs.next())
                totalSectorNumber = rs.getLong("total_sector_number");
            return totalSectorNumber;
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

    /**
     * �w��t�@�C���ɕR�Â��Z�N�^�[�̃Z�N�^�[���ʎq��z��ŕԂ��܂��B
     *
     * @param fileID    �t�@�C�����ʎq
     * 
     * @return  <tt>fileID</tt> �ɕR�Â��e�Z�N�^�[�̃Z�N�^�[���ʎq�̔z��B
     *          �����Z�N�^�[���Ȃ��ꍇ�͋�̔z���Ԃ��܂��B
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final String[] getSectorIDs(String fileID) {
        if (fileID == null)
            throw new NullPointerException("fileID");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT sector_id FROM sector " +
                    "WHERE file_id=? " + 
                    "ORDER BY seq_num");
            stmt.setString(1, fileID);
            ResultSet rs = stmt.executeQuery();
            List<String> buf = new ArrayList<String>();
            while (rs.next())
                buf.add(rs.getString("sector_id"));
            return buf.toArray(new String[0]);
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

    /**
     * �w��t�@�C���ɕR�Â��Z�N�^�[��S�č폜���܂��B
     * �Z�N�^�[�������ꍇ�͉������܂���B
     *
     * @param fileID    �t�@�C�����ʎq
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final void deleteSectors(String fileID) {
        if (fileID == null)
            throw new NullPointerException("fileID");

        DBUtil.update(
                "vfssector",
                "DELETE FROM sector WHERE file_id=?",
                new Object[] {fileID});
        _log.info("Relevant sectors with the file deleted. " + fileID);
    }

    /**
     * �w��Z�N�^�[���폜���܂��B
     * �Z�N�^�[���Ȃ��ꍇ�͉������܂���B
     *
     * @param sectorID  �Z�N�^�[���ʎq
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final void deleteSector(String sectorID) {
        if (sectorID == null)
            throw new NullPointerException("sectorID");

        DBUtil.update(
                "vfssector",
                "DELETE FROM sector WHERE sector_id=?",
                new Object[] {sectorID});
        _log.info("The sector deleted. " + sectorID);
    }

    /**
     * �ۑ�����Ă���S�Z�N�^�[���ێ����Ă���f�[�^�̑��o�C�g����Ԃ��܂��B
     *
     * @return  �S�Z�N�^�[���ێ����Ă���f�[�^�̑��o�C�g���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    public static final long getUsedBytes() {
        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT sum(size) as used_bytes FROM sector");
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getLong("used_bytes");
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

    //////////////////////////////////////////////////////////// 
    // Private methods.

    private static final Sector getInstance(ResultSet rs)
        throws SQLException
    {
        return new Sector(
                rs.getString("sector_id"),
                rs.getString("file_id"),
                rs.getInt("seq_num"),
                rs.getInt("size"));
    }

    /**
     * �w��� <tt>sectorID</tt> �ɕR�Â��Z�N�^�[��񂪑��݂���ꍇ��
     * <tt>true</tt> ��Ԃ��܂��B
     *
     * @param sectorID      �Z�N�^�[���ʎq�B
     *
     * @return  �w��̃Z�N�^�[��񂪂���� <tt>true</tt> ��Ԃ��܂��B
     *          �����ꍇ�� <tt>false</tt> ��Ԃ��܂��B
     *
     * @throws  NullPointerException
     *          ������ <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�A�N�Z�X���ɃG���[�����������ꍇ�ɃX���[���܂��B
     */
    private static final boolean exists(String sectorID) {
        if (sectorID == null)
            throw new NullPointerException("sectorID.");

        ConnectionPool pool = ConnectionPool.getInstance("vfssector");
        Connection con = pool.engageConnection(10);
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT sector_id FROM sector WHERE sector_id=?");
            stmt.setString(1, sectorID);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
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
