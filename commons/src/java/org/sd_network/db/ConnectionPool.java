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
 * {@link java.sql.Connection} �̃C���X�^���X���Ǘ�����ConnectionPool���
 * ���܂��B�قȂ�f�[�^�x�[�X�ڑ�����������������ConnectionPool��񋟂���
 * �����\�ŁA{@link #getInstance(String)} ���\�b�h�̈����Ɏw�肷�鎯�ʎq
 * �ɂ���ĈقȂ�ConnectionPool�C���X�^���X���Ǘ����܂��B�f�[�^�x�[�X�ڑ����
 * �� {@link ConnectionParameter} �ŊǗ����Ă���AConnectionPool�C���X�^���X
 * ���ɈقȂ�����g�p���܂��B<br>
 * �P��ConnectionPool�C���X�^���X�ł́A{@link #MAX_POOL_SIZE} �Œ�`
 * ����Ă��鐔���� {@link java.sql.Connection} �C���X�^���X��ێ����鎖��
 * �ł��܂��B
 *
 * <p> Connection�C���X�^���X�� {@link #engageConnection(long)} ���\�b�h
 * �Ŏ擾���܂��B���̃��\�b�h�ł́AConnectionPool�ɗ��p�\��Connection
 * �����݂��邩�ǂ������m�F���A���݂���ꍇ�͂���Connection��Ԃ��܂��B
 * ConnectionPool�ł́A����Connection��pool����폜���A�g�p����Connection����
 * �J�E���g���܂��B<br>
 * ConnectionPool�ɗ��p�\��Connection�����݂����A����Pool���Ă���Connection
 * ���� {@link #MAX_POOL_SIZE} �ɓ��B���Ă����Ȃ��ꍇ�A�V����Connection
 * �C���X�^���X�𐶐����Ă���A�g�p����Connection�����J�E���g���A
 * ����Connection��Ԃ��܂��B<br>
 * ConnectionPool�ɗ��p�\��Connection�����݂����A����Pool���Ă���Connection
 * ���� {@link #MAX_POOL_SIZE} �ɓ��B���Ă���ꍇ�A�����Ɏw�肳�ꂽ
 * �^�C���A�E�g���ԁi�b�j���o�߂���܂ŁAPool�ɗ��p�\��Connection���ԋp�����
 * �̂�҂��܂��B
 *
 * <p> Connection�̕ԋp�� {@link java.sql.Connection#close()} ���\�b�h��
 * �Ăяo�����ƂŎ����I�ɍs���܂��B{@link #engageConnection(long)} ���\�b�h��
 * �Ԃ��̂́A���ۂɂ� {@link ConnectionProxy} �C���X�^���X�ł���A
 * {@link java.sql.Connection} �����b�v���Ă��܂��BConnectionProxy��close()
 * ���\�b�h�ł́AConnection��close()���\�b�h�͌Ăяo�����AConnectionPool
 * �ւ̕ԋp�������s���邾���ł��B���̍ہA�S�Ẵg�����U�N�V�����̓��[���o�b�N
 * ����܂��B<br>
 * �܂��AConnection��close()���\�b�h���Ăяo����Ȃ��Ă��Afinalize()���\�b�h
 * ���Ăяo���ꂽ���iGC���j��ConnectionPool�ւ̕ԋp�������s���܂����A
 * finalize()���\�b�h���Ăяo�����^�C�~���O��JVM�̎����Ɉˑ�����\����
 * ���邽�߁A��������҂��������͍s�킸�A�K��Connection�g�p���close()���\�b�h
 * ���ĂԂ悤�ɂ��Ă��������B
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

    /** �P��Pool�ŕێ��\��Connection�C���X�^���X�� */
    public static final int MAX_POOL_SIZE = 20;

    /** Connection�C���X�^���X��Pool����擾���鎞�̑҂����� (mSec) */
    public static final long WAIT_TIME = 1000;

    /** ConnectionPool�̃C���X�^���X�o�b�t�@ */
    private static Map<String, ConnectionPool> _instanceMap =
        new HashMap<String, ConnectionPool>();

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    /** {@link java.sql.Connection} �C���X�^���X�o�b�t�@ */
    private LinkedList <Connection> _connections;

    /** �`�F�b�N�A�E�g����Ă���i�g�p���j�R�l�N�V������ */
    private int _checkedOutConnections;

    /** ConnectionPool�̎��ʎq */
    private String _ID;

    /** �f�[�^�x�[�X�ڑ�����������ConnectionParameter�̃C���X�^���X */
    private ConnectionParameter _parameter;

    //////////////////////////////////////////////////////////// 
    // Factories.

    /**
     * �f�t�H���g�̎��ʎq�ɕR�Â��Ă���ConnectionPool�̃C���X�^���X��Ԃ��܂��B
     * �C���X�^���X���Ȃ��ꍇ�́A�V���ɐ������܂��B�������A�������ʎq�ɕR�Â�
     * {@link ConnectionParameter} �C���X�^���X�����݂��Ȃ��ꍇ�́A�f�[�^�x�[�X
     * �ւ̐ڑ����ł��Ȃ����߁A�����Ɏ��s���܂��B
     *
     * <p> �g�p����鎯�ʎq�͎����I��"default"�ɂȂ�܂��B
     *
     * @return  "default"�Ƃ������ʎq�ɕR�Â�ConnectionPool�̃C���X�^���X�B
     *
     * @throws  IllegalStateException
     *          �������ʎq�ɕR�Â� {@link ConnectionParameter} �C���X�^���X��
     *          ���݂��Ȃ��ꍇ�ɃX���[���܂��B
     *
     * @see ConnectionParameter
     * @see #getInstance(String)
     */
    public static ConnectionPool getInstance() {
        return getInstance(null);
    }

    /**
     * �w�肳�ꂽ���ʎq�ɕR�Â�ConnectionPool�̃C���X�^���X��Ԃ��܂��B
     * ���ʎq <tt>ID</tt> �́A{@link ConnectionParameter} �Ŏg�p����鎯�ʎq��
     * �����łȂ���΂Ȃ�܂���B
     * �C���X�^���X���Ȃ��ꍇ�́A�V���ɐ������܂��B�������A�������ʎq�ɕR�Â�
     * {@link ConnectionParameter} �C���X�^���X�����݂��Ȃ��ꍇ�́A�f�[�^�x�[�X
     * �ւ̐ڑ����ł��Ȃ����߁A�����Ɏ��s���܂��B
     *
     * @param ID    ConnectionPool�̎��ʎq�B
     *
     * @return  ���ʎq�ɕR�Â�ConnectionPool�̃C���X�^���X�B
     *
     * @throws  ConnectionPoolException
     *          �������ʎq�ɕR�Â� {@link ConnectionParameter} �C���X�^���X��
     *          ���݂��Ȃ��ꍇ�ɃX���[���܂��B
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
     * �w�肳�ꂽ���ʎq�ƃf�[�^�x�[�X�ڑ��������V����ConnectionPool��
     * �C���X�^���X�𐶐����܂��B
     *
     * @param ID        �C���X�^���X���ʎq
     * @param parameter �f�[�^�x�[�X�ڑ����
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
     * ConnectionPool�ɂ��� {@link java.sql.Connection} �C���X�^���X��
     * �Ԃ��܂��B����Connection�����݂��Ȃ��ꍇ�A�w�肳�ꂽ�^�C���A�E�g���Ԃ܂�
     * �ȉ��̏������J��Ԃ��܂��B
     *
     * <ul>
     *  <li> ConnectionPool�ɕێ����Ă���Connection���� {@link #MAX_POOL_SIZE}
     *       �ɒB���Ă��Ȃ��ꍇ�A�V�K��Connection�𐶐����܂��B
     *  <li> ConnectionPool�ɕێ����Ă���Connection���� {@link #MAX_POOL_SIZE}
     *       �ɒB���Ă����ꍇ�A�w�肳�ꂽ�^�C���A�E�g���Ԃ܂ŁA
     *       {@link #WAIT_TIME} ����ConnectionPool��Connection���ԋp�����̂�
     *       �҂��A�ԋp���ꂽ�炻��Connection��Ԃ��܂��B
     * </ul>
     *
     * �����A�^�C���A�E�g���Ԃ܂łɁAConnection�̐�����������Connection��
     * �ԋp���Ȃ������ꍇ�A{@link org.sd_network.db.ConnectTimeoutExcepiton}
     * ���X���[���܂��B
     *
     * <p> <strong>���̃��\�b�h�͓���������Ă��܂��B</strong>
     * ���̃��\�b�h�������́A���X���b�h����̂��̃��\�b�h�̌Ăяo����
     * �҂�����܂��B����āAConnection�̍ő吔�����Connection�̃`�F�b�N�A�E�g
     * ���͕ۏႳ��܂��B
     *
     * @param timeout   Connection���擾����܂ł̃^�C���A�E�g���ԁi�b�j
     *
     * @return  {@link java.sql.Connection} �̃C���X�^���X�B
     *
     * @throws  ConnectTimeoutException
     *          �w�肳�ꂽ�^�C���A�E�g���Ԃ܂ł�Connection���擾�ł��Ȃ�����
     *          �ꍇ�ɃX���[���܂��B
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

            // ConnectionPool�ɋ󂫂�����ꍇ�A�V����Connection�̐�����
            // ���݂�B�����ł��Ȃ������ꍇ�A�l�b�g���[�N��Q��f�[�^�x�[�X����
            // �ꎞ�I�ȃG���[�̉\�������邽�߁Atimeout�܂ŌJ��Ԃ����݂�B
            if (_checkedOutConnections < MAX_POOL_SIZE) {
                con = createNewConnection();
                if (con != null) {
                    _log.log(Level.FINE,
                            "[" + _ID + "]: Created new connection.");
                    break;
                }
            }

            // Connection�̐������ł��Ȃ��ꍇ�AWAIT_TIME�����҂���
            // Connection�̕ԋp�����邩�ǂ����m�F����B�ԋp���ꂽConnection
            // ���������ꍇ�A������擾����B�Ȃ��ꍇ��timeout�܂ŌJ��Ԃ��B
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
     * �ێ����Ă��邷�ׂĂ�Connection���N���[�Y���AConnectionPool��������
     * ���܂��B�����A���̃��\�b�h���Ă΂ꂽ���_�Ŏg�p����Connection������
     * �ꍇ�ł��A�����I�ɃC���X�^���X�������������܂��B
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
     * �w�肳�ꂽConnection������ConnectionPool�ɕԋp���܂��B
     * �����A���łɃN���[�Y���ꂽConnection���w�肳�ꂽ�ꍇ�AConnectionPool
     * �ɂ͕ԋp�����A�`�F�b�N�A�E�g���̂ݖ߂��܂��B���̏ꍇ�A�N���[�Y���ꂽ
     * Connection�͔j������A{@link #engageConnection(long)} ���\�b�h��
     * �Ă΂ꂽ���ɂ͐V����Connection�𐶐�����悤�ɂȂ�܂��B
     *
     * @param con   �ԋp����Connection�̃C���X�^���X
     *
     * @throws  NullPointerException
     *          <tt>con</tt> �� <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          <tt>con</tt> �� {@link ConnectionProxy} �̃C���X�^���X��
     *          �w�肳�ꂽ�ꍇ�ɃX���[���܂��BConnectionProxy�̃C���X�^���X��
     *          {@link ConnectionProxy#close()} ���\�b�h���Ăяo�����Ƃ�
     *          �������炱�̃��\�b�h���������Ăяo����܂��B
     */
    synchronized void releaseConnection(Connection con) {
        if (con == null)
            throw new NullPointerException("con");
        if (con instanceof ConnectionProxy) {
            throw new IllegalArgumentException(
                    "ConnectionProxy have to call Connection#close() method " +
                    "instead of releaseConnection(Connection) method.");
        }

        // �`�F�b�N�A�E�g���ꂽConnection�����Ȃ��ꍇ�A���̕ԋp�͕s���B
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

        // �N���[�Y����Ă��Ȃ�Connection�����ԋp����������B
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
     * �ێ����Ă��邷�ׂĂ�Connection���N���[�Y���AConnectionPool��������
     * ���܂��B{@link #clear()} ���\�b�h�Ƃ̈Ⴂ�́A�Ăяo���ꂽ���_��
     * �ێ����Ă���Connection���܂������ꂩ�̃X���b�h�ŗ��p����Ă��Ă�
     * �����I�ɏ��������܂��B����́A���̃��\�b�h��GC�̃^�C�~���O�ŌĂяo�����
     * ���\�b�h�ł���AConnectionPool�C���X�^���X���̂����ł��邩��ł��B
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
     * ����ConnectionPool���ێ����Ă���f�[�^�x�[�X�ڑ������g���āA
     * {@link java.sql.Connection} �̃C���X�^���X�𐶐����܂��B
     * Connection�̏�����ԂƂ��Ď����R�~�b�g�� <tt>false</tt> �ɐݒ�
     * ����܂��B <br>
     * JDBC�h���C�o�N���X��������Ȃ������ꍇ�A����ȍ~�����̌p����
     * �s�\�Ȃ��߁AIllegalStateException���X���[���܂��B<br>
     * JDBC�h���C�o�N���X�̓��[�h�ł������ڑ�����SQLException�����������ꍇ�A
     * �l�b�g���[�N��Q�������̓f�[�^�x�[�X�T�[�o���̈ꎞ�I�ȏ�Ԃł���\��
     * �����邽�߁A��O�͐��������A<tt>null</tt> ��Ԃ��܂��B
     *
     * @return  ��������Connection�̃C���X�^���X�BSQLException�̔����ɂ��
     *          �����Ɏ��s�����ꍇ�� <tt>null</tt> ��Ԃ��܂��B
     *
     * @throws  ConnectionPoolException
     *          JDBC�h���C�o�N���X�̃��[�h�Ɏ��s�����ꍇ�ɃX���[���܂��B
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
     * ���̃C���X�^���X�̎��ʎq��Ԃ��܂��B
     */
    String getID() {
        return _ID;
    }

    /**
     * ���̃C���X�^���X�̃f�[�^�x�[�X�ڑ�����Ԃ��܂��B
     */
    ConnectionParameter getConnectionParameter() {
        return _parameter;
    }

    /**
     * �`�F�b�N�A�E�g����Ă���Connection����Ԃ��܂��B
     */
    synchronized int getCheckedOutConnections() {
        return _checkedOutConnections;
    }

    /**
     * ���ݕێ�����Ă���Connection����Ԃ��܂��B
     */
    int getCurrentPoolSize() {
        return _connections.size();
    }
}
