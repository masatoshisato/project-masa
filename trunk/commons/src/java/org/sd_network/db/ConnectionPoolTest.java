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
package org.sd_network.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.sd_network.util.Config;

/**
 * {@link org.sd_network.db.ConnectionPool} のテストケースを定義します。
 * <ul>
 *  <li> インスタンスチェック
 *      <ul>
 *       <li> デフォルトインスタンスの取得
 *       <li> 特定IDのインスタンスの取得
 *       <li> 指定のIDに紐づくConnectionParameterが存在しないエラー
 *      </ul>
 *  <li> Connection取得チェック
 *      <ul>
 *       <li> 正常（新規生成）
 *       <li> 正常（既存インスタンス）
 *       <li> Connection生成できないためのタイムアウトエラー
 *       <li> 全てのConnectionが使用されているためのタイムアウトエラー
 *      </ul>
 *  <li> Connection返却チェック
 *      <ul>
 *       <li> 正常
 *       <li> 正常（クローズされたConnectionのためPoolには返却しない）
 *       <li> Nullエラー
 *       <li> ConnectionProxy指定エラー
 *       <li> 未チェックアウトPoolに対するreleaseエラー
 *      </ul>
 *  <li> clearチェック
 *      <ul>
 *       <li> 正常
 *       <li> チェックアウトConnectionが存在するためにClearできないエラー
 *      </ul>
 * </ul>
 *
 * <p> テストケースに使用するJDBC接続情報は以下のIDを使用します。
 * <ul>
 *  <li> "default" : 接続可能
 *  <li> "test1"   : 接続可能
 *  <li> "test2    : 接続情報の設定ミスによる接続不可
 * </ul>
 *
 * <p> $Id$
 * 
 * @author Masatoshi Sato
 */
public class ConnectionPoolTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
        ConnectionPoolTest.class.getName());

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    private String _name;

    ////////////////////////////////////////////////////////////
    // Constructor and Initialization.

    public ConnectionPoolTest(String name)
        throws Exception
    {
        super(name);
        _name = name;
    }

    public void setUp()
        throws Exception
    {
        _log.log(Level.FINE, "--- test [" + _name + "]");
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
        ConnectionPool.getInstance().clear();
        ConnectionPool.getInstance("test1").clear();
        ConnectionPool.getInstance("test2").clear();
    }

    ////////////////////////////////////////////////////////////
    // インスタンスチェック

    /**
     * プール識別子を指定しないで、デフォルト識別子でConnectionPoolインスタンス
     * を取得できる事を確認します。
     */
    public void testInstanceCheck_Default()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        assertNotNull(pool);
        assertEquals("default", pool.getID());
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        assertEquals("default", parameter.getID());
    }

    /**
     * 特定の識別子でConnectionPoolのインスタンスが取得できる事を
     * 確認します。
     */
    public void testInstanceCheck_SpecifiedID()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        assertNotNull(pool);
        assertEquals("test1", pool.getID());
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        assertEquals("test1", parameter.getID());
    }

    /**
     * 特定の識別子に紐づくConnectionParameterが存在しない場合に
     * IllegalStateExceptionがスローされる事を確認します。
     */
    public void testInstanceCheck_ConnectionParameterNotFoundError()
        throws Exception
    {
        try {
            ConnectionPool.getInstance("test99");
            fail("ConnectionParameter check error.");
        } catch (ConnectionPoolException e) {
            assertEquals(
                    "ConnectionParameter not found for test99",
                    e.getMessage());
        }
    }

    //////////////////////////////////////////////////////////// 
    // Connection取得チェック

    /**
     * 新規のConnectionが生成され、取得できる事を確認します。
     */
    public void testEngageCheck_NewConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        Connection con = pool.engageConnection(1);
        assertNotNull(con);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
        con.close();
    }

    /**
     * プールされているConnectionを取得できる事を確認します。
     */
    public void testEngageCheck_ExistsConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        Connection con1 = pool.engageConnection(1);
        assertNotNull(con1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con1.close();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(1, pool.getCurrentPoolSize());
        
        Connection con2 = pool.engageConnection(1);
        assertNotNull(con2);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con2.close();
    }

    /**
     * ConnectionParameter情報が間違っているために新しいConnectionが生成
     * できず、ConnectTimeoutExceptionがスローされる事を確認します。
     */
    public void testEngageCheck_TimeoutByConnectionCreationError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test2");
        try {
            Connection con1 = pool.engageConnection(1);
            if (con1 != null)
                con1.close();
            fail("Creation of connection error.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test2]: Failed to engage connection.", e.getMessage());
        }
    }

    /**
     * 全てのConnectionが使用されているためにConnectTimeoutExceptionが
     * スローされる事を確認します。
     */
    public void testEngageCheck_TimeoutByAllConnectionIsUsingError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection[] cons = new Connection[20];
        for (int idx = 0; idx < ConnectionPool.MAX_POOL_SIZE; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(20, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        try {
            Connection con = pool.engageConnection(1);
            if (con != null)
                con.close();
            fail("Connection pool size check error.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test1]: Failed to engage connection.", e.getMessage());
            assertEquals(20, pool.getCheckedOutConnections());
            assertEquals(0, pool.getCurrentPoolSize());
        }

        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx].close();
        }
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(20, pool.getCurrentPoolSize());
    }

    //////////////////////////////////////////////////////////// 
    // Connection返却チェック

    /**
     * チェックアウトされたConnectionが正常にプールに返却できる事を
     * 確認します。
     */
    public void testReleaseCheck_OK()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con.close();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(1, pool.getCurrentPoolSize());
    }

    /**
     * 既に切断されているConnectionをプールに返却できる事を確認します。
     * ただし、インスタンスはプールされず、チェックアウト数だけカウント
     * します。
     */
    public void testReleaseCheck_ClosedConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con1 = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        Class.forName(parameter.getJDBCDriver());
        Connection con2 = DriverManager.getConnection(
                parameter.getURL(),
                parameter.getUserName(),
                parameter.getPassword());
        con2.close();
        pool.releaseConnection(con2);
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize()); // 返却なし

        // このテストでは無理やりConnectionを生成して返却処理を行っているため、
        // ConnectionPoolのチェックアウト数に矛盾が発生してしまう。
        // このため、正常にチェックアウトしたConnectionをcloseしようとすると
        // ConnectionPoolExceptionが発生する。
        // このテストだけの事なので無視する。
        try {
            con1.close();
        } catch (ConnectionPoolException e) {
            // OK.
        }
    }

    /**
     * Nullオブジェクトをプールに返却しようとした時にNullPointerExceptionが
     * スローされる事を確認します。
     */
    public void testReleaseCheck_NullError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        try {
            pool.releaseConnection(null);
            fail("Null check error.");
        } catch (NullPointerException e) {
            assertEquals("con", e.getMessage());
        }
    }

    /**
     * プールへ返却しようとしているのが{@link java.sql.Connection} ではなく、
     * {@link ConnectionProxy} だった場合にIllegalArgumentExceptionがスロー
     * される事を確認します。
     */
    public void testReleaseCheck_SpecifiedConnectionProxyError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con1 = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        
        try {
            pool.releaseConnection(con1);
            fail("Connection instance check error.");
        } catch (IllegalArgumentException e) {
            assertEquals(1, pool.getCheckedOutConnections());
            assertEquals(
                    "ConnectionProxy have to call Connection#close() method " +
                    "instead of releaseConnection(Connection) method.",
                    e.getMessage());
            assertEquals(1, pool.getCheckedOutConnections());
        }
        if (con1 != null)
            con1.close();
    }

    /**
     * チェックアウトされたConnectionが存在しないプールに対して、接続済みの
     * Connectionを返却しようとした時にConnectionPoolExceptionが発生する事を
     * 確認します。
     */
    public void testReleaseCheck_InitialPoolError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        ConnectionParameter parameter = pool.getConnectionParameter();
        Class.forName(parameter.getJDBCDriver());
        Connection con1 = DriverManager.getConnection(
                parameter.getURL(),
                parameter.getUserName(),
                parameter.getPassword());
        try {
            pool.releaseConnection(con1);
            fail("Number of checked out connection check error.");
        } catch (ConnectionPoolException e) {
            assertEquals(
                    "[test1]: checked out connection is nothing.",
                    e.getMessage());
        }
        assertTrue(con1.isClosed());
    }

    //////////////////////////////////////////////////////////// 
    // Clearチェック

    /**
     * {@link ConnectionPool#clear()} メソッドが正常に動作する事を確認します。
     * clear()メソッドが実行されると、プールに保持されているConnectionは
     * 全て切断され、全てのプール情報はクリアされます。
     */
    public void testClearCheck_OK()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");

        // 初期状態チェック
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // プールの最大値までチェックアウト
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // 全てのConnectionをリリース
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx].close();
        }
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCurrentPoolSize());

        // clear.
        pool.clear();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
    }

    /**
     * チェックアウトされているConnectionが存在する場合でも
     * {@link ConnectionPool#clear()} メソッドで初期化される事を確認します。
     */
    public void testClearCheck_ExistCheckedOutConnections()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");

        // 初期状態チェック
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Connection取得
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(cons.length, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // 半分リリース
        for (int idx = 0; idx < (cons.length / 2); idx++) {
            cons[idx].close();
        }
        assertEquals((cons.length / 2), pool.getCheckedOutConnections());
        assertEquals(cons.length - (cons.length / 2), pool.getCurrentPoolSize());

        // clear.
        pool.clear();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
    }
}
