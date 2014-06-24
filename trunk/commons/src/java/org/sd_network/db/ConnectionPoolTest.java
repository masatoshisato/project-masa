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
 * {@link org.sd_network.db.ConnectionPool} �̃e�X�g�P�[�X���`���܂��B
 * <ul>
 *  <li> �C���X�^���X�`�F�b�N
 *      <ul>
 *       <li> �f�t�H���g�C���X�^���X�̎擾
 *       <li> ����ID�̃C���X�^���X�̎擾
 *       <li> �w���ID�ɕR�Â�ConnectionParameter�����݂��Ȃ��G���[
 *      </ul>
 *  <li> Connection�擾�`�F�b�N
 *      <ul>
 *       <li> ����i�V�K�����j
 *       <li> ����i�����C���X�^���X�j
 *       <li> Connection�����ł��Ȃ����߂̃^�C���A�E�g�G���[
 *       <li> �S�Ă�Connection���g�p����Ă��邽�߂̃^�C���A�E�g�G���[
 *      </ul>
 *  <li> Connection�ԋp�`�F�b�N
 *      <ul>
 *       <li> ����
 *       <li> ����i�N���[�Y���ꂽConnection�̂���Pool�ɂ͕ԋp���Ȃ��j
 *       <li> Null�G���[
 *       <li> ConnectionProxy�w��G���[
 *       <li> ���`�F�b�N�A�E�gPool�ɑ΂���release�G���[
 *      </ul>
 *  <li> clear�`�F�b�N
 *      <ul>
 *       <li> ����
 *       <li> �`�F�b�N�A�E�gConnection�����݂��邽�߂�Clear�ł��Ȃ��G���[
 *      </ul>
 * </ul>
 *
 * <p> �e�X�g�P�[�X�Ɏg�p����JDBC�ڑ����͈ȉ���ID���g�p���܂��B
 * <ul>
 *  <li> "default" : �ڑ��\
 *  <li> "test1"   : �ڑ��\
 *  <li> "test2    : �ڑ����̐ݒ�~�X�ɂ��ڑ��s��
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
    // Constructor and Initialization.

    public void setUp()
        throws Exception
    {
        super.setUp();
        _log.log(Level.FINE, "--- Run test case [" + getName() + "]");
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
    // �C���X�^���X�`�F�b�N

    /**
     * ���ʎq�Ŏw�肵��ConnectionPool�C���X�^���X���擾�ł��鎖���m�F���܂��B
     */
    public void testInstanceCheck_Normal() {
        // ID is not specified (=default)
        ConnectionPool poolNoID = ConnectionPool.getInstance();
        assertNotNull(poolNoID);
        assertEquals("default", poolNoID.getID());
        assertEquals(0, poolNoID.getCheckedOutConnections());
        assertEquals(0, poolNoID.getCurrentPoolSize());

        ConnectionParameter parameterNoID = poolNoID.getConnectionParameter();
        assertEquals("default", parameterNoID.getID());

        // ID is default.
        ConnectionPool poolDefault = ConnectionPool.getInstance("default");
        assertNotNull(poolDefault);
        assertEquals("default", poolDefault.getID());
        assertEquals(0, poolDefault.getCheckedOutConnections());
        assertEquals(0, poolDefault.getCurrentPoolSize());

        ConnectionParameter parameterDefault = 
            poolDefault.getConnectionParameter();
        assertEquals("default", parameterDefault.getID());

        // ID is test1.
        ConnectionPool pool1 = ConnectionPool.getInstance("test1");
        assertNotNull(pool1);
        assertEquals("test1", pool1.getID());
        assertEquals(0, pool1.getCheckedOutConnections());
        assertEquals(0, pool1.getCurrentPoolSize());

        ConnectionParameter parameter1 = pool1.getConnectionParameter();
        assertEquals("test1", parameter1.getID());

        // ID is test2.
        ConnectionPool pool2 = ConnectionPool.getInstance("test2");
        assertNotNull(pool2);
        assertEquals("test2", pool2.getID());
        assertEquals(0, pool2.getCheckedOutConnections());
        assertEquals(0, pool2.getCurrentPoolSize());

        ConnectionParameter parameter2 = pool2.getConnectionParameter();
        assertEquals("test2", parameter2.getID());
    }

    /**
     * ����̎��ʎq�ɕR�Â�ConnectionParameter�����݂��Ȃ��ꍇ��
     * IllegalStateException���X���[����鎖���m�F���܂��B
     */
    public void testInstanceCheck_ConnectionParameterNotFoundError() {
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
    // Connection�擾�`�F�b�N

    /**
     * �V�K��Connection����������A�擾�ł��鎖���m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     */
    public void testEngageCheck_NewConnection()
        throws SQLException
    {
        // ID is not specified (=default)
        ConnectionPool poolNoID = ConnectionPool.getInstance();
        Connection conNoID = poolNoID.engageConnection(0);
        assertNotNull(conNoID);
        assertEquals(1, poolNoID.getCheckedOutConnections());
        assertEquals(0, poolNoID.getCurrentPoolSize());
        conNoID.close();

        // ID is test1.
        ConnectionPool pool1 = ConnectionPool.getInstance("test1");
        Connection con1 = pool1.engageConnection(0);
        assertNotNull(con1);
        assertEquals(1, pool1.getCheckedOutConnections());
        assertEquals(0, pool1.getCurrentPoolSize());
        con1.close();

        // Do not this test for ID [test2] because of set illegal parameters 
        // to it.
    }

    /**
     * �v�[������Ă���Connection���擾�ł��鎖���m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     */
    public void testEngageCheck_ExistsConnection()
        throws SQLException
    {
        // get connection with ID is not specified (=default)
        ConnectionPool poolNoID = ConnectionPool.getInstance();
        assertEquals(0, poolNoID.getCheckedOutConnections());
        assertEquals(0, poolNoID.getCurrentPoolSize());

        Connection conNoID1 = poolNoID.engageConnection(0);
        assertNotNull(conNoID1);
        assertEquals(1, poolNoID.getCheckedOutConnections());
        assertEquals(0, poolNoID.getCurrentPoolSize());

        String conNoID1String = conNoID1.toString();

        // release to pool.
        conNoID1.close();
        assertEquals(0, poolNoID.getCheckedOutConnections());
        assertEquals(1, poolNoID.getCurrentPoolSize());

        // get connection again from pool.
        Connection conNoID2 = poolNoID.engageConnection(0);
        assertNotNull(conNoID2);
        assertEquals(1, poolNoID.getCheckedOutConnections());
        assertEquals(0, poolNoID.getCurrentPoolSize());

        // check same connection (engaged pooled connection)
        assertEquals(conNoID1String, conNoID2.toString());

        // release to pool.
        conNoID2.close();
    }

    /**
     * ConnectionParameter��񂪊Ԉ���Ă��邽�߂ɐV����Connection������
     * �ł����AConnectTimeoutException���X���[����鎖���m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     */
    public void testEngageCheck_TimeoutByConnectionCreationError()
        throws SQLException
    {
        ConnectionPool pool = ConnectionPool.getInstance("test2");
        try {
            Connection con1 = pool.engageConnection(1);
            if (con1 != null)
                con1.close();
            fail(
                    "This method is expected that Connection creation error," +
                    " but it is created normally.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test2]: Failed to engage connection.", e.getMessage());
        }
    }

    /**
     * �S�Ă�Connection���g�p����Ă��邽�߂�ConnectTimeoutException��
     * �X���[����鎖���m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     */
    public void testEngageCheck_TimeoutByAllConnectionIsUsingError()
        throws SQLException
    {
        // Prepare: get connection to max size.
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < ConnectionPool.MAX_POOL_SIZE; idx++) {
            cons[idx] = pool.engageConnection(0);
        }

        // Check pooled size is max.
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Check error occur by pooled size is max.
        try {
            Connection con = pool.engageConnection(0);
            if (con != null)
                con.close();
            fail(
                    "This method is expected that occur timed out error" +
                    " when connection creation bacause of all connection is" +
                    " checked out, but it is created normally.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test1]: Failed to engage connection.", e.getMessage());
            assertEquals(ConnectionPool.MAX_POOL_SIZE, 
                    pool.getCheckedOutConnections());
            assertEquals(0, pool.getCurrentPoolSize());
        }

        // close all connections.
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx].close();
        }

        // Check pooled connection size.
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(ConnectionPool.MAX_POOL_SIZE, pool.getCurrentPoolSize());
    }

    //////////////////////////////////////////////////////////// 
    // Connection�ԋp�`�F�b�N

    /**
     * �`�F�b�N�A�E�g���ꂽConnection������Ƀv�[���ɕԋp�ł��鎖��
     * �m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     */
    public void testReleaseCheck_OK()
        throws SQLException
    {
        // Prepare: get a connection from pool.
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con = pool.engageConnection(0);

        // Check pool size and checked out connection after get connection.
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Check pool size and checked out connection after close connection.
        con.close();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(1, pool.getCurrentPoolSize());
    }

    /**
     * ���ɐؒf����Ă���Connection���v�[���ɕԋp�����Ƃ��ɃC���X�^���X��
     * �v�[�����ꂸ�A�`�F�b�N�A�E�g�������J�E���g���܂��B
     * ���̃e�X�g�ł́A�蓮��Connection�𐶐����APool�ɋ����I�ɕԋp������
     * �s���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when call Connection#close()
     *          method.
     *
     * @throws  ClassNotFoundException
     *          Throws if JDBCDriver is not found when creation instance of
     *          JDBCDriver.
     */
    public void testReleaseCheck_ClosedConnection()
        throws SQLException, ClassNotFoundException
    {
        // Prepare: Get connection for count up checked out connection.
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con1 = pool.engageConnection(0);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Create connection with parameter in the pool.
        ConnectionParameter parameter = pool.getConnectionParameter();
        Class.forName(parameter.getJDBCDriver());
        Connection con2 = DriverManager.getConnection(
                parameter.getURL(),
                parameter.getUserName(),
                parameter.getPassword());

        // Force close connection.
        con2.close();

        // release to pool. This connection is already closed, count down
        // checked out connection count only, and connection was destroied.
        pool.releaseConnection(con2);
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize()); // �ԋp�Ȃ�

        // ���̃e�X�g�ł͖������Connection�𐶐����ĕԋp�������s���Ă��邽�߁A
        // ConnectionPool�̃`�F�b�N�A�E�g���ɖ������������Ă��܂��B
        // ���̂��߁A����Ƀ`�F�b�N�A�E�g����Connection��close���悤�Ƃ����
        // ConnectionPoolException����������B
        // ���̃e�X�g�����̎��Ȃ̂Ŗ�������B
        try {
            con1.close();
        } catch (ConnectionPoolException e) {
            // OK.
        }
    }

    /**
     * Null�I�u�W�F�N�g���v�[���ɕԋp���悤�Ƃ�������NullPointerException��
     * �X���[����鎖���m�F���܂��B
     */
    public void testReleaseCheck_NullError() {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        try {
            pool.releaseConnection(null);
            fail("Null check error.");
        } catch (NullPointerException e) {
            assertEquals("con", e.getMessage());
        }
    }

    /**
     * �v�[���֕ԋp���悤�Ƃ��Ă���̂�{@link java.sql.Connection} �ł͂Ȃ��A
     * {@link ConnectionProxy} �������ꍇ��IllegalArgumentException���X���[
     * ����鎖���m�F���܂��B
     */
    public void testReleaseCheck_SpecifiedConnectionProxyError() {
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
            try {
                con1.close();
            } catch (SQLException e) {
                // this exception is ignored.
            }
    }

    /**
     * �`�F�b�N�A�E�g���ꂽConnection�����݂��Ȃ��v�[���ɑ΂��āA�ڑ��ς݂�
     * Connection��ԋp���悤�Ƃ�������ConnectionPoolException���������鎖��
     * �m�F���܂��B
     *
     * @throws  ClassNotFoundException
     *          Throws if JDBCDriver class not found when load JDBCDriver class.
     *
     * @throws  SQLException
     *          Throws if database error occurred when create connection or
     *          created connection is closed.
     */
    public void testReleaseCheck_InitialPoolError()
        throws ClassNotFoundException, SQLException
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
    // Clear�`�F�b�N

    /**
     * {@link ConnectionPool#clear()} ���\�b�h������ɓ��삷�鎖���m�F���܂��B
     * clear()���\�b�h�����s�����ƁA�v�[���ɕێ�����Ă���Connection��
     * �S�Đؒf����A�S�Ẵv�[�����̓N���A����܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when close connection.
     */
    public void testClearCheck_OK()
        throws SQLException
    {
        // get ConnectionPool instance.
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        //////////////////////////////////////////////////////////// 
        // Tests call #clear() method immidietally after #getInstance() method
        // called.
        pool.clear();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        //////////////////////////////////////////////////////////// 
        // Tests call #clear() method after release all connection.

        // Engage all connection.
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Release all connection.
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

        // Check closed connection.
        for (int idx = 0; idx < cons.length; idx++) {
            assertTrue(cons[idx].isClosed());
        }
    }

    /**
     * {@link ConnectionPool#clear()} ���\�b�h�Ăяo�����Ƀ`�F�b�N�A�E�g
     * ����Ă���Connection�����݂���ꍇ�� {@link ConnectionPoolException} 
     * ���������鎖���m�F���܂��B
     *
     * @throws  SQLException
     *          Throws if database error occurred when close connection.
     */
    public void testClearCheck_ExistCheckedOutConnections()
        throws SQLException
    {
        // Get ConnectionPool instance.
        ConnectionPool pool = ConnectionPool.getInstance("test1");

        // Check initial status.
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Engage connection.
        Connection con = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // clear.
        try {
            pool.clear();
            fail("Checked out connection is exists, " +
                    "but not thrown a ConnectionPoolException.");
        } catch (ConnectionPoolException e) {
            assertEquals(
                    "Connection of ConnectionPool [test1] is still used. " +
                    "Number of checked out connection is [1].",
                    e.getMessage());
        } finally {
            con.close();
        }
    }
}
