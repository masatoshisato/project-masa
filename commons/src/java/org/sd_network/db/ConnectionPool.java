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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sd_network.util.Config;

/**
 * {@link java.sql.Connection} のインスタンスを管理するConnectionPoolを提供
 * します。異なるデータベース接続情報を持った複数のConnectionPoolを提供する
 * 事が可能で、{@link #getInstance(String)} メソッドの引数に指定する識別子
 * によって異なるConnectionPoolインスタンスを管理します。データベース接続情報
 * は {@link ConnectionParameter} で管理しており、ConnectionPoolインスタンス
 * 毎に異なる情報を使用します。<br>
 * １つのConnectionPoolインスタンスでは、{@link #MAX_POOL_SIZE} で定義
 * されている数分の {@link java.sql.Connection} インスタンスを保持する事が
 * できます。
 *
 * <p> Connectionインスタンスは {@link #engageConnection(long)} メソッド
 * で取得します。このメソッドでは、ConnectionPoolに利用可能なConnection
 * が存在するかどうかを確認し、存在する場合はそのConnectionを返します。
 * ConnectionPoolでは、そのConnectionをpoolから削除し、使用中のConnection数を
 * カウントします。<br>
 * ConnectionPoolに利用可能なConnectionが存在せず、かつPoolしているConnection
 * 数が {@link #MAX_POOL_SIZE} に到達していいない場合、新しいConnection
 * インスタンスを生成してから、使用中のConnection数をカウントし、
 * そのConnectionを返します。<br>
 * ConnectionPoolに利用可能なConnectionが存在せず、かつPoolしているConnection
 * 数が {@link #MAX_POOL_SIZE} に到達している場合、引数に指定された
 * タイムアウト時間（秒）が経過するまで、Poolに利用可能なConnectionが返却される
 * のを待ちます。
 *
 * <p> Connectionの返却は {@link java.sql.Connection#close()} メソッドを
 * 呼び出すことで自動的に行われます。{@link #engageConnection(long)} メソッドが
 * 返すのは、実際には {@link ConnectionProxy} インスタンスであり、
 * {@link java.sql.Connection} をラップしています。ConnectionProxyのclose()
 * メソッドでは、Connectionのclose()メソッドは呼び出さず、ConnectionPool
 * への返却処理が行われるだけです。その際、全てのトランザクションはロールバック
 * されます。<br>
 * また、Connectionのclose()メソッドが呼び出されなくても、finalize()メソッド
 * が呼び出された時（GC時）にConnectionPoolへの返却処理が行われますが、
 * finalize()メソッドが呼び出されるタイミングはJVMの実装に依存する可能性が
 * あるため、これを期待した実装は行わず、必ずConnection使用後にclose()メソッド
 * を呼ぶようにしてください。
 * 
 * <p> $Id$
 *
 * @author Masatoshi sato
 */
public class ConnectionPool
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ConnectionPool.class.getName());

    /** １つのPoolで保持可能なConnectionインスタンス数 */
    public static final int MAX_POOL_SIZE = 20;

    /** ConnectionインスタンスをPoolから取得する時の待ち時間 (mSec) */
    public static final long WAIT_TIME = 1000;

    /** ConnectionPoolのインスタンスバッファ */
    private static Map<String, ConnectionPool> _instanceMap =
        new HashMap<String, ConnectionPool>();

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    /** {@link java.sql.Connection} インスタンスバッファ */
    private LinkedList <Connection> _connections;

    /** チェックアウトされている（使用中）コネクション数 */
    private int _checkedOutConnections;

    /** ConnectionPoolの識別子 */
    private String _ID;

    /** データベース接続情報を持ったConnectionParameterのインスタンス */
    private ConnectionParameter _parameter;

    //////////////////////////////////////////////////////////// 
    // Factories.

    /**
     * デフォルトの識別子に紐づいているConnectionPoolのインスタンスを返します。
     * インスタンスがない場合は、新たに生成します。ただし、同じ識別子に紐づく
     * {@link ConnectionParameter} インスタンスが存在しない場合は、データベース
     * への接続ができないため、生成に失敗します。
     *
     * <p> 使用される識別子は自動的に"default"になります。
     *
     * @return  "default"という識別子に紐づくConnectionPoolのインスタンス。
     *
     * @throws  IllegalStateException
     *          同じ識別子に紐づく {@link ConnectionParameter} インスタンスが
     *          存在しない場合にスローします。
     *
     * @see ConnectionParameter
     * @see #getInstance(String)
     */
    public static ConnectionPool getInstance() {
        return getInstance(null);
    }

    /**
     * 指定された識別子に紐づくConnectionPoolのインスタンスを返します。
     * 識別子 <tt>ID</tt> は、{@link ConnectionParameter} で使用される識別子と
     * 同じでなければなりません。
     * インスタンスがない場合は、新たに生成します。ただし、同じ識別子に紐づく
     * {@link ConnectionParameter} インスタンスが存在しない場合は、データベース
     * への接続ができないため、生成に失敗します。
     *
     * @param ID    ConnectionPoolの識別子。
     *
     * @return  識別子に紐づくConnectionPoolのインスタンス。
     *
     * @throws  ConnectionPoolException
     *          同じ識別子に紐づく {@link ConnectionParameter} インスタンスが
     *          存在しない場合にスローします。
     *
     * @see ConnectionParameter
     */
    public static ConnectionPool getInstance(String ID) {
        if (ID == null)
            ID = "default";

        ConnectionPool pool = _instanceMap.get(ID);
        if (pool == null) {
            ConnectionParameter parameter = ConnectionParameter.getInstance(ID);
            if (parameter == null)
                throw new ConnectionPoolException(
                        "ConnectionParameter not found for " + ID);
            pool = new ConnectionPool(ID, parameter);
            _instanceMap.put(ID, pool);
        }
        return pool;
    }

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * 指定された識別子とデータベース接続情報を持つ新たなConnectionPoolの
     * インスタンスを生成します。
     *
     * @param ID        インスタンス識別子
     * @param parameter データベース接続情報
     */
    private ConnectionPool(String ID, ConnectionParameter parameter) {
        _connections = new LinkedList <Connection> ();
        _checkedOutConnections = 0;
        _ID = ID;
        _parameter = parameter;
        _log.log(Level.FINE, "ConnectionPool [" + ID + "] is created.");
    }

    /////////////////////////////////////////////////////////////////////
    // Public methods.

    /**
     * ConnectionPoolにある {@link java.sql.Connection} インスタンスを
     * 返します。もしConnectionが存在しない場合、指定されたタイムアウト時間まで
     * 以下の処理を繰り返します。
     *
     * <ul>
     *  <li> ConnectionPoolに保持しているConnection数が {@link #MAX_POOL_SIZE}
     *       に達していない場合、新規にConnectionを生成します。
     *  <li> ConnectionPoolに保持しているConnection数が {@link #MAX_POOL_SIZE}
     *       に達していた場合、指定されたタイムアウト時間まで、
     *       {@link #WAIT_TIME} 毎にConnectionPoolにConnectionが返却されるのを
     *       待ち、返却されたらそのConnectionを返します。
     * </ul>
     *
     * もし、タイムアウト時間までに、Connectionの生成もしくはConnectionの
     * 返却がなかった場合、{@link org.sd_network.db.ConnectTimeoutExcepiton}
     * をスローします。
     *
     * <p> <strong>このメソッドは同期化されています。</strong>
     * このメソッド処理中は、他スレッドからのこのメソッドの呼び出しは
     * 待たされます。よって、Connectionの最大数およびConnectionのチェックアウト
     * 数は保障されます。
     *
     * @param timeout   Connectionを取得するまでのタイムアウト時間（秒）
     *
     * @return  {@link java.sql.Connection} のインスタンス。
     *
     * @throws  ConnectTimeoutException
     *          指定されたタイムアウト時間までにConnectionが取得できなかった
     *          場合にスローします。
     */
    public synchronized Connection engageConnection(long timeout) {
        Connection con = null;

        if (_connections.size() > 0) {
            con = _connections.remove(_connections.size() - 1);
            _checkedOutConnections++;
            _log.log(Level.FINE, "Checked out connection increment[" +
                    _checkedOutConnections + "].");
            return new ConnectionProxy(this, con);
        }

        for (; timeout >= 0; timeout--) {

            // ConnectionPoolに空きがある場合、新しいConnectionの生成を
            // 試みる。生成できなかった場合、ネットワーク障害やデータベース側の
            // 一時的なエラーの可能性もあるため、timeoutまで繰り返し試みる。
            if (_checkedOutConnections < MAX_POOL_SIZE) {
                con = createNewConnection();
                if (con != null) {
                    _log.log(Level.FINE,
                            "[" + _ID + "]: Created new connection.");
                    break;
                }
            }

            // Connectionの生成ができない場合、WAIT_TIMEだけ待って
            // Connectionの返却があるかどうか確認する。返却されたConnection
            // があった場合、それを取得する。ない場合はtimeoutまで繰り返す。
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
            }
            if (_connections.size() > 0) {
                con = _connections.getLast();
                break;
            }
        }
        if (con == null)
            throw new ConnectTimeoutException(
                    "[" + _ID + "]: Failed to engage connection.");

        _checkedOutConnections++;
        _log.log(Level.FINE,
                "Checked out connection increment[" +
                _checkedOutConnections + "].");
        return new ConnectionProxy(this, con);
    }

    /////////////////////////////////////////////////////////////////////
    // Package scope methods.

    /**
     * 保持しているすべてのConnectionをクローズし、ConnectionPoolを初期化
     * します。もし、このメソッドが呼ばれた時点で使用中のConnectionがある
     * 場合でも、強制的にインスタンス情報を初期化します。
     */
    void clear() {
        if (_checkedOutConnections != 0)
            _log.log(Level.WARNING,
                    "Connection of ConnectionPool [" + _ID + "] " +
                    "is still used. Number of checked out connection is " +
                    "[" + _checkedOutConnections + "].", 
                    new ConnectionPoolException());
        _log.log(Level.FINE, "ConnectionPool [" + _ID + "] is clear.");
        _checkedOutConnections = 0;

        while (_connections.size() > 0) {
            Connection con = _connections.remove(_connections.size() - 1);
            try {
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING,
                        "[" + _ID + "]: Connection could not close.", e);
            }
        }
    }

    /**
     * 指定されたConnectionをこのConnectionPoolに返却します。
     * もし、すでにクローズされたConnectionが指定された場合、ConnectionPool
     * には返却せず、チェックアウト数のみ戻します。この場合、クローズされた
     * Connectionは破棄され、{@link #engageConnection(long)} メソッドが
     * 呼ばれた時には新たなConnectionを生成するようになります。
     *
     * @param con   返却するConnectionのインスタンス
     *
     * @throws  NullPointerException
     *          <tt>con</tt> に <tt>null</tt> が指定された場合にスローします。
     *
     * @throws  IllegalArgumentException
     *          <tt>con</tt> に {@link ConnectionProxy} のインスタンスが
     *          指定された場合にスローします。ConnectionProxyのインスタンスは
     *          {@link ConnectionProxy#close()} メソッドを呼び出すことで
     *          内部からこのメソッドが正しく呼び出されます。
     */
    synchronized void releaseConnection(Connection con) {
        if (con == null)
            throw new NullPointerException("con");
        if (con instanceof ConnectionProxy) {
            throw new IllegalArgumentException(
                    "ConnectionProxy have to call Connection#close() method " +
                    "instead of releaseConnection(Connection) method.");
        }

        // チェックアウトされたConnection数がない場合、この返却は不正。
        if (_checkedOutConnections <= 0) {
            try {
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, e.getMessage());
            }
            String message =
                "[" + _ID + "]: checked out connection is nothing.";
            _log.log(Level.WARNING, message);
            throw new ConnectionPoolException(message);
        }

        // クローズされていないConnectionだけ返却処理をする。
        try {
            if (!con.isClosed())
                _connections.addFirst(con);
        } catch (SQLException e) {
            _log.log(Level.WARNING, e.getMessage());
        }
        _checkedOutConnections--;

        _log.log(Level.FINE, "Checked out connection decremented[" +
                _checkedOutConnections + "].");
    }

    //////////////////////////////////////////////////////////// 
    // Protected methods.

    /**
     * 保持しているすべてのConnectionをクローズし、ConnectionPoolを初期化
     * します。{@link #clear()} メソッドとの違いは、呼び出された時点で
     * 保持しているConnectionがまだいずれかのスレッドで利用されていても
     * 強制的に初期化します。これは、このメソッドがGCのタイミングで呼び出される
     * メソッドであり、ConnectionPoolインスタンス自体が消滅するからです。
     */
    protected void finalize()
        throws Exception
    {
        if (_checkedOutConnections != 0)
            _log.log(Level.WARNING,
                    "[" + _ID + "]: " +
                    "" +  _checkedOutConnections + 
                    " connections is still used.");

        while (_connections.size() > 0) {
            Connection con = _connections.remove(_connections.size() - 1);
            try {
                con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING,
                        "[" + _ID + "]: Connection could not close.", e);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Private scope methods.

    /**
     * このConnectionPoolが保持しているデータベース接続情報を使って、
     * {@link java.sql.Connection} のインスタンスを生成します。
     * Connectionの初期状態として自動コミットは <tt>false</tt> に設定
     * されます。 <br>
     * JDBCドライバクラスが見つからなかった場合、これ以降処理の継続が
     * 不可能なため、IllegalStateExceptionをスローします。<br>
     * JDBCドライバクラスはロードできたが接続時にSQLExceptionが発生した場合、
     * ネットワーク障害もしくはデータベースサーバ側の一時的な状態である可能性
     * もあるため、例外は生成せず、<tt>null</tt> を返します。
     *
     * @return  生成したConnectionのインスタンス。SQLExceptionの発生により
     *          生成に失敗した場合は <tt>null</tt> を返します。
     *
     * @throws  ConnectionPoolException
     *          JDBCドライバクラスのロードに失敗した場合にスローします。
     */
    private Connection createNewConnection() {
        Connection con = null;
        try {
            Class.forName(_parameter.getJDBCDriver());
            con = DriverManager.getConnection(
                    _parameter.getURL(),
                    _parameter.getUserName(),
                    _parameter.getPassword());
            con.setAutoCommit(false);
            _log.log(Level.INFO,
                    "[" + _ID + "]: " +
                    "New JDBC connection was created.");
        } catch (ClassNotFoundException e) {
            _log.log(Level.SEVERE,
                    "[" + _ID + "]: " +
                    "JDBC driver class not found. " + e.getMessage(), e);
            throw new ConnectionPoolException(
                    "[" + _ID + "]: " +
                    "JDBC driver class not found. " + e.getMessage());
        } catch (SQLException e) {
            _log.log(Level.WARNING,
                    "[" + _ID + "]: " +
                    "Failed to get connection. " + e.getMessage(), e);
        }
        return con;
    }

    /////////////////////////////////////////////////////////////////////
    // Unit test stuff.

    /**
     * このインスタンスの識別子を返します。
     */
    String getID() {
        return _ID;
    }

    /**
     * このインスタンスのデータベース接続情報を返します。
     */
    ConnectionParameter getConnectionParameter() {
        return _parameter;
    }

    /**
     * チェックアウトされているConnection数を返します。
     */
    synchronized int getCheckedOutConnections() {
        return _checkedOutConnections;
    }

    /**
     * 現在保持されているConnection数を返します。
     */
    int getCurrentPoolSize() {
        return _connections.size();
    }
}
