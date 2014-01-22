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
 * {@link java.sql.Connection}�̃v���L�V�ł��B�I���W�i���� Connection
 * �C���X�^���X��ێ����A{@link ConnectionPool} �ƘA�g���� Connection ��
 * ���C�t�T�C�N�����Ǘ����܂��B�C���X�^���X���������{@link #close()}
 * ���\�b�h�������S�Ẵ��\�b�h�́A�I���W�i����Connection�ւ��̂܂܏�����
 * �ڏ����܂��B
 *
 * <p> ���̃N���X�̃C���X�^���X�� {@link ConnectionPool} �Ő�������A���̂܂�
 * ConnectionPool�ŊǗ�����܂��̂ŁA�ʏ푼�̃N���X����C���X�^���X�𐶐�
 * ���鎖�͂���܂���B<br>
 * ���̃N���X�� {@link #close()} ���\�b�h�ł́A���ۂɂ̓I���W�i����
 * Connection��close�����AConnection��ConnectionPool�֕ԋp���A�C���X�^���X
 * �t�B�[���h�𖳌������邾���ł��B�I���W�i����Connection��close����ɂ́A
 * �S�Ă�Connection�C���X�^���X��ConnectionPool�֕ԋp�i{@link #close()}
 * ���\�b�h���ĂԂ����j������A{@link ConnectionPool#clear()} ���\�b�h��
 * �Ăяo���܂��B
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

    /** �I���W�i���� {@link java.sql.Connection} �̃C���X�^���X */
    private Connection _con;

    /** ���̃C���X�^���X���Ǘ����� {@link ConnectionPool} �̃C���X�^���X */
    private ConnectionPool _pool;

    /** ���̃C���X�^���X�̎��ʎq */
    private String _ID;

    /////////////////////////////////////////////////////////////////////
    // Constructors.

    /**
     * �C���X�^���X�𐶐����܂��B
     * ���̃��\�b�h�� {@link ConnectionPool} �Ŏg�p����܂��B
     *
     * @param pool  ���̃C���X�^���X���Ǘ����� ConnectionPool �̃C���X�^���X�B
     * @param con   �I���W�i���� Connection �̃C���X�^���X�B
     *
     * @throws  NullPointerException
     *          <tt>con</tt> �������� <tt>pool</tt> �� <tt>null</tt> ���w��
     *          ���ꂽ�ꍇ�ɃX���[���܂��B
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
     * ���̃C���X�^���X���ێ����Ă���Connection������ConnectionPool��
     * �ԋp����Ă��邩�ǂ������O�X���[�����Ɋm�F���܂��B
     * ���̃C���X�^���X���ێ����Ă���Connection�� <tt>null</tt> �̏ꍇ�A
     * ����Pool�ɕԋp�ς݂ł���Aclose����Ă���Ƃ݂Ȃ��܂��B
     *
     * @return  Connection������ConnectionPool�ɕԋp�ς݂̏ꍇ�� <tt>true</tt>
     *          ��Ԃ��܂��B
     */
    public boolean isClosed() {
        return (_con == null);
    }

    /**
     * ���̃C���X�^���X���ێ����Ă���I���W�i���� {@link java.sql.Connection} 
     * �C���X�^���X�� {@link ConnectionPool} �֕ԋp���܂��B
     * ���ɕԋp�ς݂̏ꍇ�͉����s���܂���B
     *
     * @throws  ConnectionPoolException
     *          �ԋp��������ConnectionPool�ŃG���[�����������ꍇ��
     *          �X���[���܂��B
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
     * ���̃C���X�^���X�̕�����`����Ԃ��܂��B
     * ConnectionProxy�̃C���X�^���X�� {@link ConnectionPool} ����
     * Connection���擾����ۂɖ��񐶐�����܂����A�ێ����Ă���Connection
     * ���̂�Pool����Ă���C���X�^���X���ݒ肳��܂��B
     */
    public String toString() {
        return _con.toString();
    }

    /////////////////////////////////////////////////////////////////////
    // Protected methods.

    /**
     * ���̃C���X�^���X�ɕێ����Ă���Connection�� {@link ConnectionPool}
     * �Ƀ����[�X����Ă��Ȃ��ꍇ�A�����[�X�������s���܂��B
     * 
     * <p> �ʏ�A���̃��\�b�h��GC���s���ɌĂяo����܂����A���̃��\�b�h��
     * �Ăяo�����O�ɁA�����I�� {@link #close()} ���\�b�h���Ăяo���A
     * {@link ConnectionPool} �Ƀ����[�X����K�v������܂��B<BR>
     * �Ⴆ�΁A{@link #close()} ���Ăяo����Connection�̃����[�X������
     * GC����̌Ăяo���ɗ����Ă��܂��ƁA�`�F�b�N�A�E�g���ꂽConnection��
     * GC�����s�����܂Ń�������ɕ��u����A{@link ConnectionPool} ��
     * �`�F�b�N�A�E�g���ꂽConnection�ň�t�ɂȂ�A���̃��\�b�h��GC�ɂ����
     * �Ăяo�����܂ŁA{@link ConnectionPool} ����Connection���擾
     * �ł��Ȃ��Ȃ�\�������邽�߂ł��B
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
     * �C���X�^���X�ɕێ�����Ă���Connection�����ɃN���[�Y����Ă��邩
     * �ǂ������m�F���܂��B
     * ���ɃN���[�Y����Ă���ꍇ�AIllegalStateException���X���[���܂��B
     * ��O���X���[�����Ƀ`�F�b�N�������ꍇ�� {@link #isClosed()} ���\�b�h��
     * �g�p���Ă��������B
     *
     * @throws  IllegalStateException
     *          ���ɃN���[�Y����Ă���ꍇ�ɃX���[���܂��B
     */
    private void checkClosed() {
        if (isClosed())
            throw new IllegalStateException(
                "This connection is already released.");
    }

    ////////////////////////////////////////////////////////////////////
    // Package method for Emergency.

    /**
     * Connection�������I�ɃN���[�Y���܂��B
     *
     * <p> ���̃��\�b�h�͏C���s�\��SQLException�������������ꍇ�Ɏg�p����
     * ����O��Ƃ��Ă���A�ʏ�͎g�p���Ȃ��ł��������B
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
