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

import java.util.TreeSet;

import java.util.logging.Logger;

import org.sd_network.db.DBUtil;
import org.sd_network.vfssector.test.VfsSectorTestCase;

/**
 * Sector関連クラスの単体テストケースを定義します。
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SectorTest
    extends VfsSectorTestCase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            SectorTest.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constructors and Initializations.

    public void setUp()
        throws Exception
    {
        super.setUp();
        Schema.setup();
    }

    public void tearDown()
        throws Exception
    {
        DBUtil.update("vfssector", "DELETE from sector");
    }

    //////////////////////////////////////////////////////////// 
    // Test Cases.
    
    /**
     * セクター情報の登録(create)とデータの取得(getContent)をテストします。
     */
    public void testCreateAndGet()
        throws Exception
    {
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        String fileID = "fileID1";

        String sectorID1 =
            SectorDB.create(fileID, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID, 2, content2.length, content2);

        byte[] result1 = SectorDB.getContent(sectorID1);
        assertEquals(content1, result1);

        byte[] result2 = SectorDB.getContent(sectorID2);
        assertEquals(content2, result2);
    }

    public void testCreateError()
        throws Exception
    {
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        String fileID = "fileID1";

        try {
            SectorDB.create(null, 1, content1.length, content1);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID.", e.getMessage());
        }

        try {
            SectorDB.create(fileID, -1, content1.length, content1);
            fail("seqNum validation check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("seqNum too small.", e.getMessage());
        }

        try {
            SectorDB.create(fileID, 1, 0, content1);
            fail("size minimum check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid size.", e.getMessage());
        }
        
        try {
            SectorDB.create(fileID, 1, content1.length + 1, content1);
            fail("size maximum check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid size.", e.getMessage());
        }

        try {
            SectorDB.create(fileID, 1, content1.length, null);
            fail("content null check error.");
        } catch (NullPointerException e) {
            assertEquals("content.", e.getMessage());
        }

        try {
            SectorDB.create(fileID, 1, content1.length, new byte[0]);
            fail("content empty check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("content was empty.", e.getMessage());
        }
    }

    public void testGetContentError()
        throws Exception
    {
        try {
            SectorDB.getContent(null);
            fail("sectorID check error.");
        } catch (NullPointerException e) {
            assertEquals("sectorID", e.getMessage());
        }

        try {
            byte[] content = SectorDB.getContent("1");
            if (content != null)
                fail("sectorID [1] was exists.");
            fail("Exception was not thrown and sector was not found.");
        } catch (IllegalStateException e) {
            assertEquals("Sector not found. sectorID = 1", e.getMessage());
        }
    }

    /**
     * データとデータサイズの更新をテストします。
     */
    public void testUpdate()
        throws Exception
    {
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        String fileID = "fileID1";
        String sectorID1 =
            SectorDB.create(fileID, 1, content1.length, content1);


        byte[] content2 = "0123456789".getBytes();
        String sectorID2 =
            SectorDB.update(sectorID1, content2.length, content2);

        assertEquals(sectorID1, sectorID2);

        byte[] result = SectorDB.getContent(sectorID1);
        assertEquals(content2, result);
    }

    public void testUpdateError()
        throws Exception
    {
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        String fileID = "fileID1";
        String sectorID1 =
            SectorDB.create(fileID, 1, content1.length, content1);

        try {
            SectorDB.update(null, content1.length, content1);
            fail("sectorID null check error.");
        } catch (NullPointerException e) {
            assertEquals("sectorID.", e.getMessage());
        }

        try {
            SectorDB.update("a", content1.length, content1);
            fail("sectorID validation check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("The sector was not found.", e.getMessage());
        }

        try {
            SectorDB.update(sectorID1, content1.length, null);
            fail("content null check error.");
        } catch (NullPointerException e) {
            assertEquals("content.", e.getMessage());
        }

        try {
            SectorDB.update(sectorID1, content1.length, new byte[0]);
            fail("content empty check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("content was empty.", e.getMessage());
        }

        try {
            SectorDB.update(sectorID1, 0, content1);
            fail("size minimum check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid size.", e.getMessage());
        }

        try {
            SectorDB.update(sectorID1, content1.length + 1, content1);
            fail("size maximum check error.");
        } catch (IllegalArgumentException e) {
            assertEquals("invalid size.", e.getMessage());
        }
    }

    /**
     * 特定ファイルの最終セクター情報の取得をテストします。
     */
    public void testGetLastSector()
        throws Exception
    {
        byte[] content1 = "abcde".getBytes();
        byte[] content2 = "fghij".getBytes();
        byte[] content3 = "klmno".getBytes();
        byte[] content4 = "pqrst".getBytes();
        byte[] content5 = "uvwxyz".getBytes();
        String fileID = "fileID1";

        // テストデータの登録
        String sectorID1 =
            SectorDB.create(fileID, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID, 3, content3.length, content3);
        String sectorID4 =
            SectorDB.create(fileID, 4, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID, 5, content5.length, content5);

        // test normal.
        Sector lastSector1 = SectorDB.getLastSector(fileID);
        assertNotNull(lastSector1);
        assertEquals(sectorID5, lastSector1.getSectorID());
        assertEquals(fileID, lastSector1.getFileID());
        assertEquals(5, lastSector1.getSeqNum());
        assertEquals(content5.length, lastSector1.getContentSize());
        assertEquals(content5, lastSector1.getContent());

        // シーケンス番号が欠けた場合
        SectorDB.deleteSector(sectorID3);
        Sector lastSector2 = SectorDB.getLastSector(fileID);
        assertNotNull(lastSector2);
        assertEquals(sectorID5, lastSector2.getSectorID());
        assertEquals(fileID, lastSector2.getFileID());
        assertEquals(5, lastSector2.getSeqNum());
        assertEquals(content5.length, lastSector2.getContentSize());
        assertEquals(content5, lastSector2.getContent());

        // 最終セクターが削除された場合
        SectorDB.deleteSector(sectorID5);
        Sector lastSector3 = SectorDB.getLastSector(fileID);
        assertNotNull(lastSector3);
        assertEquals(sectorID4, lastSector3.getSectorID());
        assertEquals(fileID, lastSector3.getFileID());
        assertEquals(4, lastSector3.getSeqNum());
        assertEquals(content4.length, lastSector3.getContentSize());
        assertEquals(content4, lastSector3.getContent());

        // 存在しないファイル
        assertNull(SectorDB.getLastSector("a"));
    }

    public void testGetLastSectorError()
        throws Exception
    {
        try {
            SectorDB.getLastSector(null);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID.", e.getMessage());
        }
    }

    /**
     * ファイルのバイト数の取得をテストします。
     */
    public void testGetFileSize()
        throws Exception
    {
        // 存在しないセクター
        assertEquals(0L, SectorDB.getFileSize("1"));

        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);

        // テストデータ2の登録
        String fileID2 = "fileID2";
        byte[] content4 = "abcdefghijk".getBytes();
        byte[] content5 = "01234".getBytes();
        byte[] content6 = new byte[] {0x00, 0x01, 0x02};
        String sectorID4 =
            SectorDB.create(fileID2, 1, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID2, 2, content5.length, content5);
        String sectorID6 =
            SectorDB.create(fileID2, 3, content6.length, content6);

        // テストデータ1のチェック
        long size1 = SectorDB.getFileSize(fileID1);
        assertEquals(
                content1.length + content2.length + content3.length, size1);

        // テストデータ2のチェック
        long size2 = SectorDB.getFileSize(fileID2);
        assertEquals(
                content4.length + content5.length + content6.length, size2);

        // sectorID1 を削除
        SectorDB.deleteSector(sectorID1);
        long size3 = SectorDB.getFileSize(fileID1);
        assertEquals(content2.length + content3.length, size3);

        // sectorID2 を削除
        SectorDB.deleteSector(sectorID2);
        long size4 = SectorDB.getFileSize(fileID1);
        assertEquals(content3.length, size4);

        // sectorID3 を削除
        SectorDB.deleteSector(sectorID3);
        long size5 = SectorDB.getFileSize(fileID1);
        assertEquals(0, size5);
    }

    public void testGetFileSizeError()
        throws Exception
    {
        try {
            SectorDB.getFileSize(null);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID.", e.getMessage());
        }
    }

    /**
     * 特定ファイルのセクター数の取得をテストします。
     */
    public void testGetTotalSectorNumber()
        throws Exception
    {
        // 存在しないファイル
        assertEquals(0L, SectorDB.getTotalSectorNumber("1"));

        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);

        // テストデータ2の登録
        String fileID2 = "fileID2";
        byte[] content4 = "abcdefghijk".getBytes();
        byte[] content5 = "01234".getBytes();
        String sectorID4 =
            SectorDB.create(fileID2, 1, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID2, 2, content5.length, content5);

        // テストデータ1のチェック
        long sectorNumber1 = SectorDB.getTotalSectorNumber(fileID1);
        assertEquals(3, sectorNumber1);

        // テストデータ2のチェック
        long sectorNumber2 = SectorDB.getTotalSectorNumber(fileID2);
        assertEquals(2, sectorNumber2);
    }

    public void testGetTotalSectorNumberError()
        throws Exception
    {
        try {
            SectorDB.getTotalSectorNumber(null);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID", e.getMessage());
        }
    }

    /**
     * 特定ファイルに紐づく各セクターのセクター識別子の取得をテストします。
     */
    public void testGetSectorIDs()
        throws Exception
    {
        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);
        String[] sectorIDs1 = new String[] {sectorID1, sectorID2, sectorID3};

        // テストデータ2の登録
        String fileID2 = "fileID2";
        byte[] content4 = "abcdefghijk".getBytes();
        byte[] content5 = "01234".getBytes();
        String sectorID4 =
            SectorDB.create(fileID2, 1, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID2, 2, content5.length, content5);
        String[] sectorIDs2 = new String[] {sectorID4, sectorID5};

        String[] result1 = SectorDB.getSectorIDs(fileID1);
        assertEquals(3, result1.length);
        for (int idx = 0; idx < result1.length; idx++) {
            assertEquals(sectorIDs1[idx], result1[idx]);
        }

        String[] result2 = SectorDB.getSectorIDs(fileID2);
        assertEquals(2, result2.length);
        for (int idx = 0; idx < result2.length; idx++) {
            assertEquals(sectorIDs2[idx], result2[idx]);
        }

        String[] result3 = SectorDB.getSectorIDs("a");
        assertEquals(0, result3.length);
    }

    public void testGetSectorIDsError()
        throws Exception
    {
        try {
            SectorDB.getSectorIDs(null);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID", e.getMessage());
        }
    }

    public void testDeleteSectors()
        throws Exception
    {
        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);
        String[] sectorIDs1 = new String[] {sectorID1, sectorID2, sectorID3};

        // テストデータ2の登録
        String fileID2 = "fileID2";
        byte[] content4 = "abcdefghijk".getBytes();
        byte[] content5 = "01234".getBytes();
        String sectorID4 =
            SectorDB.create(fileID2, 1, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID2, 2, content5.length, content5);
        String[] sectorIDs2 = new String[] {sectorID4, sectorID5};

        String[] result1 = SectorDB.getSectorIDs(fileID1);
        assertEquals(3, result1.length);
        String[] result2 = SectorDB.getSectorIDs(fileID2);
        assertEquals(2, result2.length);

        SectorDB.deleteSectors(fileID1);

        String[] result3 = SectorDB.getSectorIDs(fileID1);
        assertEquals(0, result3.length);
        String[] result4 = SectorDB.getSectorIDs(fileID2);
        assertEquals(2, result4.length);

        SectorDB.deleteSectors(fileID2);

        String[] result5 = SectorDB.getSectorIDs(fileID1);
        assertEquals(0, result5.length);
        String[] result6 = SectorDB.getSectorIDs(fileID2);
        assertEquals(0, result6.length);

        SectorDB.deleteSectors("");
    }

    public void testDeleteSectorsError()
        throws Exception
    {
        try {
            SectorDB.deleteSectors(null);
            fail("fileID null check error.");
        } catch (NullPointerException e) {
            assertEquals("fileID", e.getMessage());
        }
    }

    public void testDeleteSector()
        throws Exception
    {
        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);
        String[] sectorIDs1 = new String[] {sectorID1, sectorID2, sectorID3};

        String[] result1 = SectorDB.getSectorIDs(fileID1);
        assertEquals(3, result1.length);
        assertEquals(sectorID1, result1[0]);
        assertEquals(sectorID2, result1[1]);
        assertEquals(sectorID3, result1[2]);

        SectorDB.deleteSector(sectorID2);

        String[] result2 = SectorDB.getSectorIDs(fileID1);
        assertEquals(2, result2.length);
        assertEquals(sectorID1, result2[0]);
        assertEquals(sectorID3, result2[1]);

        SectorDB.deleteSector(sectorID1);

        String[] result3 = SectorDB.getSectorIDs(fileID1);
        assertEquals(1, result3.length);
        assertEquals(sectorID3, result3[0]);

        SectorDB.deleteSector(sectorID3);

        String[] result4 = SectorDB.getSectorIDs(fileID1);
        assertEquals(0, result4.length);

        SectorDB.deleteSector("");
    }

    public void testDeleteSectorError()
        throws Exception
    {
        try {
            SectorDB.deleteSector(null);
            fail("sectorID null check error.");
        } catch (NullPointerException e) {
            assertEquals("sectorID", e.getMessage());
        }
    }

    public void testGetUsedBytes()
        throws Exception
    {
        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);
        String[] sectorIDs1 = new String[] {sectorID1, sectorID2, sectorID3};

        // テストデータ2の登録
        String fileID2 = "fileID2";
        byte[] content4 = "abcdefghijk".getBytes();
        byte[] content5 = "01234".getBytes();
        String sectorID4 =
            SectorDB.create(fileID2, 1, content4.length, content4);
        String sectorID5 =
            SectorDB.create(fileID2, 2, content5.length, content5);
        String[] sectorIDs2 = new String[] {sectorID4, sectorID5};

        long expect1 =
            SectorDB.getFileSize(fileID1) + SectorDB.getFileSize(fileID2);
        assertEquals(expect1, SectorDB.getUsedBytes());

        SectorDB.deleteSectors(fileID1);
        SectorDB.deleteSectors(fileID2);

        assertEquals(0, SectorDB.getUsedBytes());
    }

    public void testSector()
        throws Exception
    {
        // テストデータ1の登録
        String fileID1 = "fileID1";
        byte[] content1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        byte[] content2 = "0123456789".getBytes();
        byte[] content3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        String sectorID1 =
            SectorDB.create(fileID1, 1, content1.length, content1);
        String sectorID2 =
            SectorDB.create(fileID1, 2, content2.length, content2);
        String sectorID3 =
            SectorDB.create(fileID1, 3, content3.length, content3);

        Sector sector1 = SectorDB.getLastSector(fileID1);
        assertNotNull(sector1);

        assertEquals(sectorID3, sector1.getSectorID());
        assertEquals(fileID1, sector1.getFileID());
        assertEquals(3, sector1.getSeqNum());
        assertEquals(6, sector1.getContentSize());
        assertEquals(content3, sector1.getContent());
    }
}
