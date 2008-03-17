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
 * vfssectorパッケージの {@link java.io.InputStream} インタフェースの実装
 * です。vfssectorパッケージによって管理されているセクターデータベースからの
 * データ読み込みに関する処理を行います。
 *
 * <p> このインスタンスは、
 * {@link org.sd_network.vfs.sector.SectorDriver#getInputStream(String)} 
 * メソッドの戻り値です。
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

    /** データ読み込み対象ファイルの識別子 */
    private final String _fileID;

    /** １セクター分のデータバッファ */
    private byte[] _buffer;

    /** データバッファのポインタ */
    private int _bufferIdx;
    
    /** データ読み込み対象ファイルと紐づく各セクターの識別子の配列 */
    private String[] _sectorIDs;

    /** セクター識別子配列のポインタ */
    private int _sectorIdx;

    /** Stream終了フラグ */
    private boolean _closed;

    /** SectorDriverのインスタンス */
    private final VfsSectorDriver _driver;

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisations.

    /**
     * SectorInputStreamのインスタンスを生成します。
     * 以下の値が初期値として設定されます。
     * <ul>
     *  <li> データバッファの初期化
     *  <li> データバッファのindexを0に設定
     *  <li> 指定ファイルのセクター識別子配列の取得
     *  <li> セクター識別子配列のポインタを0に設定
     *  <li> 終了フラグのリセット
     * </ul>
     * １セクター分のデータバッファサイズは、セクターに保存されている
     * データバイト数に依存します。
     *
     * @param fileID        ファイル識別子
     * @param driver        SectorDriverのインスタンス
     *
     * @throws  NullPointerException
     *          引数に <tt>null</tt> が指定された場合にスローします。
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
