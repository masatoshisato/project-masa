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
     * 指定ファイルに紐づくセクターを登録します。
     * 通常１つのファイルにつき、複数のセクターを関連付ける事ができます。
     * 各セクターには、そのファイル内におけるデータの順番を示すシーケンス番号
     * が必要です。
     *
     * @param fileID    ファイル識別子。
     * @param seqNum    ファイル内におけるセクターのシーケンス番号。
     * @param content   セクターに格納するデータのbyte配列。
     * @param size      <tt>content</tt> から取り出すバイト数。
     *
     * @return  登録時に生成したセクターの一意な識別子。
     *
     * @throws  NullPointerException
     *          引数にNullが指定された場合にスローします。
     *
     * @throws  IllegalArgumentException
     *          以下の場合にスローします。
     *          <ul>
     *              <li> <tt>seqNum</tt> に0未満の値が指定された
     *              <li> <tt>content</tt> が空の配列だった
     *              <li> <tt>size</tt> に <tt>content</tt> の配列サイズ以上の
     *                   数が指定された
     *              <li> <code>size</code> に0以下の値が指定された
     *          </ul>
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定セクターに保存されているデータをbyte配列で返します。
     *
     * @param sectorID  セクター識別子。
     *
     * @return  データのbyte配列。
     *
     * @throws  NullPointerExeption
     *          <tt>sectorID</tt> にnullが指定された場合にスローします。
     *
     * @throws  IllegalStateException
     *          <tt>sectorID</tt> に紐づくセクター情報が存在しない場合に
     *          スローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定セクターに保存されているデータを、指定の <tt>content</tt>
     * で上書きします。<tt>size</tt> は <tt>content</tt> の配列サイズより小さく
     * かつ、1以上でなければなりません。<BR>
     * もし指定された <tt>sectorID</tt> に紐づくセクター情報が存在しない場合は
     * 何も行いません。
     *
     * @param sectorID  上書きするセクターのID。
     * @param size      <tt>content</tt> の書き込みバイト数。
     * @param content   上書きするデータのバイト配列。
     *
     * @return  上書きしたセクターのID。
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  IllegalArgumentException
     *          以下の場合にスローします。
     *          <ul>
     *              <li> <tt>content</tt> が空の配列だった
     *              <li> <tt>size</tt> に <tt>content</tt> の配列サイズ以上の
     *                   数が指定された
     *              <li> <code>size</code> に0以下の値が指定された
     *              <li> <tt>sectorID</tt> に紐づくセクター情報が存在しない
     *          </ul>
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定ファイルに紐づくセクターのうち、最終セクターのインスタンスを
     * 返します。
     * もし指定ファイルに紐づくセクターが存在しない場合は <tt>null<tt> を
     * 返します。
     *
     * @param fileID    ファイル識別子。
     *
     * @return  <tt>fileID</tt> に紐づく最後のセクター情報の {@link Sector}
     *          のインスタンス。もし、セクター情報が存在しない場合は 
     *          <tt>null</tt> を返します。
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定ファイルに紐づく全セクターが保持しているデータの総バイト数を
     * 返します。
     *
     * @param fileID    ファイル識別子
     *
     * @return  <tt>fileID</tt> に紐づく各セクターが保持するデータの総バイト数
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定ファイルに紐づくセクター数を返します。
     *
     * @param fileID    ファイル識別子
     *
     * @return  <tt>fileID</tt> に紐づくセクター数。
     *          セクターが存在しない場合は <tt>0</tt> を返します。
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定ファイルに紐づくセクターのセクター識別子を配列で返します。
     *
     * @param fileID    ファイル識別子
     * 
     * @return  <tt>fileID</tt> に紐づく各セクターのセクター識別子の配列。
     *          もしセクターがない場合は空の配列を返します。
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定ファイルに紐づくセクターを全て削除します。
     * セクターが無い場合は何もしません。
     *
     * @param fileID    ファイル識別子
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定セクターを削除します。
     * セクターがない場合は何もしません。
     *
     * @param sectorID  セクター識別子
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 保存されている全セクターが保持しているデータの総バイト数を返します。
     *
     * @return  全セクターが保持しているデータの総バイト数。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
     * 指定の <tt>sectorID</tt> に紐づくセクター情報が存在する場合に
     * <tt>true</tt> を返します。
     *
     * @param sectorID      セクター識別子。
     *
     * @return  指定のセクター情報があれば <tt>true</tt> を返します。
     *          無い場合は <tt>false</tt> を返します。
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  DBException
     *          データベースアクセス時にエラーが発生した場合にスローします。
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
