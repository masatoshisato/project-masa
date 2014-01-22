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

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

import org.sd_network.util.ThrowableUtil;

/**
 * {@link java.sql.Connection}のプロキシです。オリジナルの Connection
 * インスタンスを保持し、{@link ConnectionPool} と連携して Connection の
 * ライフサイクルを管理します。インスタンス生成および{@link #close()}
 * メソッドを除く全てのメソッドは、オリジナルのConnectionへそのまま処理を
 * 移譲します。
 *
 * <p> このクラスのインスタンスは {@link ConnectionPool} で生成され、そのまま
 * ConnectionPoolで管理されますので、通常他のクラスからインスタンスを生成
 * する事はありません。<br>
 * このクラスの {@link #close()} メソッドでは、実際にはオリジナルの
 * Connectionはcloseせず、ConnectionをConnectionPoolへ返却し、インスタンス
 * フィールドを無効化するだけです。オリジナルのConnectionをcloseするには、
 * 全てのConnectionインスタンスをConnectionPoolへ返却（{@link #close()}
 * メソッドを呼ぶだけ）した後、{@link ConnectionPool#clear()} メソッドを
 * 呼び出します。
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class ConnectionProxy
    implements Connection
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ConnectionProxy.class.getName());

    /////////////////////////////////////////////////////////////////////
    // Instance fields.

    /** オリジナルの {@link java.sql.Connection} のインスタンス */
    private Connection _con;

    /** このインスタンスを管理する {@link ConnectionPool} のインスタンス */
    private ConnectionPool _pool;

    /** このインスタンスの識別子 */
    private String _ID;

    /////////////////////////////////////////////////////////////////////
    // Constructors.

    /**
     * インスタンスを生成します。
     * このメソッドは {@link ConnectionPool} で使用されます。
     *
     * @param pool  このインスタンスを管理する ConnectionPool のインスタンス。
     * @param con   オリジナルの Connection のインスタンス。
     *
     * @throws  NullPointerException
     *          <tt>con</tt> もしくは <tt>pool</tt> に <tt>null</tt> が指定
     *          された場合にスローします。
     */
    ConnectionProxy(ConnectionPool pool, Connection con) {
        if (con == null)
            throw new NullPointerException("con");
        if (pool == null)
            throw new NullPointerException("pool");
        _pool = pool;
        _con = con;
        _ID = UUID.randomUUID().toString();
        _log.log(Level.FINE, "Create ConnectionProxy[" + _ID + "].");
    }

    //////////////////////////////////////////////////////////// 
    // Implementation of java.sql.Connection for ConnectionPool.

    /**
     * このインスタンスが保持しているConnectionが既にConnectionPoolに
     * 返却されているかどうかを例外スローせずに確認します。
     * このインスタンスが保持しているConnectionが <tt>null</tt> の場合、
     * 既にPoolに返却済みであり、closeされているとみなします。
     *
     * @return  Connectionが既にConnectionPoolに返却済みの場合は <tt>true</tt>
     *          を返します。
     */
    public boolean isClosed() {
        return (_con == null);
    }

    /**
     * このインスタンスが保持しているオリジナルの {@link java.sql.Connection} 
     * インスタンスを {@link ConnectionPool} へ返却します。
     * 既に返却済みの場合は何も行いません。
     *
     * @throws  ConnectionPoolException
     *          返却処理中にConnectionPoolでエラーが発生した場合に
     *          スローします。
     */
    public void close() {
        if (isClosed()) {
            Throwable t = new IllegalStateException(
                    "ConnectionProxy[" + _ID + "] is already closed.");
            _log.log(Level.WARNING, 
                    ThrowableUtil.toStackTraceString(t));
            return;
        }

        try {
            rollback();
        } catch (SQLException e) {
            _log.log(Level.WARNING, e.getMessage());
            // ignore excetion that occured this point.
        }

        try {
            _pool.releaseConnection(_con);
            _log.log(Level.FINE, "Close ConnectionProxy[" + _ID + "].");
        } catch (ConnectionPoolException e) {
            throw e;
        } finally {
            _con = null;
        }
    }
    
    /////////////////////////////////////////////////////////////////////
    // Public methods.

    /**
     * このインスタンスの文字列形式を返します。
     * ConnectionProxyのインスタンスは {@link ConnectionPool} から
     * Connectionを取得する際に毎回生成されますが、保持しているConnection
     * 自体はPoolされているインスタンスが設定されます。
     */
    public String toString() {
        return _con.toString();
    }

    /////////////////////////////////////////////////////////////////////
    // Protected methods.

    /**
     * このインスタンスに保持しているConnectionが {@link ConnectionPool}
     * にリリースされていない場合、リリース処理を行います。
     * 
     * <p> 通常、このメソッドはGC実行時に呼び出されますが、このメソッドが
     * 呼び出される前に、明示的に {@link #close()} メソッドを呼び出し、
     * {@link ConnectionPool} にリリースする必要があります。<BR>
     * 例えば、{@link #close()} を呼び出さずConnectionのリリース処理を
     * GCからの呼び出しに頼ってしまうと、チェックアウトされたConnectionは
     * GCが実行されるまでメモリ上に放置され、{@link ConnectionPool} は
     * チェックアウトされたConnectionで一杯になり、このメソッドがGCによって
     * 呼び出されるまで、{@link ConnectionPool} からConnectionが取得
     * できなくなる可能性があるためです。
     */
    protected void finalize() {
        if (isClosed())
            return;
        _log.log(Level.SEVERE,
                "You have to close this connection before finalize() method " +
                "is called. [" + _ID + "]");
        close();
    }

    /////////////////////////////////////////////////////////////////////
    // Private methods.

    /**
     * インスタンスに保持されているConnectionが既にクローズされているか
     * どうかを確認します。
     * 既にクローズされている場合、IllegalStateExceptionをスローします。
     * 例外をスローせずにチェックしたい場合は {@link #isClosed()} メソッドを
     * 使用してください。
     *
     * @throws  IllegalStateException
     *          既にクローズされている場合にスローします。
     */
    private void checkClosed() {
        if (isClosed())
            throw new IllegalStateException(
                "This connection is already released.");
    }

    ////////////////////////////////////////////////////////////////////
    // Package method for Emergency.

    /**
     * Connectionを強制的にクローズします。
     *
     * <p> このメソッドは修復不可能なSQLException等が発生した場合に使用する
     * 事を前提としており、通常は使用しないでください。
     */
    void closeForce() {
        try {
            _con.close();
        } catch (SQLException e) {
            _log.warning(e.getMessage());
        }
        _con = null;
    }

    //////////////////////////////////////////////////////////// 
    // Proxy methods for Wrapper interface.
    
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        checkClosed();
        return _con.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        checkClosed();
        return _con.unwrap(iface);
    }

    //////////////////////////////////////////////////////////// 
    // Proxy methods for JDBC ver 4.0

    public Struct createStruct(String typeName, Object[] attributes)
        throws SQLException
    {
        checkClosed();
        return _con.createStruct(typeName, attributes);
    }

    public Array createArrayOf(String typeName, Object[] elements)
        throws SQLException
    {
        checkClosed();
        return _con.createArrayOf(typeName, elements);
    }

    public Properties getClientInfo()
        throws SQLException
    {
        checkClosed();
        return _con.getClientInfo();
    }

    public String getClientInfo(String name)
        throws SQLException
    {
        checkClosed();
        return _con.getClientInfo(name);
    }

    public void setClientInfo(Properties properties)
        throws SQLClientInfoException
    {
        checkClosed();
        _con.setClientInfo(properties);
    }

    public void setClientInfo(String name, String value)
        throws SQLClientInfoException
    {
        checkClosed();
        _con.setClientInfo(name, value);
    }

    public boolean isValid(int timeout)
        throws SQLException
    {
        checkClosed();
        return _con.isValid(timeout);
    }

    public SQLXML createSQLXML()
        throws SQLException
    {
        checkClosed();
        return _con.createSQLXML();
    }

    public NClob createNClob()
        throws SQLException
    {
        checkClosed();
        return _con.createNClob();
    }

    public Blob createBlob()
        throws SQLException
    {
        checkClosed();
        return _con.createBlob();
    }

    public Clob createClob()
        throws SQLException
    {
        checkClosed();
        return _con.createClob();
    }

    /////////////////////////////////////////////////////////////////////
    // Proxy methods for java.sql.Connection class.

    public void clearWarnings()
        throws SQLException
    {
        checkClosed();
        _con.clearWarnings();
    }

    public void commit()
        throws SQLException
    {
        checkClosed();
        _con.commit();
    }

    public Statement createStatement()
        throws SQLException
    {
        checkClosed();
        return _con.createStatement();
    }

    public Statement createStatement(int rsType, int rsConcurrency)
        throws SQLException
    {
        checkClosed();
        return _con.createStatement(rsType, rsConcurrency);
    }

    public boolean getAutoCommit()
        throws SQLException
    {
        checkClosed();
        return _con.getAutoCommit();
    }

    public String getCatalog()
        throws SQLException
    {
        checkClosed();
        return _con.getCatalog();
    }

    public DatabaseMetaData getMetaData()
        throws SQLException
    {
        checkClosed();
        return _con.getMetaData();
    }

    public int getTransactionIsolation()
        throws SQLException
    {
        checkClosed();
        return _con.getTransactionIsolation();
    }

    public Map<String, Class<?>> getTypeMap()
        throws SQLException
    {
        checkClosed();
        return _con.getTypeMap();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        checkClosed();
        return _con.getWarnings();
    }

    public boolean isReadOnly()
        throws SQLException
    {
        checkClosed();
        return _con.isReadOnly();
    }

    public String nativeSQL(String sql)
        throws SQLException
    {
        checkClosed();
        return _con.nativeSQL(sql);
    }

    public CallableStatement prepareCall(String sql)
        throws SQLException
    {
        checkClosed();
        return _con.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int rsType,
        int rsConcurrency)
        throws SQLException
    {
        checkClosed();
        return _con.prepareCall(sql, rsType, rsConcurrency);
    }

    public PreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int rsType,
        int rsConcurrency)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql, rsType, rsConcurrency);
    }

    public void rollback()
        throws SQLException
    {
        checkClosed();
        _con.rollback();
    }

    public void setAutoCommit(boolean autoCommit)
        throws SQLException
    {
        checkClosed();
        _con.setAutoCommit(autoCommit);
    }

    public void setCatalog(String catalog)
        throws SQLException
    {
        checkClosed();
        _con.setCatalog(catalog);
    }

    public void setReadOnly(boolean readOnly)
        throws SQLException
    {
        checkClosed();
        _con.setReadOnly(readOnly);
    }

    public void setTransactionIsolation(int level)
        throws SQLException
    {
        checkClosed();
        _con.setTransactionIsolation(level);
    }

    public void setTypeMap(Map<String, Class<?>> map)
        throws SQLException
    {
        checkClosed();
        _con.setTypeMap(map);
    }

    public void setHoldability(int holdability)
        throws SQLException
    {
        checkClosed();
        _con.setHoldability(holdability);
    }

    public int getHoldability()
        throws SQLException
    {
        checkClosed();
        return _con.getHoldability();
    }

    public Savepoint setSavepoint()
        throws SQLException
    {
        checkClosed();
        return _con.setSavepoint();
    }

    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        checkClosed();
        return _con.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        checkClosed();
        _con.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        checkClosed();
        _con.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        checkClosed();
        return _con.createStatement(resultSetType, resultSetConcurrency,
            resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        checkClosed();
        return _con.prepareCall(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGenerateKeys)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql, autoGenerateKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException
    {
        checkClosed();
        return _con.prepareStatement(sql, columnNames);
    }
}
