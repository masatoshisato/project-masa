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
 * データベーステーブル <tt>sector</tt> の１レコードのエンティティを表します。
 * このエンティティクラスは読み出し専用となっており、セクターの登録／更新
 * には {@link SectorDB} を使用します。
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

    /** セクター識別子 */
    private final String _sectorID;

    /** ファイル識別子 */
    private final String _fileID;

    /** データのバイト数 */
    private int _size;

    /** セクターの順番 */
    private int _seqNum;

    /** データ */
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
     * このセクターのセクター識別子を返します。
     *
     * @return  セクター識別子
     */
    public String getSectorID() {
        return _sectorID;
    }

    /**
     * このセクターのファイル識別子を返します。
     *
     * @return  ファイル識別子
     */
    public String getFileID() {
        return _fileID;
    }

    /**
     * このセクターの順番を返します。
     *
     * @return  順番
     */
    public int getSeqNum() {
        return _seqNum;
    }

    /**
     * このセクターが保持するデータのバイト数を返します。
     *
     * @return  データのバイト数
     */
    public int getContentSize() {
        return _size;
    }

    /**
     * このセクターが保持するデータをバイト配列として返します。
     *
     * @return  データのバイト配列
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
